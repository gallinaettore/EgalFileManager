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
 * Task per la creazione di una cartella su un server smb
 */
public class SmbCreaCartellaTask extends AsyncTask <Void, Void, Boolean> {
    private final WeakReference<Activity> activity;
    private final String percorso, nomeCartella;
    private final SmbNuovaCartellaListener listener;
    private ColoredProgressDialog progress;
    private SmbFile nuovaCartella;
    private String dialogMessage;
    private int dialogType;


    /**
     *
     * @param activity Activity chiamante
     * @param percorso Percorso in cui creare la cartella (es. smb://192.168.1.x/)
     * @param nomeCartella Nome della cartella da creare
     * @param auth Autenticazione al server smb
     * @param listener Listener eseguito al termine dell'operazione
     */
    public SmbCreaCartellaTask(@NonNull Activity activity, String percorso, @NonNull String nomeCartella, NtlmPasswordAuthentication auth, SmbNuovaCartellaListener listener){
        this.activity = new WeakReference<>(activity);
        this.percorso = percorso;
        this.nomeCartella = nomeCartella;
        this.listener = listener;
        try {
            this.nuovaCartella = new SmbFile(percorso + nomeCartella.trim() + "/", auth); //la certella deve finere con /
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
            progress = ColoredProgressDialog.show(activity.get(), null, activity.get().getString(R.string.nuova_cartella));
            progress.setCancelable(false);
        }
    }


    /**
     * Creo la cartella in background
     * @param objects
     * @return True se la cartella è stata creata correttamente o se è già esistente
     */
    @Override
    protected Boolean doInBackground(Void... objects) {
        if(percorso == null || nomeCartella == null || nuovaCartella == null) return false;
        try {
            if (FileUtils.fileNameIsValid(nomeCartella)) {
                if(nuovaCartella.exists()){
                    dialogMessage = activity.get().getString(R.string.cartella_gia_esistente);
                    dialogType = CustomDialogBuilder.TYPE_WARNING;
                    return true;
                } else {
                    nuovaCartella.mkdir();
                    return true;
                }
            } else {
                dialogMessage = activity.get().getString(R.string.nome_non_valido);
                dialogType = CustomDialogBuilder.TYPE_ERROR;
                return false;
            }
        } catch (Exception e){
            e.printStackTrace();
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
                    CustomDialogBuilder.make(activity.get(), String.format("%s %s", activity.get().getString(R.string.impossibile_creare_cartella), nuovaCartella.toString()), CustomDialogBuilder.TYPE_ERROR).show();
                }
            }
            if(listener != null){
                listener.onSmbNewFolderFinished(success);
            }
        }
    }


    /**
     * Listener per la creazione della cartella
     */
    public interface SmbNuovaCartellaListener {

        /**
         * Chiamato al termine della creazione della cartella
         * @param created True se la cartella è stata creata o è già esistente.
         */
        void onSmbNewFolderFinished(boolean created);
    }
}
