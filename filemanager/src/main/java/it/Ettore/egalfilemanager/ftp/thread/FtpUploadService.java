package it.Ettore.egalfilemanager.ftp.thread;

import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.copyutils.CopyHandler;
import it.Ettore.egalfilemanager.copyutils.CopyService;
import it.Ettore.egalfilemanager.copyutils.SovrascritturaFiles;
import it.Ettore.egalfilemanager.filemanager.FileManager;
import it.Ettore.egalfilemanager.filemanager.OrdinatoreFiles;
import it.Ettore.egalfilemanager.fileutils.RootFileInputStream;
import it.Ettore.egalfilemanager.fileutils.StoragesUtils;
import it.Ettore.egalfilemanager.ftp.FtpFileUtils;
import it.Ettore.egalfilemanager.ftp.FtpSession;
import it.Ettore.egalfilemanager.ftp.ServerFtp;
import it.Ettore.egalfilemanager.mediastore.MediaUtils;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Service per la copia di files su server FTP da locale
 */
public class FtpUploadService extends CopyService {
    private static final String KEYBUNDLE_SERVER_FTP = "server_ftp";

    private long ultimoAggiornamento;
    private FileManager fileManager;
    private List<File> filesDaCancellareDaMediaStore;
    private FtpSession ftpSession;
    private StoragesUtils storagesUtils;



    /**
     * Costruttore di default (obbligatorio)
     */
    public FtpUploadService(){
        super("FtpUploadService");
    }


    /**
     * Crea l'intent per l'esecuzione del service
     * @param context Context chiamante
     * @param listaFiles Lista di files da copiare
     * @param serverFtp Server FTP
     * @param pathDestinazione Cartella FTP di destinazione
     * @param totSize Dimensione totale in bytes dei files da copiare
     * @param totFiles Numero totale di files da copiare
     * @param azioniFilesGiaPresenti Map contenente le associazioni files-azione da eseguire
     * @param handler Handler che permette al service di comunicare con la UI
     * @param cancellaOrigine True se in modalità taglia. False se in modalità copia
     * @return Intent per avviare il service
     */
    public static Intent createStartIntent(@NonNull Context context, List<File> listaFiles, @NonNull ServerFtp serverFtp, String pathDestinazione, long totSize, int totFiles,
                                           Map<File, Integer> azioniFilesGiaPresenti, @NonNull CopyHandler handler, boolean cancellaOrigine){
        final ArrayList<String> listaPaths = FileUtils.listFileToListPath(listaFiles);
        final HashMap<String, Integer> azioniPathsGiaPresenti = new HashMap<>(azioniFilesGiaPresenti.size());
        for(Map.Entry<File, Integer> entry : azioniFilesGiaPresenti.entrySet()){
            azioniPathsGiaPresenti.put(entry.getKey().getAbsolutePath(), entry.getValue());
        }
        final Intent intent = makeStartIntent(context, FtpUploadService.class, listaPaths, pathDestinazione, azioniPathsGiaPresenti, handler, totSize, totFiles, cancellaOrigine);
        intent.putExtra(KEYBUNDLE_SERVER_FTP, serverFtp.toString());
        return intent;
    }




    /**
     * Esecuzione copia in background
     * @param intent Intent
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        super.onHandleIntent(intent);

        fileManager = new FileManager(this);
        fileManager.ottieniStatoRootExplorer();
        storagesUtils = new StoragesUtils(this);
        filesDaCancellareDaMediaStore = new ArrayList<>();
        final ServerFtp serverFtp = ServerFtp.fromJson(this, intent.getStringExtra(KEYBUNDLE_SERVER_FTP));
        ftpSession = new FtpSession(this, serverFtp);

        final List<String> listaPaths = getListaPathDaCopiare();
        List<File> listaFiles = FileUtils.listPathToListFile(listaPaths);

        //verifico la correttezza dei dati
        if(listaFiles == null || getPathDestinazione() == null || listaFiles.isEmpty() || tutteAzioniIgnoraFiles()){
            sendMessageCanceled();
            return;
        }

        //notifico di avviare la dialog di copia
        sendMessageStartCopy();

        //connessione
        ftpSession.connectInTheSameThread();

        if(ftpSession.getFtpClient() != null){
            //copio i files se il server è connesso
            listaFiles = OrdinatoreFiles.ordinaPerNome(listaFiles);
            for (File inputFile : listaFiles) {
                if (!isRunning()){
                    sendMessageCanceled();
                    return;
                }
                copiaRicorsiva(inputFile, getPathDestinazione(), getAzioniPathsGiaPresenti());
            }

            //aggiorno il mediastore se ci sono files da cancellare (modalità "taglia")
            if(!filesDaCancellareDaMediaStore.isEmpty()){
                try {
                    sendTextMessage(getString(R.string.aggiornamento_media_library));
                    final MediaUtils mediaUtils = new MediaUtils(this);
                    mediaUtils.removeFilesFromMediaLibrary(filesDaCancellareDaMediaStore);
                } catch (Exception ignored){}
            }
        } else {
            //connessione non effettuata
            for(File file : listaFiles){
                aggiungiPathNonProcessato(file.getAbsolutePath());
            }
        }

        //notifico che la copia è terminata
        if(isRunning()){
            sendMessageCopyFinished();
        }
    }


    @Override
    protected int getTipoCopia() {
        return COPY_LOCAL_TO_FTP;
    }


    /**
     * Copia ricorsiva dei file
     * @param inputFile File di origine
     * @param pathDestinazione Path cartella di destinazione
     * @param azioniFilesGiaPresenti Map che contiene per ogni path l'azione da utilizzare (sovrascrivi, rinomina, ignora)
     */
    private void copiaRicorsiva(File inputFile, String pathDestinazione, Map<String, Integer> azioniFilesGiaPresenti){

        if(inputFile.isFile()) {
            //file
            incrementaIndiceFile();
            long fileSize = inputFile.length();

            String outputFilePath = null;
            final Integer azione = azioniFilesGiaPresenti.get(inputFile.getAbsolutePath());
            if(azione == null){
                //il file non è ancora presente
                outputFilePath = pathDestinazione + "/" + inputFile.getName();
            } else if(azione == SovrascritturaFiles.AZIONE_SOVRASCRIVI){
                outputFilePath = pathDestinazione + "/" + inputFile.getName();
                try {
                    ftpSession.getFtpClient().deleteFile(outputFilePath);
                } catch (IOException ignored) {}
            } else if (azione == SovrascritturaFiles.AZIONE_RINOMINA){
                //se è un file già esistente ed è stato scelto di rinominarlo
                final String possibileOutFile = pathDestinazione + "/" + inputFile.getName();
                outputFilePath = FtpFileUtils.rinominaFilePerEvitareSovrascrittura(ftpSession.getFtpClient(), possibileOutFile, false);
            } else if (azione == SovrascritturaFiles.AZIONE_IGNORA){
                //se è un file già esistente ed è stato scelto di mantenere quello già presente, non copia niente
                return;
            }

            sendMessageUpdateFile(inputFile.getName(), inputFile.getParent(), pathDestinazione, fileSize);

            InputStream in = null;
            OutputStream out = null;
            boolean success = false;

            try {
                if(storagesUtils.isOnRootPath(inputFile)){
                    in = new RootFileInputStream(inputFile);
                } else {
                    in = new FileInputStream(inputFile);
                }
                out = ftpSession.getFtpClient().storeFileStream(outputFilePath);

                long bytesWrited = 0L;
                byte[] buf = new byte[1024 * 4];
                int read;
                // Transfer bytes from in to out
                while ((read = in.read(buf)) > 0) {
                    if (!isRunning()) {
                        //cancello il file parziale se si annulla l'operazione
                        try {
                            out.close();
                        } catch (Exception ignored) {}
                        try {
                            in.close();
                        } catch (Exception ignored) {}
                        try {
                            ftpSession.getFtpClient().deleteFile(outputFilePath);
                        } catch (IOException ignored) {}
                        sendMessageCanceled();
                        return;
                    }
                    out.write(buf, 0, read);
                    bytesWrited += read;
                    incrementaTotWrited(read);

                    final long now = System.currentTimeMillis();
                    if(now - ultimoAggiornamento > FREQ_AGGIORNAMENTO){
                        sendMessageUpdateProgress(bytesWrited);
                        ultimoAggiornamento = now;
                    }

                }

                out.flush();
                success = bytesWrited == fileSize;

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    out.close();
                } catch (Exception ignored) {
                }
                try {
                    in.close();
                } catch (Exception ignored) {}
                try {
                    ftpSession.getFtpClient().completePendingCommand();
                } catch (Exception ignored) {}
            }

            //verifico l'esistenza del file copiato
            if(success && FtpFileUtils.fileExists(ftpSession.getFtpClient(), outputFilePath)){
                aggiungiPathProcessato(inputFile.getAbsolutePath());
                //funzione "taglia"
                if(isCancellaOrigine()){
                    fileManager.cancella(inputFile, true);
                    filesDaCancellareDaMediaStore.add(inputFile);
                }
            } else {
                aggiungiPathNonProcessato(inputFile.getAbsolutePath());
                //cancella file incompleto
                if(FtpFileUtils.fileExists(ftpSession.getFtpClient(), outputFilePath)){
                    try {
                        ftpSession.getFtpClient().deleteFile(outputFilePath);
                    } catch (IOException ignored) {}
                }
            }

        } else {
            //cartella
            final String pathNuovaDir = pathDestinazione + "/" + inputFile.getName();
            if(!FtpFileUtils.directoryExists(ftpSession.getFtpClient(), pathNuovaDir)){
                try {
                    ftpSession.getFtpClient().makeDirectory(pathNuovaDir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            List<File> listaFile = fileManager.ls(inputFile); //utilizzando il file manager posso usare anche i permessi di root
            if(listaFile != null) {
                listaFile = OrdinatoreFiles.ordinaPerNome(listaFile);
                for (File f : listaFile) {
                    if(!isRunning()) {
                        sendMessageCanceled();
                        return;
                    }
                    copiaRicorsiva(f, pathNuovaDir, azioniFilesGiaPresenti);
                }
            }
            //dopo aver finito di copiare tutti i files al suo interno, se c'è la funzione taglia, la cartella dovrebbe essere già vuota
            if(isCancellaOrigine()){
                List<File> listaFileVuota = fileManager.ls(inputFile);
                if(listaFileVuota != null && listaFileVuota.isEmpty()){
                    //mi accerto che la cartella sia davvero vuota
                    fileManager.cancella(inputFile, true);
                }
            }
        }
    }
}
