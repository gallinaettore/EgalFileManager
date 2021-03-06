package it.Ettore.egalfilemanager.ftp.thread;

import android.app.Activity;
import android.os.Bundle;
import android.os.Message;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.List;

import androidx.annotation.NonNull;
import it.Ettore.egalfilemanager.filemanager.thread.BaseProgressHandler;
import it.Ettore.egalfilemanager.ftp.FtpElement;

import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.KEYBUNDLE_LISTENER_DATA;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.WHAT_OPERATION_CANCELED;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.WHAT_OPERATION_ERROR;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.WHAT_OPERATION_SUCCESSFULLY;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


public class FtpRinominaHandler extends BaseProgressHandler {
    private WeakReference<FtpRinominaListener> listener;


    /**
     * @param activity Activity
     */
    public FtpRinominaHandler(@NonNull Activity activity, FtpRinominaListener listener) {
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
     * Esegue il listener passato all'handler se l'activity ?? ancora esistente
     * @param success True se l'operazione ?? stata completata con successo
     * @param msg Messaggio ricevuto dall'handler
     */
    private void eseguiListener(boolean success, Message msg){
        final Bundle data = msg.getData();
        ListenerData listenerData = (ListenerData) data.getSerializable(KEYBUNDLE_LISTENER_DATA);
        if(listenerData == null) listenerData = new ListenerData(); //inizializzo un nuovo oggetto (anche se avr?? valori interni nulli)
        if(listener != null && listener.get() != null && !isActivityDestroyed()){
            listener.get().onFtpRenameFinished(success, listenerData.oldFiles, listenerData.newFilesPaths);
        }
    }


    /**
     * Classe di utilit?? che contiene tutti i dati che il service deve poi passare al listener
     */
    public static class ListenerData implements Serializable {
        public List<FtpElement> oldFiles;
        public List<String> newFilesPaths;
    }





    /**
     * Listner di rinominazione
     */
    public interface FtpRinominaListener {

        /**
         * Chiamato dopo aver rinominato tutti i files
         * @param success True se tutti i files sono stati rinominati con successo
         * @param oldFiles Files non pi?? presenti (perch?? sono stati rinominati e hanno un path diverso)
         * @param newFilesPaths Nuovi files (files con il nuovo nome e quindi path diverso)
         */
        void onFtpRenameFinished(boolean success, List<FtpElement> oldFiles, List<String> newFilesPaths);
    }
}
