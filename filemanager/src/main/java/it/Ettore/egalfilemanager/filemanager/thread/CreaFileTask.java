package it.Ettore.egalfilemanager.filemanager.thread;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.ui.ColoredProgressDialog;
import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.filemanager.FileManager;


/**
 * Classe che crea una cartella in un task separato
 */
public class CreaFileTask extends AsyncTask<Void, Void, Boolean> {
    private final FileManager fileManager;
    private final WeakReference<Activity> activity;
    private final CreaFileListener listener;
    private final File percorso;
    private final String nomeFile;
    private final File nuovoFile;
    private ColoredProgressDialog progress;
    private String dialogMessage;
    private int dialogType;


    /**
     *
     * @param fileManager File manager
     * @param percorso Percorso in cui si troverà il file
     * @param nomeFile Nome del file da creare
     * @param listener Listener chiamato al termine dell'operazione
     */
    public CreaFileTask(@NonNull FileManager fileManager, File percorso, @NonNull String nomeFile, CreaFileListener listener){
        this.activity = new WeakReference<>((Activity)fileManager.getContext());
        this.fileManager = fileManager;
        this.percorso = percorso;
        this.nomeFile = nomeFile;
        this.listener = listener;
        nuovoFile = new File(percorso, nomeFile.trim());
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
     * @return
     */
    @Override
    protected Boolean doInBackground(Void... params){
        if(percorso == null || nomeFile == null) return false;
        try {
            if (FileUtils.fileNameIsValid(nomeFile)) {
                if(nuovoFile.exists()){
                    dialogMessage = activity.get().getString(R.string.file_esistente);
                    dialogType = CustomDialogBuilder.TYPE_WARNING;
                    return true;
                } else {
                    return fileManager.creaFile(nuovoFile);
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
                    CustomDialogBuilder.make(activity.get(), String.format("%s %s", activity.get().getString(R.string.impossibile_creare_file), nuovoFile.getAbsolutePath()), CustomDialogBuilder.TYPE_ERROR).show();
                }
            }
            if(listener != null){
                listener.onFileManagerNewFileFinished(success);
            }
        }
    }


    /**
     * Listener di creazione del file
     */
    public interface CreaFileListener {


        /**
         * Chiamato al termine della creazione di un file
         *
         * @param created True se il file è stato creato o è già esistente.
         */
        void onFileManagerNewFileFinished(boolean created);
    }
}
