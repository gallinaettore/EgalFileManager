package it.Ettore.egalfilemanager.filemanager.thread;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.app.Activity;
import android.os.AsyncTask;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

import androidx.annotation.NonNull;
import it.Ettore.egalfilemanager.filemanager.FileManager;


/**
 * Classe per la scansione del contenuto delle cartelle in un task separato.
 */
public class LsTask extends AsyncTask<Void, Void, List<File>> {
    private final FileManager fileManager;
    private final WeakReference<Activity> activity;
    private final File directory;
    private final LsListener listener;


    /**
     *
     * @param fileManager File manager
     * @param directory Directory da scansionare
     * @param listener Listener chiamato al termine della scansione
     */
    public LsTask(@NonNull FileManager fileManager, File directory, LsListener listener){
        this.fileManager = fileManager;
        this.activity = new WeakReference<>((Activity)fileManager.getContext());
        this.directory = directory;
        this.listener = listener;
    }



    /**
     * Scansione la cartella in background
     * @param params Nessun parametro
     * @return Lista ordinata di files trovati. Lista vuota se non è stato trovato nessun file.
     */
    @Override
    protected List<File> doInBackground(Void... params){
        return fileManager.ls(directory);
    }


    /**
     * Mostra le dialog di conferma o di errore
     * @param listaFilesOrdinata Lista files ordinata
     */
    @Override
    protected void onPostExecute(List<File> listaFilesOrdinata){
        if(activity.get() != null && !activity.get().isFinishing()){
            if(listener != null){
                listener.onFileManagerLsFinished(directory, listaFilesOrdinata);
            }
        }
    }



    /**
     * Listener del file manager locale
     */
    public interface LsListener {
        /**
         * Chiamato al termine della scansione della cartella
         *
         * @param directory  Cartella scansionata
         * @param listaFiles Lista di files o directory al suo interno
         */
        void onFileManagerLsFinished(File directory, List<File> listaFiles);
    }
}
