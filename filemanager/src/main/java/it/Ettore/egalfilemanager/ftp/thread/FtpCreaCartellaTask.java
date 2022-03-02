package it.Ettore.egalfilemanager.ftp.thread;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTPClient;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.ui.ColoredProgressDialog;
import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.ftp.FtpFileUtils;
import it.Ettore.egalfilemanager.ftp.FtpSession;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Task per la creazione di una cartella su un server FTP
 */
public class FtpCreaCartellaTask extends AsyncTask <Void, Void, Boolean> {
    private final WeakReference<Activity> activity;
    private final String nomeCartella, hostname;
    private final FtpNuovaCartellaListener listener;
    private final FTPClient ftpClient;
    private ColoredProgressDialog progress;
    private String dialogMessage, pathNuovaCartella;
    private int dialogType;


    /**
     *
     * @param activity Activity chiamante
     * @param percorso Percorso assoluto in cui creare la cartella (es. /cartella1/cartella2)
     * @param nomeCartella Nome della cartella da creare
     * @param ftpSession Sessione FTP
     * @param listener Listener eseguito al termine dell'operazione
     */
    public FtpCreaCartellaTask(@NonNull Activity activity, String percorso, @NonNull String nomeCartella, @NonNull FtpSession ftpSession, FtpNuovaCartellaListener listener){
        this.activity = new WeakReference<>(activity);
        this.nomeCartella = nomeCartella;
        this.listener = listener;
        this.ftpClient = ftpSession.getFtpClient();
        this.hostname = ftpSession.getServerFtp().getHost();
        if(percorso != null && nomeCartella != null){
            pathNuovaCartella = percorso + "/" + nomeCartella;
        }
    }


    /**
     * Mostro la dialog
     */
    @Override
    protected void onPreExecute(){
        progress = ColoredProgressDialog.show(activity.get(), null, activity.get().getString(R.string.nuova_cartella));
        progress.setCancelable(false);
    }


    /**
     * Creo la cartella in background
     * @param objects Nessun parametro
     * @return True se la cartella è stata creata correttamente o se è già esistente
     */
    @Override
    protected Boolean doInBackground(Void... objects) {
        if(pathNuovaCartella == null || ftpClient == null) return false;
        try {
            if (FileUtils.fileNameIsValid(nomeCartella)) {
                if(FtpFileUtils.directoryExists(ftpClient, pathNuovaCartella)){
                    dialogMessage = activity.get().getString(R.string.cartella_gia_esistente);
                    dialogType = CustomDialogBuilder.TYPE_WARNING;
                    return true;
                } else {
                    return ftpClient.makeDirectory(pathNuovaCartella);
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
     * @param success Se l'operazione è avvenuta con successo
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
                    final String pathCompleto = hostname + pathNuovaCartella;
                    CustomDialogBuilder.make(activity.get(), String.format("%s %s", activity.get().getString(R.string.impossibile_creare_cartella), pathCompleto), CustomDialogBuilder.TYPE_ERROR).show();
                }
            }
            if(listener != null){
                listener.onFtpNewFolderFinished(success);
            }
        }
    }


    /**
     * Listener per la creazione della cartella
     */
    public interface FtpNuovaCartellaListener {

        /**
         * Chiamato al termine della creazione della cartella
         * @param created True se la cartella è stata creata o è già esistente.
         */
        void onFtpNewFolderFinished(boolean created);
    }
}
