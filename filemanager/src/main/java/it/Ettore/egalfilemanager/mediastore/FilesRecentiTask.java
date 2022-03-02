package it.Ettore.egalfilemanager.mediastore;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.utils.FileUtils;


/**
 * Task per la ricerca dei files recenti
 */
public class FilesRecentiTask extends AsyncTask<Void, Void, List<File>> {
    private final WeakReference<Activity> activity;
    private final long ultimaData;
    private final MediaUtils mediaUtils;
    private final FilesRecentiTaskListener listener;
    private boolean mostraNascosti = false;


    /**
     *
     * @param activity Activity chiamante
     * @param giorni Giorni da ricercare
     * @param listener Listener eseguito al temine della ricerca
     */
    public FilesRecentiTask(@NonNull Activity activity, int giorni, FilesRecentiTaskListener listener){
        this.activity = new WeakReference<>(activity);
        this.ultimaData = System.currentTimeMillis() - (86400000 * (long)giorni); // 1000 * 60 * 60 * 24
        this.mediaUtils = new MediaUtils(activity);
        this.listener = listener;
    }


    /**
     * Imposta la modalità mostra nascosti
     * @param mostraNascosti True per mostraere tutti i files. False per nascondere i files nascosti o i files che si trovano all'interno di cartelle nascoste
     */
    public void setMostraNascosti(boolean mostraNascosti){
        this.mostraNascosti = mostraNascosti;
    }


    /**
     * Ricerca all'interno del media store
     * @param voids .
     * @return .
     */
    @Override
    protected List<File> doInBackground(Void... voids) {
        final List<File> listaFiles = new ArrayList<>();
        if(activity.get() != null && !activity.get().isFinishing()) {
            final String[] colums = {MediaStore.MediaColumns.DATA};
            final Uri uri = mediaUtils.uriForType(MediaUtils.MEDIA_TYPE_FILES);
            try (Cursor cursor = activity.get().getContentResolver().query(uri, colums, null, null, null))
            {
                int columnIndexData = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                File file;
                while (cursor.moveToNext()) {
                    if (isCancelled()) {
                        return listaFiles;
                    }
                    file = new File(cursor.getString(columnIndexData));
                    if (file != null && file.isFile() && file.lastModified() > ultimaData && !file.getAbsolutePath().contains("/Android/data/")) {
                        if (mostraNascosti) {
                            listaFiles.add(file);
                        } else {
                            //se si escludono i files nascosti, saranno esclusi anche i files visibili ma che si trovano in un percorso nascosto
                            if (!FileUtils.fileIsInHiddenPath(file)) {
                                listaFiles.add(file);
                            }
                        }
                    }
                }
            } catch (Exception ignored){
                //SecurityException se non ci sono i permessi di lettura/scrittura storage
            }
        }
        return listaFiles;
    }


    /**
     * Al termine della ricerca
     * @param files Lista dei files trovati
     */
    @Override
    protected void onPostExecute(List<File> files) {
        super.onPostExecute(files);
        if(activity.get() != null && !activity.get().isFinishing() && listener != null){
            listener.onRecentFilesFound(files);
        }
    }



    /**
     * Listener per la ricerca dei files recenti
     */
    public interface FilesRecentiTaskListener {

        /**
         * Chiamato al termine della ricerca
         * @param listaFiles Lista dei files trovati
         */
        void onRecentFilesFound(List<File> listaFiles);
    }
}
