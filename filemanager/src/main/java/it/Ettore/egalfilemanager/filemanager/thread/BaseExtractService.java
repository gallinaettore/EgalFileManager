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
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.filemanager.FileManager;
import it.Ettore.egalfilemanager.fileutils.StoragesUtils;
import it.Ettore.egalfilemanager.mediastore.MediaUtils;



/**
 * Classe generica per servizi che eseguono operazioni di estrazione con progress dialog
 */
public abstract class BaseExtractService extends BaseProgressService {
    protected static final String KEYBUNDLE_PATH_ZIP_FILE = "path_zip_file";

    private FileManager fileManager;
    private File file, destinationFolder;
    private List<File> filesDaAggiungereSuMediaStore;



    /**
     * Costruttore
     * @param name Nome del service
     */
    public BaseExtractService(String name) {
        super(name);
    }


    /**
     * Crea l'intent per l'esecuzione del service
     * @param context Context chiamante
     * @param serviceClass Classe del service da avviare
     * @param file Archivio compresso
     * @param destinationFolder Directory di estrazione
     * @param handler Handler che permette al service di comunicare con la UI
     * @return Intent per l'esecuzione del service
     */
    protected static Intent createStartIntent(@NonNull Context context, Class<? extends BaseExtractService> serviceClass, @NonNull File file, @NonNull File destinationFolder, @NonNull ExtractHandler handler){
        final Intent intent = makeStartBaseIntent(context, serviceClass, null, handler);
        intent.putExtra(KEYBUNDLE_PATH_ZIP_FILE, file.getAbsolutePath());
        intent.putExtra(KEYBUNDLE_PATH_DESTINAZIONE, destinationFolder.getAbsolutePath());
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
        filesDaAggiungereSuMediaStore = new ArrayList<>();

        final String pathDestinazione = intent.getStringExtra(KEYBUNDLE_PATH_DESTINAZIONE);
        if(pathDestinazione != null){
            destinationFolder = new File(pathDestinazione);
        }
        final String pathZipFile = intent.getStringExtra(KEYBUNDLE_PATH_ZIP_FILE);
        if(pathZipFile != null){
            file = new File(pathZipFile);
        }
    }


    /**
     * Notifica all'handler che l'operazione è iniziata
     */
    protected void sendMessageStartOperation(){
        final String dialogTitle = getString(R.string.estrazione);
        final String dialogMessage = String.format(getString(R.string.estrazione_in_corso), file.getName());
        sendMessageStartOperation(dialogTitle, dialogMessage);
    }


    /**
     * Aggiungi il file corrente a una lista di files. Al termine dell'estarzione questi files saranno aggiunti al media store.
     * @param file File da aggiungere
     */
    protected void aggiungiAListaInserimentoSuMediaStore(File file){
        filesDaAggiungereSuMediaStore.add(file);
    }


    /**
     * Effettua l'aggiornamento del media store con i nuovi files estratti
     */
    protected void aggiornaMediaStore(){
        try {
            if(!filesDaAggiungereSuMediaStore.isEmpty() && new StoragesUtils(this).isOnSdCard(filesDaAggiungereSuMediaStore.get(0))) {
                final MediaUtils mediaUtils = new MediaUtils(this);
                mediaUtils.addFilesToMediaLibrary(filesDaAggiungereSuMediaStore, null);
            }
        } catch (Exception ignored){}
    }


    public FileManager getFileManager() {
        return fileManager;
    }


    public File getFile() {
        return file;
    }


    public File getDestinationFolder() {
        return destinationFolder;
    }


    /**
     * Restituisce un oggetto serializzabile che contiene tutti i dati da passare poi al listener
     * (questo oggetto verrà passato al bundle dell'handler che a sua volta passerà i dati al listener)
     * @return Dati da passare al listener
     */
    @Override
    protected Serializable creaDatiPerListener() {
        final ExtractHandler.ListenerData listenerData = new ExtractHandler.ListenerData();
        listenerData.zipFile = getFile();
        listenerData.destFolder = getDestinationFolder();
        return listenerData;
    }
}
