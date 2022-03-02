package it.Ettore.egalfilemanager.ftp.thread;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.IOException;
import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.ui.ColoredProgressDialog;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.ftp.ServerFtp;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Task che effettua la connessione al server FTP
 */
public class FtpConnectTask extends AsyncTask<Void, Void, Boolean> {
    private final WeakReference<Context> context;
    private final ServerFtp serverFtp;
    private final FtpConnectionListener listener;
    private ColoredProgressDialog progress;
    private FTPClient ftpClient;
    private boolean erroreUserPassword;


    /**
     *
     * @param context Context chiamante
     * @param serverFtp Dati del server FTP
     * @param listener Listener eseguito al termine della connessione
     */
    public FtpConnectTask(@NonNull Context context, @NonNull ServerFtp serverFtp, FtpConnectionListener listener){
        this.context = new WeakReference<>(context);
        this.serverFtp = serverFtp;
        this.listener = listener;
    }


    /**
     * Mostro la dialog
     */
    @Override
    protected void onPreExecute(){
        if(context.get() instanceof Activity){
            progress = ColoredProgressDialog.show(context.get(), null, context.get().getString(R.string.connessione_in_corso));
            progress.setCancelable(false);
        }
    }


    /**
     * Effettua la connessione in background
     * @param params Nessun parametro
     * @return True se le connessione avviene con successo
     */
    @Override
    protected Boolean doInBackground(Void... params) {
        if(serverFtp == null) return false;

        if(serverFtp.getTipo() == ServerFtp.TIPO_FTP){
            ftpClient = new FTPClient();
        } else if(serverFtp.getTipo() == ServerFtp.TIPO_FTPS){
            ftpClient = new FTPSClient();
        }

        try {
            ftpClient.setDefaultPort(serverFtp.getPorta());
            if(serverFtp.getCodifica() != null) {
                ftpClient.setControlEncoding(serverFtp.getCodifica());
            }
            ftpClient.connect(serverFtp.getHost());
            // After connection attempt, you should check the reply code to verify success.
            int reply = ftpClient.getReplyCode();

            if(!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                return false;
            }

            boolean login = ftpClient.login(serverFtp.getUsername(), serverFtp.getPassword());
            if(!login){
                erroreUserPassword = true;
                ftpClient.logout();
                ftpClient.disconnect();
                return false;
            } else {
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                if (serverFtp.getModalita() == ServerFtp.MODALITA_PASSIVO) {
                    ftpClient.enterLocalPassiveMode();
                } else {
                    ftpClient.enterLocalActiveMode();
                }
                if(serverFtp.getCodifica() == null) {
                    ftpClient.setAutodetectUTF8(true);
                }
                ftpClient.setSoTimeout(20000); //timeout di risposta del socket, non è il timeout di disconnessione dal server ftp
                return true;
            }

        } catch(IOException e) {
            e.printStackTrace();
            if(ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch(IOException ignored) {}
            }
            return false;
        }

    }


    /**
     * Mostra le dialog di conferma o di errore
     * @param success True se l'operazione è andata a buon fine
     */
    @Override
    protected void onPostExecute(Boolean success){
        try {
            if (progress != null && progress.isShowing()) {
                progress.dismiss();
            }
        } catch (final IllegalArgumentException ignored) {}

        if(!success && context.get() != null && context.get() instanceof Activity && !((Activity)context.get()).isFinishing() && serverFtp != null){
            String errorMessage;
            if(erroreUserPassword){
                errorMessage = context.get().getString(R.string.user_password_errati);
            } else {
                errorMessage = context.get().getString(R.string.impossibile_connettersi, serverFtp.getHost());
            }
            CustomDialogBuilder.make(context.get(), errorMessage, CustomDialogBuilder.TYPE_ERROR).show();
        }

        if(listener != null){
            listener.onFtpConnection(success ? ftpClient : null);
        }
    }


    /**
     * Metodo da chiamare in un thread che non sia quello dell'UI. Esegue il task nello stesso thread.
     */
    public void executeInTheSameThread(){
        final Boolean success = doInBackground();
        onPostExecute(success);
    }
}
