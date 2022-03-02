package it.Ettore.egalfilemanager.ftp.thread;

import android.os.AsyncTask;

import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Task per la verifica della connessione FTP
 */
public class FtpVerifyConnectionTask extends AsyncTask <Void, Void, Boolean> {
    private final FtpVerifyConnectionListener listener;
    private final FTPClient ftpClient;


    /**
     *
     * @param ftpClient Client FTP
     * @param listener Listener eseguito al termine della verifica
     */
    public FtpVerifyConnectionTask(FTPClient ftpClient, FtpVerifyConnectionListener listener){
        this.ftpClient = ftpClient;
        this.listener  = listener;
    }


    /**
     * Verifica in background
     * @param voids Nessun parametro
     * @return True se risulta connesso. False se disconnesso o in caso di errore
     */
    @Override
    protected Boolean doInBackground(Void... voids) {
        if(ftpClient == null) return false;

        if(ftpClient.isConnected()){
            boolean noopSuccess = false;
            try {
                noopSuccess = ftpClient.sendNoOp();
            } catch (IOException ignored) {}
            return noopSuccess;
        } else {
            return false;
        }
    }


    /**
     * Esegui il listener
     * @param isConnected True se risulta connesso. False se disconnesso o in caso di errore
     */
    @Override
    protected void onPostExecute(Boolean isConnected) {
        if(listener != null){
            listener.onVerifyConnection(isConnected);
        }
    }



    /**
     * Listener di verifica connessione
     */
    public interface FtpVerifyConnectionListener {

        /**
         * Chiamato al termine della verifica
         * @param isConnected True se risulta connesso. False se disconnesso o in caso di errore
         */
        void onVerifyConnection(boolean isConnected);
    }
}
