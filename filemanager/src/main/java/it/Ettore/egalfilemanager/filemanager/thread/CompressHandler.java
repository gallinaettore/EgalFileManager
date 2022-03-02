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

import androidx.annotation.NonNull;

import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.KEYBUNDLE_LISTENER_DATA;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.WHAT_OPERATION_CANCELED;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.WHAT_OPERATION_ERROR;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.WHAT_OPERATION_SUCCESSFULLY;


/**
 * Handler che gestisce la comunicazione del service di compressione con la UI
 */
public class CompressHandler extends BaseProgressHandler {
    private WeakReference<ZipCompressListener> listener;


    /**
     *
     * @param activity Activity chiamante
     * @param listener Listener da eseguire al termine dell'operazione
     */
    public CompressHandler(@NonNull Activity activity, ZipCompressListener listener) {
        super(activity);
        this.listener = new WeakReference<>(listener);
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
                eseguiListener(false, msg);
                break;
            case WHAT_OPERATION_ERROR:
                //inviato quando l'operazione termina con errori
                eseguiListener(false, msg);
                break;
            case WHAT_OPERATION_SUCCESSFULLY:
                //inviato quando l'operazione termina con successo
                eseguiListener(true, msg);
                break;
        }
    }


    /**
     * Esegue il listener passato all'handler se l'activity è ancora esistente
     * @param success True sel'operazione è stata completata con successo
     * @param msg Messaggio ricevuto dall'handler
     */
    private void eseguiListener(boolean success, Message msg){
        final Bundle data = msg.getData();
        ListenerData listenerData = (ListenerData) data.getSerializable(KEYBUNDLE_LISTENER_DATA);
        if(listenerData == null) listenerData = new ListenerData(); //inizializzo un nuovo oggetto (anche se avrà valori interni nulli)
        if(listener != null && listener.get() != null && !isActivityDestroyed()){
            listener.get().onZipCompressFinished(success, listenerData.destinationZipFile);
        }
    }


    /**
     * Classe di utilità che contiene tutti i dati che il service deve poi passare al listener
     */
    public static class ListenerData implements Serializable {
        public File destinationZipFile;
    }


    /**
     * Listener per la compressione dei file zip
     */
    public interface ZipCompressListener {

        /**
         * Chiamato al termine della compressione
         * @param success True se la compressione è avvenuta con successo
         * @param destinationFile File zip creato
         */
        void onZipCompressFinished(boolean success, File destinationFile);

    }
}
