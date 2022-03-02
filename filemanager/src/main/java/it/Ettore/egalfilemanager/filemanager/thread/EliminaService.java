package it.Ettore.egalfilemanager.filemanager.thread;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.filemanager.FileManager;
import it.Ettore.egalfilemanager.filemanager.OrdinatoreFiles;
import it.Ettore.egalfilemanager.fileutils.LocalFileUtils;
import it.Ettore.egalfilemanager.fileutils.StoragesUtils;
import it.Ettore.egalfilemanager.mediastore.MediaUtils;
import it.Ettore.egalfilemanager.mount.MountUtils;


/**
 * Servizio che esegue l'eliminazione di files locali
 */
public class EliminaService extends BaseProgressService {
    private FileManager fileManager;
    private List<File> filesDaCancellare, filesDaCancellareDaMediaStore;



    /**
     * Costruttore
     */
    public EliminaService() {
        super("EliminaService");
    }


    /**
     * Crea l'intent per l'esecuzione del service
     * @param context Context chiamante
     * @param listaFiles Lista di files da processare
     * @param handler Handler che permette al service di comunicare con la UI
     * @return Intent per l'esecuzione del service
     */
    public static Intent createStartIntent(@NonNull Context context, @NonNull List<File> listaFiles, @NonNull EliminaHandler handler){
        final ArrayList<String> listaPaths = FileUtils.listFileToListPath(listaFiles);
        return makeStartBaseIntent(context, EliminaService.class, listaPaths, handler);
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
        final MountUtils mountUtils = new MountUtils(this);

        final List<String> listaPaths = getListaPaths();
        List<File> listaFiles = FileUtils.listPathToListFile(listaPaths);
        listaFiles = LocalFileUtils.fileListToRootFileList(this, listaFiles);

        //verifico la correttezza dei dati
        if (listaFiles == null || listaFiles.isEmpty()) {
            sendMessageOperationFinished();
            return;
        }

        this.filesDaCancellare = new ArrayList<>();
        this.filesDaCancellareDaMediaStore = new ArrayList<>();
        final List<File> filesNonCancellati = new ArrayList<>();


        //notifico di avviare la progress dialog (e aggiorno la notifica)
        sendMessageStartOperation(getString(R.string.elimina), null);

        boolean totalSuccess;
        try {
            analisiRicorsiva(listaFiles);
            for(int i=0; i < filesDaCancellare.size(); i++){
                if (!isRunning()){
                    mountUtils.ripristinaRo();
                    sendMessageCanceled();
                    sendMessageOperationFinished(); //sempre prima di ogni return in onHandleIntent()
                    return;
                }
                final File file = filesDaCancellare.get(i);
                mountUtils.montaInRwSeNecessario(file);
                final String message = String.format(getString(R.string.eliminazione_in_corso), file.getName());
                sendMessageUpdateProgress(message, i, filesDaCancellare.size());
                boolean success = fileManager.cancella(file, false);
                if(success){
                    filesDaCancellareDaMediaStore.add(file);
                } else {
                    filesNonCancellati.add(file);
                }
            }

            //aggiorno il mediastore
            if(!filesDaCancellareDaMediaStore.isEmpty() && new StoragesUtils(this).isOnSdCard(filesDaCancellareDaMediaStore.get(0))){
                try {
                    sendMessageUpdateProgress(R.string.aggiornamento_media_library, 1, 1);
                    final MediaUtils mediaUtils = new MediaUtils(this);
                    mediaUtils.removeFilesFromMediaLibrary(filesDaCancellareDaMediaStore);
                    sendMessageMediaScannerFinished();
                } catch (Exception ignored){}
            }

            totalSuccess = filesNonCancellati.isEmpty();

        } catch (Exception ignored){
            totalSuccess = false;
        }

        //il rimontaggio in RO della partizione /system a volte potrebbe richiedere diversi tentativi perchè risulta occupato
        sendMessageUpdateProgress("Remount busy partition...", 1, 1);
        mountUtils.ripristinaRoRicorsivamenteSeOccupato(60);

        if(isRunning()) {
            if (totalSuccess) {
                //operazione completata con successo
                sendMessageSuccessfully(String.format(getString(R.string.files_eliminati), String.valueOf(filesDaCancellare.size())));
            } else {
                sendMessageError(R.string.files_non_eliminati);
            }
        }

        sendMessageOperationFinished();
    }


    /**
     * Analisi ricorsiva dei files da cancellare
     * @param daAnalizzare Lista di files da analizzare
     */
    private void analisiRicorsiva(final List<File> daAnalizzare){
        final List<File> listaOrdinata = OrdinatoreFiles.ordinaPerNome(daAnalizzare);
        for(File file : listaOrdinata){
            if (!isRunning()){
                sendMessageCanceled();
                return;
            }
            if(file.isDirectory()){
                final List<File> filesNellaCartella = fileManager.ls(file);
                if(filesNellaCartella != null){
                    analisiRicorsiva(filesNellaCartella);
                }
            }
            filesDaCancellare.add(file);
        }
    }


    /**
     * Restituisce un oggetto serializzabile che contiene tutti i dati da passare poi al listener
     * (questo oggetto verrà passato al bundle dell'handler che a sua volta passerà i dati al listener)
     * @return Dati da passare al listener
     */
    @Override
    protected Serializable creaDatiPerListener() {
        final EliminaHandler.ListenerData listenerData = new EliminaHandler.ListenerData();
        listenerData.deletedFiles = filesDaCancellareDaMediaStore;
        return listenerData;
    }
}
