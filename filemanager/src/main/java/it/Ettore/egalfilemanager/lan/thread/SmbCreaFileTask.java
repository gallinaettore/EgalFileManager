package it.Ettore.egalfilemanager.lan.thread;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.ui.ColoredProgressDialog;
import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Task per la creazione di un file su un server smb
 */
public class SmbCreaFileTask extends AsyncTask<Void, Void, Boolean> {
    private final WeakReference<Activity> activity;
    private final String percorso, nomeFile;
    private final SmbNuovoFileListener listener;
    private SmbFile nuovoFile;
    private ColoredProgressDialog progress;
    private String dialogMessage;
    private int dialogType;


    /**
     *
     * @param activity Activity chiamante
     * @param percorso Percorso in cui creare il file
     * @param nomeFile Nome del file da creare
     * @param auth Autenticazione al server smb
     * @param listener Listener eseguito al termine dell'operazione
     */
    public SmbCreaFileTask(@NonNull Activity activity, String percorso, @NonNull String nomeFile, NtlmPasswordAuthentication auth, SmbNuovoFileListener listener){
        this.activity = new WeakReference<>(activity);
        this.percorso = percorso;
        this.nomeFile = nomeFile;
        this.listener = listener;
        try {
            this.nuovoFile = new SmbFile(percorso + nomeFile.trim(), auth);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }


    /**
     * Mostro la dialog
     */
    @Override
    protected void onPreExecute(){
        if(activity.get() != null && !activity.get().isFinishing()){
            progress = ColoredProgressDialog.show(activity.get(), null, activity.get().getString(R.string.nuovo_file));
            progress.setCancelable(false);
        }
    }


    /**
     * Creo in background
     * @param params
     * @return True se il file è stato creato correttamente o se è già esistente
     */
    @Override
    protected Boolean doInBackground(Void... params){
        if(percorso == null || nomeFile == null || nuovoFile == null) return false;
        try {
            if (FileUtils.fileNameIsValid(nomeFile)) {
                if(nuovoFile.exists()){
                    dialogMessage = activity.get().getString(R.string.file_esistente);
                    dialogType = CustomDialogBuilder.TYPE_WARNING;
                    return true;
                } else {
                    nuovoFile.createNewFile();
                    return true;
                }
            } else {
                dialogMessage = activity.get().getString(R.string.nome_non_valido);
                dialogType = CustomDialogBuilder.TYPE_ERROR;
                return false;
            }
        } catch (Exception e){
            return false;
        }
    }


    /**
     * Mostra le dialog di conferma o di errore
     * @param success
     */
    @Override
    protected void onPostExecute(Boolean success){
        try {
            if (progress != null && progress.isShowing()) {
                progress.dismiss();
            }
        } catch (final IllegalArgumentException ignored) {}
        if(activity.get() != null && !activity.get().isFinishing()){
            if(dialogMessage != null){
                //notifica già presente
                CustomDialogBuilder.make(activity.get(), dialogMessage, dialogType).show();
            } else {
                if(success){
                    ColoredToast.makeText(activity.get(), R.string.file_creato_con_successo, Toast.LENGTH_LONG).show();
                } else {
                    CustomDialogBuilder.make(activity.get(), String.format("%s %s", activity.get().getString(R.string.impossibile_creare_file), nuovoFile.toString()), CustomDialogBuilder.TYPE_ERROR).show();
                }
            }
            if(listener != null){
                listener.onSmbNewFileFinished(success);
            }
        }
    }




    /**
     * Listener per la creazione del file
     */
    public interface SmbNuovoFileListener {

        /**
         * Chiamato al termine della creazione del file
         * @param created True se il file è stato creato correttamente o se è già esistente
         */
        void onSmbNewFileFinished(boolean created);
    }
}
