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
public class CreaCartellaTask extends AsyncTask<Void, Void, Boolean> {
    private final FileManager fileManager;
    private final WeakReference<Activity> activity;
    private final CreaCartellaListener listener;
    private final File percorso;
    private final String nomeCartella;
    private final File nuovaCartella;
    private ColoredProgressDialog progress;
    private String dialogMessage;
    private int dialogType;


    /**
     *
     * @param fileManager File manager
     * @param percorso Percorso in cui si troverà la nuova cartella
     * @param nomeCartella Nome della cartella da creare
     * @param listener Listener chiamato al termine dell'operazione
     */
    public CreaCartellaTask(@NonNull FileManager fileManager, File percorso, @NonNull String nomeCartella, CreaCartellaListener listener){
        this.activity = new WeakReference<>((Activity)fileManager.getContext());
        this.fileManager = fileManager;
        this.percorso = percorso;
        this.nomeCartella = nomeCartella;
        this.listener = listener;
        nuovaCartella = new File(percorso, nomeCartella.trim());
    }


    /**
     * Mostro la dialog
     */
    @Override
    protected void onPreExecute(){
        if(activity.get() != null && !activity.get().isFinishing()){
            progress = ColoredProgressDialog.show(activity.get(), null, activity.get().getString(R.string.nuova_cartella));
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
        if(percorso == null || nomeCartella == null) return false;
        try {
            if (FileUtils.fileNameIsValid(nomeCartella)) {
                if(nuovaCartella.exists()){
                    dialogMessage = activity.get().getString(R.string.cartella_gia_esistente);
                    dialogType = CustomDialogBuilder.TYPE_WARNING;
                    return true;
                } else {
                    return fileManager.creaCartella(percorso, nomeCartella);
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
                    ColoredToast.makeText(activity.get(), R.string.cartella_creata, Toast.LENGTH_LONG).show();
                } else {
                    CustomDialogBuilder.make(activity.get(), String.format("%s %s", activity.get().getString(R.string.impossibile_creare_cartella), nuovaCartella.getAbsolutePath()), CustomDialogBuilder.TYPE_ERROR).show();
                }
            }
            if(listener != null){
                listener.onFileManagerNewFolderFinished(success);
            }
        }
    }


    /**
     * Listener di crezione della cartella
     */
    public interface CreaCartellaListener {


        /**
         * Chiamato al termine della creazione della cartella
         *
         * @param created True se la cartella è stata creata o è già esistente.
         */
        void onFileManagerNewFolderFinished(boolean created);
    }
}
