package it.Ettore.egalfilemanager.ftp.thread;

import android.os.AsyncTask;

import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Task per la disconnessione dal server
 */
public class FtpDisconnectTask extends AsyncTask<Void, Void, Void> {
    private final FTPClient ftpClient;


    /**
     *
     * @param ftpClient Client FTP connesso
     */
    public FtpDisconnectTask(FTPClient ftpClient){
        this.ftpClient = ftpClient;
    }


    /**
     * Disconnessione in background
     * @param params Nessun parametro
     * @return Nessun ritorno
     */
    @Override
    protected Void doInBackground(Void... params) {
        if(ftpClient != null && ftpClient.isConnected()){
            try {
                ftpClient.logout();
            } catch (IOException ignored) {}
            try {
                ftpClient.disconnect();
            } catch (IOException ignored) {}
        }
        return null;
    }
}
