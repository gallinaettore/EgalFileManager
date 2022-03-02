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
 * Handler che gestisce la comunicazione del service di estrazione con la UI
 */
public class ExtractHandler extends BaseProgressHandler {
    private WeakReference<ZipExtractListener> listener;


    /**
     *
     * @param activity Activity chiamante
     * @param listener Listener da eseguire al termine dell'operazione
     */
    public ExtractHandler(@NonNull Activity activity, ZipExtractListener listener) {
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
            case WHAT_OPERATION_ERROR:
                //inviato quando l'operazione termina con errori
                eseguiListener(false, msg);
                break;
            case WHAT_OPERATION_CANCELED:
                //inviato quando viene annullata l'operazione, chiudo la dialog e mostro il toast
                eseguiListener(true, msg);
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
            listener.get().onZipExtractFinished(success, listenerData.zipFile, listenerData.destFolder);
        }
    }


    /**
     * Classe di utilità che contiene tutti i dati che il service deve poi passare al listener
     */
    public static class ListenerData implements Serializable {
        public File zipFile, destFolder;
    }


    /**
     * Listener per l'estrazione dei files compressi
     */
    public interface ZipExtractListener {

        /**
         * Chiamato al termine dell'estrazione
         * @param successs True se l'estrazione è andata a buon fine
         * @param zipFile File compresso da estrarre
         * @param destFolder Cartella in cui è stato estratto il file
         */
        void onZipExtractFinished(boolean successs, File zipFile, File destFolder);

    }
}
