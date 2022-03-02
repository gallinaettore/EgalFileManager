package it.Ettore.egalfilemanager.lan.thread;

import android.app.Activity;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.ui.ColoredProgressDialog;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.lan.SmbFileUtils;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbFile;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Task per la verifica dei dati di autenticazione (user, password) di un server smb
 */
public class SmbAuthenticationTask extends AsyncTask<Void, Void, Integer> {

    /**
     * User e password corrette
     */
    public final static int RESULT_AUTHENTICATED = 0;

    /**
     * User o password errati
     */
    public final static int RESULT_NON_AUTHENTICATED = 1;

    /**
     * Non è possibile stabilire una connessione per altri motivi
     */
    public final static int RESULT_ERROR_CONNECTION = 2;


    private final String path, username, password;
    private final AuthenticationTaskListener listener;
    private final NtlmPasswordAuthentication auth;
    private final WeakReference<Activity> activity;
    private ColoredProgressDialog progress;


    /**
     *
     * @param path Path del server smb
     * @param username Username
     * @param password Password
     * @param listener Listener da eseguire al termine della verifica
     */
    public SmbAuthenticationTask(@NonNull Activity activity, @NonNull String path, String username, String password, AuthenticationTaskListener listener){
        this.activity = new WeakReference<>(activity);
        this.path = path;
        this.username = username;
        this.password = password;
        /*if(username != null && password != null){
            final String userpwd = username + ":" + password;
            auth = new NtlmPasswordAuthentication(userpwd);
        }*/
        this.auth = SmbFileUtils.createAuth(username, password);
        this.listener = listener;
    }


    /**
     * Mostra la progress dialog
     */
    @Override
    protected void onPreExecute(){
        progress = ColoredProgressDialog.show(activity.get(), null, activity.get().getString(R.string.connessione_in_corso));
        progress.setCancelable(false);
    }


    /**
     * Prova a connettersi in background per vedere se user e password sono corretti
     * @param voids /
     * @return Una delle costanti RESULT di questa classe
     */
    @Override
    protected Integer doInBackground(Void... voids) {
        try {
            final SmbFile smbFile = new SmbFile(path, auth);
            smbFile.connect();
            return RESULT_AUTHENTICATED;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return RESULT_ERROR_CONNECTION;
        } catch (SmbAuthException e){
            //errore di autenticazione, username o password errati
            return RESULT_NON_AUTHENTICATED;
        } catch (Exception e){
            e.printStackTrace();
            return RESULT_ERROR_CONNECTION;
        }
    }


    /**
     * Al termine esegue il listener
     * @param result Una delle costanti RESULT di questa classe
     */
    @Override
    protected void onPostExecute(Integer result) {
        try {
            if (progress != null && progress.isShowing()) {
                progress.dismiss();
            }
        } catch (final IllegalArgumentException ignored) {}
        listener.onAuthenticationFinished(result, path, username, password);
    }




    /**
     *  Listener della verifica autenticazione
     */
    public interface AuthenticationTaskListener {

        /**
         *
         * @param result Una delle costanti RESULT di questa classe
         * @param path Path del server smb
         * @param username Username
         * @param password Password
         */
        void onAuthenticationFinished(int result, @NonNull String path, String username, String password);
    }
}
