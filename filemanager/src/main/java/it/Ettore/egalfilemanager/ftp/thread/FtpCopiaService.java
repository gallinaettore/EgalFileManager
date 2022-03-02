package it.Ettore.egalfilemanager.ftp.thread;

import android.content.Context;
import android.content.Intent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.Ettore.egalfilemanager.copyutils.CopyHandler;
import it.Ettore.egalfilemanager.copyutils.CopyService;
import it.Ettore.egalfilemanager.copyutils.SovrascritturaFiles;
import it.Ettore.egalfilemanager.ftp.FtpElement;
import it.Ettore.egalfilemanager.ftp.FtpFileUtils;
import it.Ettore.egalfilemanager.ftp.FtpSession;
import it.Ettore.egalfilemanager.ftp.OrdinatoreFilesFtp;
import it.Ettore.egalfilemanager.ftp.ServerFtp;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Service per la copia di files da server FTP all'interno di server FTP
 */
public class FtpCopiaService extends CopyService {
    private static final String KEYBUNDLE_FTP_ELEMENTS = "ftp_elements";
    private static final String KEYBUNDLE_SERVER_FTP_ORIGINE = "server_ftp_origine";
    private static final String KEYBUNDLE_SERVER_FTP_DESTINAZIONE = "server_ftp_destinazione";

    private long ultimoAggiornamento;
    private FtpSession originftpSession, destFtpSession;



    /**
     * Costruttore di default (obbligatorio)
     */
    public FtpCopiaService(){
        super("FtpCopiaService");
    }


    /**
     * Crea l'intent per l'esecuzione del service
     * @param context Context chiamante
     * @param listaFiles Lista di files da copiare
     * @param serverFtpOrigine Dati del server FTP di origine
     * @param serverFtpDestinazione Dati del server FTP di destinazione
     * @param pathDestinazione Path della cartella FTP di destinazione
     * @param totSize Dimensione totale in bytes dei files da copiare
     * @param totFiles Numero totale di files da copiare
     * @param azioniFilesGiaPresenti Map contenente le associazioni files-azione da eseguire
     * @param handler Handler che permette al service di comunicare con la UI
     * @return Intent per avviare il service
     */
    public static Intent createStartIntent(@NonNull Context context, List<FtpElement> listaFiles, @NonNull ServerFtp serverFtpOrigine, String pathDestinazione, @NonNull ServerFtp serverFtpDestinazione,
                                           long totSize, int totFiles, Map<FtpElement, Integer> azioniFilesGiaPresenti, @NonNull CopyHandler handler){
        final ArrayList<String> listaPaths = FtpFileUtils.listFileToListPath(listaFiles);
        final HashMap<String, Integer> azioniPathsGiaPresenti = new HashMap<>(azioniFilesGiaPresenti.size());
        for(Map.Entry<FtpElement, Integer> entry : azioniFilesGiaPresenti.entrySet()){
            azioniPathsGiaPresenti.put(entry.getKey().getAbsolutePath(), entry.getValue());
        }
        final Intent intent = makeStartIntent(context, FtpCopiaService.class, listaPaths, pathDestinazione, azioniPathsGiaPresenti, handler, totSize, totFiles, false);
        intent.putExtra(KEYBUNDLE_FTP_ELEMENTS, new ArrayList<>(listaFiles));
        intent.putExtra(KEYBUNDLE_SERVER_FTP_ORIGINE, serverFtpOrigine.toString());
        intent.putExtra(KEYBUNDLE_SERVER_FTP_DESTINAZIONE, serverFtpDestinazione.toString());
        return intent;
    }


    /**
     * Esecuzione copia in background
     * @param intent Intent
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void onHandleIntent(@Nullable Intent intent) {
        super.onHandleIntent(intent);

        final ServerFtp serverFtpOrigine = ServerFtp.fromJson(this, intent.getStringExtra(KEYBUNDLE_SERVER_FTP_ORIGINE));
        final ServerFtp serverFtpDestinazione = ServerFtp.fromJson(this, intent.getStringExtra(KEYBUNDLE_SERVER_FTP_DESTINAZIONE));
        originftpSession = new FtpSession(this, serverFtpOrigine);
        destFtpSession = new FtpSession(this, serverFtpDestinazione);
        List<FtpElement> listaFiles = (List<FtpElement>)intent.getSerializableExtra(KEYBUNDLE_FTP_ELEMENTS);

        //verifico la correttezza dei dati
        if(listaFiles == null || getPathDestinazione() == null || listaFiles.isEmpty() || tutteAzioniIgnoraFiles()){
            sendMessageCanceled();
            return;
        }

        //notifico di avviare la dialog di copia
        sendMessageStartCopy();

        //connessione
        originftpSession.connectInTheSameThread();
        destFtpSession.connectInTheSameThread();

        //copio i files
        if(originftpSession.getFtpClient() != null && destFtpSession.getFtpClient() != null){
            //copio i files se il server è connesso
            listaFiles = OrdinatoreFilesFtp.ordinaPerNome(listaFiles);
            for (FtpElement inputFile : listaFiles) {
                if (!isRunning()){
                    sendMessageCanceled();
                    return;
                }
                copiaRicorsiva(inputFile, getPathDestinazione(), getAzioniPathsGiaPresenti());
            }
        } else {
            //connessione non effettuata
            for(FtpElement file : listaFiles){
                aggiungiPathNonProcessato(file.getFullPath());
            }
        }

        //notifico che la copia è terminata
        if(isRunning()){
            sendMessageCopyFinished();
        }
    }


    @Override
    protected int getTipoCopia() {
        return COPY_FTP_TO_FTP;
    }


    /**
     * Copia ricorsiva dei file
     * @param inputFile File di origine
     * @param pathDestinazione Path della cartella di destinazione
     * @param azioniFilesGiaPresenti Map che contiene per ogni path l'azione da utilizzare (sovrascrivi, rinomina, ignora)
     */
    private void copiaRicorsiva(FtpElement inputFile, String pathDestinazione, Map<String, Integer> azioniFilesGiaPresenti){

        if(!inputFile.isDirectory()) {
            //file
            incrementaIndiceFile();
            long fileSize = inputFile.getSize();

            String outputFilePath = null;
            final Integer azione = azioniFilesGiaPresenti.get(inputFile.getAbsolutePath());
            if(azione == null){
                //il file non è ancora presente
                outputFilePath = pathDestinazione + "/" + inputFile.getName();
            } else if(azione == SovrascritturaFiles.AZIONE_SOVRASCRIVI){
                outputFilePath = pathDestinazione + "/" + inputFile.getName();
                try {
                    destFtpSession.getFtpClient().deleteFile(outputFilePath);
                } catch (IOException ignored) {}
            } else if (azione == SovrascritturaFiles.AZIONE_RINOMINA){
                //se è un file già esistente ed è stato scelto di rinominarlo
                final String possibileOutFile = pathDestinazione + "/" + inputFile.getName();
                outputFilePath = FtpFileUtils.rinominaFilePerEvitareSovrascrittura(destFtpSession.getFtpClient(), possibileOutFile, false);
            } else if (azione == SovrascritturaFiles.AZIONE_IGNORA){
                //se è un file già esistente ed è stato scelto di mantenere quello già presente, non copia niente
                return;
            }

            sendMessageUpdateFile(inputFile.getName(), inputFile.getParent(), pathDestinazione, fileSize);

            InputStream in = null;
            OutputStream out = null;
            boolean success = false;

            try {
                in = originftpSession.getFtpClient().retrieveFileStream(inputFile.getAbsolutePath());
                out = destFtpSession.getFtpClient().storeFileStream(outputFilePath);

                long bytesWrited = 0L;
                byte[] buf = new byte[1024 * 8];
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
                            destFtpSession.getFtpClient().deleteFile(outputFilePath);
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
                    originftpSession.getFtpClient().completePendingCommand();
                } catch (Exception ignored) {}
                try {
                    destFtpSession.getFtpClient().completePendingCommand();
                } catch (Exception ignored) {}
            }

            //verifico l'esistenza del file copiato
            if(success && FtpFileUtils.fileExists(destFtpSession.getFtpClient(), outputFilePath)){
                aggiungiPathProcessato(inputFile.getAbsolutePath());
            } else {
                aggiungiPathNonProcessato(inputFile.getFullPath());
                //cancella file incompleto
                if(FtpFileUtils.fileExists(destFtpSession.getFtpClient(), outputFilePath)){
                    try {
                        destFtpSession.getFtpClient().deleteFile(outputFilePath);
                    } catch (IOException ignored) {}
                }
            }

        } else {
            //cartella

            String pathNuovaDir;
            if (inputFile.getParent().equals(pathDestinazione)) {
                //se il copia/incolla avviene nella stessa cartella duplico la cartella
                pathNuovaDir = FtpFileUtils.rinominaFilePerEvitareSovrascrittura(destFtpSession.getFtpClient(), inputFile.getAbsolutePath(), true);
            } else {
                //copia/incolla normale
                pathNuovaDir = pathDestinazione + "/" + inputFile.getName();
            }

            if(!FtpFileUtils.directoryExists(destFtpSession.getFtpClient(), pathNuovaDir)){
                try {
                    destFtpSession.getFtpClient().makeDirectory(pathNuovaDir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            List<FtpElement> listaFiles = FtpFileUtils.explorePath(originftpSession, inputFile.getAbsolutePath());
            listaFiles = OrdinatoreFilesFtp.ordinaPerNome(listaFiles);
            for(FtpElement f : listaFiles){
                if(!isRunning()) {
                    sendMessageCanceled();
                    return;
                }
                copiaRicorsiva(f, pathNuovaDir, azioniFilesGiaPresenti);
            }
        }
    }
}
