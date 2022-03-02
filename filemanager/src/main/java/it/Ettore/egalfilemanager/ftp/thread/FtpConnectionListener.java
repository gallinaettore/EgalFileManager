package it.Ettore.egalfilemanager.ftp.thread;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import org.apache.commons.net.ftp.FTPClient;


/**
 * Listener di connessione FTP
 */
public interface FtpConnectionListener {

    /**
     * Chiamato in seguito al comando di connessione al server
     * @param ftpClient Client FTP per gestire il server. Null se non è possibile collegarsi al server.
     */
    void onFtpConnection(FTPClient ftpClient);
}
