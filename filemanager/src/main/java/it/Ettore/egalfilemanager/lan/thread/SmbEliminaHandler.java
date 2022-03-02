package it.Ettore.egalfilemanager.lan.thread;

import android.app.Activity;
import android.os.Bundle;
import android.os.Message;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.List;

import androidx.annotation.NonNull;
import it.Ettore.egalfilemanager.filemanager.thread.BaseProgressHandler;
import it.Ettore.egalfilemanager.lan.SerializableSmbFileList;
import jcifs.smb.SmbFile;

import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.KEYBUNDLE_LISTENER_DATA;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.WHAT_OPERATION_CANCELED;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.WHAT_OPERATION_ERROR;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.WHAT_OPERATION_SUCCESSFULLY;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Handler che gestisce la comunicazione del service di eliminazione con la UI
 */
public class SmbEliminaHandler extends BaseProgressHandler {
    private WeakReference<SmbEliminaListener> listener;


    /**
     * @param activity Activity
     * @param listener Listener da eseguire al termine dell'operazione
     */
    public SmbEliminaHandler(@NonNull Activity activity, SmbEliminaListener listener) {
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
            listener.get().onSmbDeleteFinished(success, listenerData.deletedFiles.toFileList());
        }
    }


    /**
     * Classe di utilità che contiene tutti i dati che il service deve poi passare al listener
     */
    public static class ListenerData implements Serializable {
        public SerializableSmbFileList deletedFiles;
    }



    /**
     * Listener per l'eliminazione di files e directories
     */
    public interface SmbEliminaListener {

        /**
         * Chiamato al termine della cancellazione dei files
         * @param success True se tutti i files sono stati cancellati
         */
        void onSmbDeleteFinished(boolean success, List<SmbFile> deletedFiles);
    }
}
