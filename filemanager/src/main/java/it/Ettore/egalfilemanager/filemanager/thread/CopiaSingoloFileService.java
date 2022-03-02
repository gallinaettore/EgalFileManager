package it.Ettore.egalfilemanager.filemanager.thread;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.copyutils.CopyService;
import it.Ettore.egalfilemanager.filemanager.FileManager;
import it.Ettore.egalfilemanager.fileutils.RootFileInputStream;
import it.Ettore.egalfilemanager.fileutils.SAFUtils;
import it.Ettore.egalfilemanager.fileutils.StoragesUtils;
import it.Ettore.egalfilemanager.mediastore.MediaUtils;



public class CopiaSingoloFileService extends BaseProgressService {
    private static final String KEYBUNDLE_PATH_ORIGINE = "path_origine";

    private FileManager fileManager;
    private File fileOrigine, fileDestinazione;
    private long ultimoAggiornamento;
    private List<File> filesCopiati;


    /**
     * Costruttore
     */
    public CopiaSingoloFileService() {
        super("CopiaSingoloFileService");
    }


    /**
     * Crea l'intent per l'esecuzione del service
     * @param context Context chiamante
     * @param fileOrigine File da copiare
     * @param fileDestinazione File di destinazione
     * @param handler Handler che permette al service di comunicare con la UI
     * @return Intent per l'esecuzione del service
     */
    public static Intent createStartIntent(@NonNull Context context, File fileOrigine, File fileDestinazione, @NonNull CopiaSingoloFileHandler handler){
        final Intent intent = makeStartBaseIntent(context, CopiaSingoloFileService.class, null, handler);
        intent.putExtra(KEYBUNDLE_PATH_ORIGINE, fileOrigine.getAbsolutePath());
        intent.putExtra(KEYBUNDLE_PATH_DESTINAZIONE, fileDestinazione.getAbsolutePath());
        return intent;
    }


    /**
     * Esecuzione in background
     * @param intent Intent
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        super.onHandleIntent(intent);

        fileManager = new FileManager(this);
        fileManager.ottieniStatoRootExplorer();
        filesCopiati = new ArrayList<>(1);

        final String pathDiOrigine = intent.getStringExtra(KEYBUNDLE_PATH_ORIGINE);
        final String pathDestinazione = intent.getStringExtra(KEYBUNDLE_PATH_DESTINAZIONE);

        //verifico la correttezza dei dati
        if(pathDiOrigine == null || pathDestinazione == null){
            sendMessageOperationFinished(); //sempre prima di ogni return in onHandleIntent()
            return;
        }

        this.fileOrigine = new File(pathDiOrigine);
        this.fileDestinazione = new File(pathDestinazione);

        final String dialogTitle = getString(R.string.copia);
        final String dialogMessage = getString(R.string.copia_in_corso, fileDestinazione.getName());

        //notifico di avviare la progress dialog (e aggiorno la notifica)
        sendMessageStartOperation(dialogTitle, dialogMessage);

        //analisi dimensioni del file
        if(fileOrigine.length() >= fileManager.getFreeSpace(fileDestinazione.getParentFile())){
            sendMessageError(R.string.spazio_insufficiente);
            sendMessageOperationFinished(); //sempre prima di ogni return in onHandleIntent()
            return;
        }

        final boolean success = copia();

        if(isRunning()){
            if(success){
                sendMessageSuccessfully(String.format(getString(R.string.files_copiati), String.valueOf(1)));
            } else {
                sendMessageError(String.format("%s\n%s", getString(R.string.files_non_copiati), fileDestinazione.getName()));
            }
        }

        sendMessageOperationFinished();
    }


    private boolean copia(){
        if(fileDestinazione.exists()) {
            fileManager.cancella(fileDestinazione, true);
        }

        boolean success = false;

        //copio i bytes
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(fileOrigine);
            if(new StoragesUtils(this).isOnRootPath(fileOrigine)){
                in = new RootFileInputStream(fileOrigine);
            } else {
                in = new FileInputStream(fileOrigine);
            }
            out = SAFUtils.getOutputStream(this, fileDestinazione);

            if(out != null) {
                //permessi di scrittura sul file
                long fileSize = fileOrigine.length();
                byte[] buffer;
                if(fileSize < 500000){ //buffer piccolo per files sotto i 500Kb
                    buffer = new byte[1024 * 8];
                } else {
                    buffer = new byte[1024 * 32];
                }
                int read;
                long writed = 0L;
                // Transfer bytes from in to out
                while ((read = in.read(buffer)) > 0) {
                    if (!isRunning()){
                        sendMessageCanceled();
                        try {
                            out.close();
                        } catch (Exception ignored){}
                        try {
                            in.close();
                        } catch (Exception ignored){}
                        return false;
                    }
                    out.write(buffer, 0, read);
                    writed += read;

                    final long now = System.currentTimeMillis();
                    if(now - ultimoAggiornamento > FREQ_AGGIORNAMENTO){
                        sendMessageUpdateProgress((int)(writed/1000), (int)(fileSize/1000));
                        ultimoAggiornamento = now;
                    }
                }
                //out.flush();
                success = writed == fileSize;
            } else {
                //provo con i permessi di root
                if(fileManager.haPermessiRoot()) {
                    success = fileManager.copiaFileComeRoot(fileOrigine, fileDestinazione, true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (Exception ignored) {}
            try {
                in.close();
            } catch (Exception ignored) {}
        }

        if(success){
            filesCopiati.add(fileDestinazione);
        }

        //aggiorno il mediastore
        try {
            if(success && new StoragesUtils(this).isOnSdCard(fileDestinazione)){
                final MediaUtils mediaUtils = new MediaUtils(this);
                mediaUtils.addFileToMediaLibrary(fileDestinazione, null);
            }
        } catch (Exception ignored){}

        return success;
    }


    @Override
    protected Serializable creaDatiPerListener() {
        final CopiaSingoloFileHandler.ListenerData listenerData = new CopiaSingoloFileHandler.ListenerData();
        listenerData.destinationPath = fileDestinazione.getAbsolutePath();
        listenerData.filesCopiati = FileUtils.listFileToListPath(filesCopiati);
        listenerData.tipoCopia = CopyService.COPY_LOCAL_TO_LOCAL;
        return listenerData;
    }
}
