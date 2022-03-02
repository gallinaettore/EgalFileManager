package it.Ettore.egalfilemanager.ftp;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.Context;

import org.apache.commons.net.ftp.FTPClient;

import androidx.annotation.NonNull;
import it.Ettore.egalfilemanager.ftp.thread.FtpConnectTask;
import it.Ettore.egalfilemanager.ftp.thread.FtpConnectionListener;
import it.Ettore.egalfilemanager.ftp.thread.FtpDisconnectTask;
import it.Ettore.egalfilemanager.ftp.thread.FtpVerifyConnectionTask;


/**
 * Gestisce la connessione al server FTP
 */
public class FtpSession {
    private final Context context;
    private final ServerFtp serverFtp;
    private FTPClient ftpClient;


    /**
     *
     * @param context Context chiamante
     * @param serverFtp Dati del server a cui connettersi
     */
    public FtpSession(@NonNull Context context, @NonNull ServerFtp serverFtp){
        this.context = context;
        this.serverFtp = serverFtp;
    }


    /**
     * Avvia un task per connessione al server
     * @param ftpConnectionListener Listener eseguito al termine dell'operazione
     */
    public void connect(final FtpConnectionListener ftpConnectionListener){
        new FtpConnectTask(context, serverFtp, ftpClient -> {
            FtpSession.this.ftpClient = ftpClient;
            if(ftpConnectionListener != null){
                ftpConnectionListener.onFtpConnection(ftpClient);
            }
        }).execute();
    }


    /**
     * Avvia un task per disconnettere il server
     */
    public void disconnect(){
        new FtpDisconnectTask(ftpClient).execute();
    }


    /**
     * Avvia un task per verificare la connessione del server
     * @param listener Listener eseguito al termine della verifica
     */
    public void verifyConnection(FtpVerifyConnectionTask.FtpVerifyConnectionListener listener){
        new FtpVerifyConnectionTask(ftpClient, listener).execute();
    }


    /**
     * Restituisce i dati del server impostato
     * @return Server FTP
     */
    public ServerFtp getServerFtp(){
        return this.serverFtp;
    }


    /**
     * Restituisce il client FTP ottenuto dalla connessione
     * @return Client FTP ottenuto dalla connessione. Null se la connessione non avviene
     */
    public FTPClient getFtpClient(){
        return this.ftpClient;
    }


    /**
     * Restituisce il context associato
     * @return Context
     */
    public Context getContext(){
        return this.context;
    }


    /**
     * Effettua la connessione nello stesso thread (Non usare nel thread della UI)
     */
    public void connectInTheSameThread(){
        new FtpConnectTask(context, serverFtp, ftpClient -> FtpSession.this.ftpClient = ftpClient).executeInTheSameThread();
    }
}
