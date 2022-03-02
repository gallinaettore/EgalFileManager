package it.Ettore.egalfilemanager.mediastore;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.app.Activity;
import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.utils.FileUtils;


/**
 *  Classe per gestire la scansione di nuovi files da aggiungere al media store.
 *  La scansione avviene in un thread separato.
 */
public class MediaScannerUtil implements Runnable {
    private final Context context;
    private String[] pathsString;
    private int count;
    private MediaScannerListener listener;
    private List<File> files;


    /**
     *
     * @param context Context chiamante (Activity o Intent Service)
     */
    public MediaScannerUtil(@NonNull Context context){
        this.context = context;
    }


    /**
     * Avvia la scansione per aggiungere i files nel media store
     */
    @Override
    public void run() {
        final Set<File> allFiles = getAllFiles(this.files);
        pathsString = new String[allFiles.size()];
        final File[] arrayFiles = allFiles.toArray(new File[0]);
        for(int i=0; i < arrayFiles.length; i++){
            pathsString[i] = arrayFiles[i].getAbsolutePath();
        }

        if(pathsString.length == 0){
            //il listener lo eseguo nel thread principale
            if(context instanceof Activity){
                ((Activity)context).runOnUiThread(() -> {
                    try {
                        listener.onScanCompleted();
                    } catch (Exception ignored){}
                });
            } else if (context instanceof IntentService){
                //il listener lo eseguo nel thread del service
                try {
                    listener.onScanCompleted();
                } catch (Exception ignored){}
            }
            return;
        }

        //MediaScannerConnection.scanFile() non aggiunge i file m3u, li inserisco io manualmente
        for (File currentFile : allFiles) {
            if (FileUtils.getFileExtension(currentFile).equalsIgnoreCase("m3u")) {
                addFileM3uToMediaStore(context, currentFile);
            }
        }

        //scansione dei files
        MediaScannerConnection.scanFile(context.getApplicationContext(), pathsString, null, (String s, Uri uri) -> {
                count++;
                //notifico al listener solo dopo che tutti i paths sono stati scansionati
                if(count == pathsString.length && listener != null){
                    //il listener lo eseguo nel thread principale
                    if(context instanceof Activity){
                        ((Activity)context).runOnUiThread(() -> {
                            try {
                                listener.onScanCompleted();
                            } catch (Exception ignored){}
                        });
                    } else if (context instanceof IntentService){
                        //il listener lo eseguo nel thread del service
                        try {
                            listener.onScanCompleted();
                        } catch (Exception ignored){}
                    }
                }
        });
    }


    /**
     * Crea un Set con tutti i files presenti, anche quelli all'interno di sottodirectory
     * @param files Lista di files o cartelle da aggiungere al media Store
     * @return Set con tutti i files presenti
     */
    private Set<File> getAllFiles(List<File> files){
        final Set<File> allFiles = new LinkedHashSet<>();
        for(File file : files){
            if(file != null) {
                if (!file.isDirectory()) {
                    allFiles.add(file);
                } else {
                    final File[] contenutoCartella = file.listFiles();
                    if (contenutoCartella != null) {
                        allFiles.addAll(getAllFiles(new ArrayList<>(Arrays.asList(contenutoCartella))));
                    }
                }
            }
        }
        return allFiles;
    }


    /**
     * Avvia un thread per l'inserimento dei files nel media Store
     * @param files Lista di files o cartelle da aggiungere al media Store
     * @param listener Listener chiamato al termine della scansione (solo se il context è un'activity)
     */
    public void scanFiles(List<File> files, final MediaScannerListener listener){
        if(files == null || files.isEmpty()) return;
        this.files = files;
        this.listener = listener;
        new Thread(this).start();
    }


    /**
     * Aggiunge un file m3u al Media Store
     * MediaScannerConnection.scanFile() non funziona con i files m3u
     * @param context Context
     * @param m3uFile File m3u
     */
    private void addFileM3uToMediaStore(Context context, File m3uFile) {
        if(context == null || m3uFile == null) return;
        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, m3uFile.getAbsolutePath());
        values.put(MediaStore.Audio.Playlists.NAME, FileUtils.getFileNameWithoutExt(m3uFile));
        try {
            context.getContentResolver().insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values);
        } catch (Exception e){
            e.printStackTrace();
        }
    }




    /**
     * Interfaccia chiamata al termine della scansione
     */
    @FunctionalInterface
    public interface MediaScannerListener {
        void onScanCompleted();
    }
}
