package it.Ettore.egalfilemanager.ftp.thread;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

import it.Ettore.egalfilemanager.ftp.FtpElement;
import it.Ettore.egalfilemanager.ftp.FtpFileUtils;
import it.Ettore.egalfilemanager.ftp.FtpSession;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Task per l'esplorazione delle directory dei server FTP
 */
public class FtpLsTask extends AsyncTask<Void, Void, List<FtpElement>> {
    private final FtpSession ftpSession;
    private final String workingDirectoryPath;
    private final FtpLsListener listener;


    /**
     *
     * @param ftpSession Sessione FTP
     * @param workingDirectoryPath Path assoluto della directory da esplorare
     * @param listener Listener eseguito al termine dell'operazione
     */
    public FtpLsTask(FtpSession ftpSession, String workingDirectoryPath, FtpLsListener listener){
        this.ftpSession = ftpSession;
        this.workingDirectoryPath = workingDirectoryPath;
        this.listener = listener;
    }


    /**
     * Esplorazione in background
     * @param voids Nessun parametro
     * @return Lista di elementi trovati
     */
    @Override
    protected List<FtpElement> doInBackground(Void... voids) {
        if(ftpSession == null || ftpSession.getFtpClient() == null || ftpSession.getServerFtp() == null){
            return new ArrayList<>();
        } else {
            return FtpFileUtils.explorePath(ftpSession, workingDirectoryPath);
        }
    }


    /**
     * Esegue il listener
     * @param ftpFiles Lista di elementi trovati
     */
    @Override
    protected void onPostExecute(List<FtpElement> ftpFiles) {
        if(listener != null){
            listener.onFtpLsFinished(workingDirectoryPath, ftpFiles);
        }
    }




    /**
     * Listener di esplorazione
     */
    public interface FtpLsListener {

        /**
         * Chiamato al termine dell'esplorazione
         * @param pathDirectory Path della directory esplorata
         * @param listaFiles Lista elementi trovati
         */
        void onFtpLsFinished(String pathDirectory, List<FtpElement> listaFiles);
    }
}
