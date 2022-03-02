package it.Ettore.egalfilemanager.filemanager.thread;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.app.Activity;
import android.os.Bundle;
import android.os.Message;

import java.io.File;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.List;

import androidx.annotation.NonNull;
import it.Ettore.egalfilemanager.mediastore.MediaScannerUtil;

import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.KEYBUNDLE_LISTENER_DATA;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.WHAT_MEDIA_SCANNER_FINISHED;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.WHAT_OPERATION_CANCELED;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.WHAT_OPERATION_ERROR;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.WHAT_OPERATION_SUCCESSFULLY;


/**
 * Handler che gestisce la comunicazione del service di eliminazione con la UI
 */
public class EliminaHandler extends BaseProgressHandler {
    private WeakReference<EliminaListener> listener;
    private WeakReference<MediaScannerUtil.MediaScannerListener> mediaScannerListener;


    /**
     * @param activity Activity
     * @param listener Listener da eseguire al termine dell'operazione
     */
    public EliminaHandler(@NonNull Activity activity, EliminaListener listener) {
        super(activity);
        this.listener = new WeakReference<>(listener);
    }


    /**
     * Setta il listener per il media scanner: informa quando, al termine dell'operazione, la funziona di scanzione sul media store è terminata
     * @param mediaScannerListener Listener del media scanner
     */
    public void setMediaScannerListener(MediaScannerUtil.MediaScannerListener mediaScannerListener) {
        this.mediaScannerListener = new WeakReference<>(mediaScannerListener);
    }


    /**
     * Chiamato quando si riceve un messaggio da parte del service.
     * @param msg Messaggio ricevuto
     */
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);

        switch (msg.what) {
            case WHAT_OPERATION_CANCELED:
                //inviato quando viene annullata l'operazione, chiudo la dialog e mostro il toast
                eseguiListener(true, msg);
                break;
            case WHAT_OPERATION_ERROR:
                //inviato quando l'operazione termina con errori
                eseguiListener(false, msg);
                break;
            case WHAT_OPERATION_SUCCESSFULLY:
                //inviato quando l'operazione termina con successo
                eseguiListener(true, msg);
                break;
            case WHAT_MEDIA_SCANNER_FINISHED:
                if(mediaScannerListener != null && mediaScannerListener.get() != null && !isActivityDestroyed()){
                    mediaScannerListener.get().onScanCompleted();
                }
                break;
        }
    }


    /**
     * Esegue il listener passato all'handler se l'activity è ancora esistente
     * @param success True se l'operazione è stata completata con successo
     * @param msg Messaggio ricevuto dall'handler
     */
    private void eseguiListener(boolean success, Message msg){
        final Bundle data = msg.getData();
        ListenerData listenerData = (ListenerData) data.getSerializable(KEYBUNDLE_LISTENER_DATA);
        if(listenerData == null) listenerData = new ListenerData(); //inizializzo un nuovo oggetto (anche se avrà valori interni nulli)
        if(listener != null && listener.get() != null && !isActivityDestroyed()){
            listener.get().onFileManagerDeleteFinished(success, listenerData.deletedFiles);
        }
    }


    /**
     * Classe di utilità che contiene tutti i dati che il service deve poi passare al listener
     */
    public static class ListenerData implements Serializable {
        public List<File> deletedFiles;
    }


    /**
     * Listener di eliminazione
     */
    public interface EliminaListener {

        /**
         * Chiamato al termine della cancellazione dei files
         * @param success True se tutti i files sono stati cancellati
         */
        void onFileManagerDeleteFinished(boolean success, List<File> deletedFiles);
    }
}
