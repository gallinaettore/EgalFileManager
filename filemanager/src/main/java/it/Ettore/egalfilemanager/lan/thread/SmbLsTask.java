package it.Ettore.egalfilemanager.lan.thread;

import android.app.Activity;
import android.os.AsyncTask;

import java.net.MalformedURLException;

import androidx.annotation.NonNull;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Task per l'analisi del contenuto di un percorso smb
 */
public class SmbLsTask extends AsyncTask <Void, Void, SmbFile[]> {
    private final SmbLsListener listener;
    private final String path;
    private SmbFile directory;


    /**
     * @param activity Activity chiamante
     * @param path Percorso da analizzare
     * @param auth Autenticazione al server smb
     * @param listener Listener eseguito al termine dell'operazione
     */
    public SmbLsTask(@NonNull Activity activity, @NonNull String path, NtlmPasswordAuthentication auth, SmbLsListener listener){
        this.path = path;
        this.listener = listener;
        try {
            this.directory = new SmbFile(path, auth);
        } catch (MalformedURLException e) {
            this.directory = null;
            e.printStackTrace();
        }
    }


    /**
     * Ottengo la lista files in background
     * @param voids .
     * @return Lista files trovati. Null se non sono presenti files o se non è stato possibile analizzare.
     */
    @Override
    protected SmbFile[] doInBackground(Void... voids) {
        try {
            if(directory != null){
                return directory.listFiles();
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Esegue il listener associato
     * @param smbFiles Lista files trovati. Null se non sono presenti files o se non è stato possibile analizzare.
     */
    @Override
    protected void onPostExecute(SmbFile[] smbFiles) {
        if(listener != null){
            try {
                listener.onSmbLsFinished(path, smbFiles);
            } catch (IllegalStateException e){
                //eccezione se non è possibile eseguire il listener perchè il fragment non è più visibile
                e.printStackTrace();
            }
        }
    }





    /**
     * Listener per l'analisi del contenuto della cartella
     */
    public interface SmbLsListener {

        /**
         * Eseguito al termine dell'analisi
         * @param directoryPath Percoso della cartella scansionata
         * @param files Lista files trovati. Null se non sono presenti files o se non è stato possibile analizzare.
         */
        void onSmbLsFinished(String directoryPath, SmbFile[] files);
    }
}
