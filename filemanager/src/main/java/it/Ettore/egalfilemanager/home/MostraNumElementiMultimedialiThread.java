package it.Ettore.egalfilemanager.home;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.app.Activity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import it.Ettore.egalfilemanager.mediastore.Album;
import it.Ettore.egalfilemanager.mediastore.FindAlbumsTask;
import it.Ettore.egalfilemanager.mediastore.MediaUtils;


/**
 *  Thread che si occupa di contare i numeri di media trovati nel catalogo e mostrarli in una TextView
 *  @author Ettore Gallina
 */
public class MostraNumElementiMultimedialiThread implements Runnable {
    private final WeakReference<Activity> activity;
    private final int mediaType;
    private final TextView textView;
    private ProgressBar progressBar;
    private final MediaUtils mediaUtils;
    private final AtomicBoolean canceled;
    private final FindAlbumsTask findAlbumsTask;


    /**
     * @param activity Activity
     * @param mediaType Tipo di media della classe MediaUtils
     * @param textView TextView su cui mostrare il numero di media trovati
     */
    public MostraNumElementiMultimedialiThread(@NonNull Activity activity, int mediaType, TextView textView){
        this.activity = new WeakReference<>(activity);
        this.mediaType = mediaType;
        this.textView = textView;
        this.mediaUtils = new MediaUtils(activity);
        this.canceled = new AtomicBoolean(false);
        findAlbumsTask = new FindAlbumsTask(activity, mediaType, null); //non eseguirò il task ma utilizzerò un suo metodo all'interno di questo thread
    }


    /**
     * Avvia il thread
     */
    public void start(){
        new Thread(this).start();
        if(progressBar != null){
            progressBar.setVisibility(View.VISIBLE);
            textView.setVisibility(View.GONE);
        }
    }


    /**
     * Interrompe l'esecuzione del thread
     * @param canceled Impostato a true interrompe l'esecuzione del thread
     */
    public void setCanceled(boolean canceled){
        this.canceled.set(canceled);
        if(findAlbumsTask != null){
            findAlbumsTask.cancel(canceled);
        }
    }


    /**
     * Imposta l'eventuale progress bar da mostrare durante l'esecuzione del thread
     * @param progressBar Progress bar da mostrare
     */
    public void setProgressBar(ProgressBar progressBar){
        this.progressBar = progressBar;
    }


    @Override
    public void run() {
        int mediaCount; //Numero di media trovati. -1 se il tipo di media è MEDIA_TYPE_FILES perchè prima devono essere scansionati gli albums. 0 se il tipo di media non è riconosciuto.
        switch (mediaType){
            case MediaUtils.MEDIA_TYPE_IMAGE:
            case MediaUtils.MEDIA_TYPE_VIDEO:
                mediaCount = mediaUtils.getMediaCount(mediaType);
                break;
            case MediaUtils.MEDIA_TYPE_AUDIO:
                //includo anche le playlist
                mediaCount = mediaUtils.getMediaCount(MediaUtils.MEDIA_TYPE_AUDIO) + mediaUtils.getMediaCount(MediaUtils.MEDIA_TYPE_PLAYLIST);
                break;
            case MediaUtils.MEDIA_TYPE_FILES:
                final List<Album> listaAlbums = findAlbumsTask.nonMediaFilesAlbums();
                mediaCount = mediaUtils.getMediaCount(listaAlbums);
                break;
            default:
                mediaCount = -1;
        }
        showValue(mediaCount);
    }


    /**
     * Mostra il valore nella textview nel thread princiaple
     * @param value Valore da mostrare
     */
    private void showValue(final int value){
        try {
            activity.get().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                        textView.setVisibility(View.VISIBLE);
                    }
                    textView.setText(String.valueOf(value));
                }
            });
        } catch (Exception ignored){}
    }

}
