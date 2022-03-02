package it.Ettore.egalfilemanager.lan.thread;

import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
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
import it.Ettore.egalfilemanager.lan.SmbFileUtils;
import it.Ettore.egalfilemanager.mediastore.MediaUtils;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Service per la copia di files su server smb da locale
 */
public class SmbUploadService extends CopyService {
    private static final String KEYBUNDLE_USER = "user";
    private static final String KEYBUNDLE_PWD = "pwd";

    private long ultimoAggiornamento;
    private FileManager fileManager;
    private List<File> filesDaCancellareDaMediaStore;
    private StoragesUtils storagesUtils;


    /**
     * Costruttore di default (obbligatorio)
     */
    public SmbUploadService(){
        super("SmbUploadService");
    }


    /**
     * Crea l'intent per l'esecuzione del service
     * @param context Context chiamante
     * @param listaFiles Lista di files da copiare
     * @param user Username del server smb
     * @param pwd Password del server smb
     * @param destinazione Cartella SMB di destinazione
     * @param totSize Dimensione totale in bytes dei files da copiare
     * @param totFiles Numero totale di files da copiare
     * @param azioniFilesGiaPresenti Map contenente le associazioni files-azione da eseguire
     * @param handler Handler che permette al service di comunicare con la UI
     * @param cancellaOrigine True se in modalità taglia. False se in modalità copia
     * @return Intent per avviare il service
     */
    public static Intent createStartIntent(@NonNull Context context, List<File> listaFiles, String user, String pwd, SmbFile destinazione, long totSize, int totFiles,
                                           Map<File, Integer> azioniFilesGiaPresenti, @NonNull CopyHandler handler, boolean cancellaOrigine){
        final ArrayList<String> listaPaths = FileUtils.listFileToListPath(listaFiles);
        final HashMap<String, Integer> azioniPathsGiaPresenti = new HashMap<>(azioniFilesGiaPresenti.size());
        for(Map.Entry<File, Integer> entry : azioniFilesGiaPresenti.entrySet()){
            azioniPathsGiaPresenti.put(entry.getKey().getAbsolutePath(), entry.getValue());
        }
        final Intent intent = makeStartIntent(context, SmbUploadService.class, listaPaths, destinazione.getPath(), azioniPathsGiaPresenti, handler, totSize, totFiles, cancellaOrigine);
        intent.putExtra(KEYBUNDLE_USER, user);
        intent.putExtra(KEYBUNDLE_PWD, pwd);
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


        final List<String> listaPaths = getListaPathDaCopiare();
        List<File> listaFiles = FileUtils.listPathToListFile(listaPaths);
        final String user = intent.getStringExtra(KEYBUNDLE_USER);
        final String pwd = intent.getStringExtra(KEYBUNDLE_PWD);
        final NtlmPasswordAuthentication auth = SmbFileUtils.createAuth(user, pwd);

        //verifico la correttezza dei dati
        if(listaFiles == null || getPathDestinazione() == null || listaFiles.isEmpty() || tutteAzioniIgnoraFiles()){
            sendMessageCanceled();
            return;
        }

        //notifico di avviare la dialog di copia
        sendMessageStartCopy();

        //copio i files
        listaFiles = OrdinatoreFiles.ordinaPerNome(listaFiles);
        for (int i=0; i < listaFiles.size(); i++) {
            if (!isRunning()){
                sendMessageCanceled();
                return;
            }
            File inputFile = listaFiles.get(i);
            try {
                final SmbFile cartellaDestinazione = new SmbFile(getPathDestinazione(), auth);
                copiaRicorsiva(inputFile, cartellaDestinazione, getAzioniPathsGiaPresenti(), auth);
            } catch (Exception e) {
                e.printStackTrace();
                aggiungiPathNonProcessato(inputFile.getAbsolutePath());
            }
        }

        //aggiorno il mediastore se ci sono files da cancellare (modalità "taglia")
        if(!filesDaCancellareDaMediaStore.isEmpty()){
            try {
                sendTextMessage(getString(R.string.aggiornamento_media_library));
                final MediaUtils mediaUtils = new MediaUtils(this);
                mediaUtils.removeFilesFromMediaLibrary(filesDaCancellareDaMediaStore);
            } catch (Exception ignored){}
        }

        //notifico che la copia è terminata
        if(isRunning()){
            sendMessageCopyFinished();
        }
    }


    @Override
    protected int getTipoCopia() {
        return COPY_LOCAL_TO_SMB;
    }


    /**
     * Copia ricorsiva dei file
     * @param inputFile File di origine
     * @param outputFolder File di destinazione
     * @param azioniFilesGiaPresenti Map che contiene per ogni path l'azione da utilizzare (sovrascrivi, rinomina, ignora)
     * @param auth Autenticazione server SMB
     */
    private void copiaRicorsiva(File inputFile, SmbFile outputFolder, Map<String, Integer> azioniFilesGiaPresenti, NtlmPasswordAuthentication auth){

        if(inputFile.isFile()) {
            //file
            incrementaIndiceFile();
            long fileSize = inputFile.length();
            SmbFile outputFile = null;
            //se azione == null
            final Integer azione = azioniFilesGiaPresenti.get(inputFile.getAbsolutePath());
            if(azione == null){
                //il file non è ancora presente
                try {
                    outputFile = new SmbFile(outputFolder + inputFile.getName(), auth);
                } catch (MalformedURLException ignored) {}
            } else if(azione == SovrascritturaFiles.AZIONE_SOVRASCRIVI){
                try {
                    outputFile = new SmbFile(outputFolder + inputFile.getName(), auth);
                    outputFile.delete();
                } catch (Exception ignored) {}
            } else if (azione == SovrascritturaFiles.AZIONE_RINOMINA){
                //se è un file già esistente ed è stato scelto di rinominarlo
                try {
                    final SmbFile possibileOutFile = new SmbFile(outputFolder + inputFile.getName(), auth);
                    outputFile = SmbFileUtils.rinominaFilePerEvitareSovrascrittura(possibileOutFile, auth);
                } catch (MalformedURLException ignored) {}
            } else if (azione == SovrascritturaFiles.AZIONE_IGNORA){
                //se è un file già esistente ed è stato scelto di mantenere quello già presente, non copia niente
                return;
            }

            sendMessageUpdateFile(inputFile.getName(), inputFile.getParent(), outputFile.getParent(), fileSize);

            InputStream in = null;
            OutputStream out = null;
            boolean success = false;

            try {
                if(storagesUtils.isOnRootPath(inputFile)){
                    in = new RootFileInputStream(inputFile);
                } else {
                    in = new FileInputStream(inputFile);
                }
                out = outputFile.getOutputStream();

                long bytesWrited = 0L;
                byte[] buf = new byte[1024 * 32];
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
                        outputFile.delete();
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
            }

            //verifico l'esistenza del file copiato
            try {
                if(success && outputFile.exists()){
                    aggiungiPathProcessato(inputFile.getAbsolutePath());
                    //funzione "taglia"
                    if(isCancellaOrigine()){
                        fileManager.cancella(inputFile, true);
                        filesDaCancellareDaMediaStore.add(inputFile);
                    }
                } else {
                    aggiungiPathNonProcessato(inputFile.getAbsolutePath());
                    //cancella file incompleto
                    if(outputFile.exists()){
                        outputFile.delete();
                    }
                }
            } catch (SmbException e) {
                e.printStackTrace();
                aggiungiPathNonProcessato(inputFile.getAbsolutePath());
            }

        } else {
            //cartella
            SmbFile nuovaDir = null;
            try {
                nuovaDir = new SmbFile(outputFolder + inputFile.getName() + "/", auth);
            } catch (MalformedURLException ignored) {}

            try {
                if(!nuovaDir.exists()){
                    nuovaDir.mkdir();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            List<File> listaFile = fileManager.ls(inputFile); //utilizzando il file manager posso usare anche i permessi di root
            if(listaFile != null) {
                listaFile = OrdinatoreFiles.ordinaPerNome(listaFile);
                for (File f : listaFile) {
                    if(!isRunning()) {
                        sendMessageCanceled();
                        return;
                    }
                    copiaRicorsiva(f, nuovaDir, azioniFilesGiaPresenti, auth);
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
