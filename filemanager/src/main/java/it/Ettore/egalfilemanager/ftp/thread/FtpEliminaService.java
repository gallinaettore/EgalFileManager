package it.Ettore.egalfilemanager.ftp.thread;

import android.content.Context;
import android.content.Intent;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService;
import it.Ettore.egalfilemanager.ftp.FtpElement;
import it.Ettore.egalfilemanager.ftp.FtpFileUtils;
import it.Ettore.egalfilemanager.ftp.FtpSession;
import it.Ettore.egalfilemanager.ftp.OrdinatoreFilesFtp;
import it.Ettore.egalfilemanager.ftp.ServerFtp;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


public class FtpEliminaService extends BaseProgressService {
    private static final String KEYBUNDLE_SERVER_FTP = "server_ftp";
    private static final String KEYBUNDLE_FTP_ELEMENTS = "ftp_elements";

    private FtpSession ftpSession;
    private List<FtpElement> filesCancellati, filesDaCancellare;


    /**
     * Costruttore
     */
    public FtpEliminaService() {
        super("FtpEliminaService");
    }


    /**
     * Crea l'intent per l'esecuzione del service
     * @param context Context chiamante
     * @param listaFiles Lista di files da processare
     * @param serverFtp Dati del server FTP
     * @param handler Handler che permette al service di comunicare con la UI
     * @return Intent per l'esecuzione del service
     */
    public static Intent createStartIntent(@NonNull Context context, @NonNull List<FtpElement> listaFiles, @NonNull ServerFtp serverFtp, @NonNull FtpEliminaHandler handler){
        final Intent intent = makeStartBaseIntent(context, FtpEliminaService.class, null, handler);
        intent.putExtra(KEYBUNDLE_FTP_ELEMENTS, new ArrayList<>(listaFiles));
        intent.putExtra(KEYBUNDLE_SERVER_FTP, serverFtp.toString());
        return intent;
    }


    /**
     * Esecuzione in background
     * @param intent Intent
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void onHandleIntent(@Nullable Intent intent) {
        super.onHandleIntent(intent);

        final ServerFtp serverFtp = ServerFtp.fromJson(this, intent.getStringExtra(KEYBUNDLE_SERVER_FTP));
        this.ftpSession = new FtpSession(this, serverFtp);
        List<FtpElement> listaFiles = (List<FtpElement>)intent.getSerializableExtra(KEYBUNDLE_FTP_ELEMENTS);

        //verifico la correttezza dei dati
        if (listaFiles == null || listaFiles.isEmpty()) {
            sendMessageOperationFinished();
            return;
        }

        this.filesDaCancellare = new ArrayList<>();
        this.filesCancellati = new ArrayList<>();
        final List<FtpElement> filesNonCancellati = new ArrayList<>();


        //notifico di avviare la progress dialog (e aggiorno la notifica)
        sendMessageStartOperation(getString(R.string.elimina), null);

        //connessione
        ftpSession.connectInTheSameThread();

        //verifico la connessione
        if(ftpSession.getFtpClient() == null){
            sendMessageError(R.string.files_non_eliminati);
            sendMessageOperationFinished(); //sempre prima di ogni return in onHandleIntent()
            return;
        }

        //analizzo i files
        analisiRicorsiva(listaFiles);

        //procedo con l'eliminazione
        for(int i=0; i < filesDaCancellare.size(); i++){
            if (!isRunning()){
                sendMessageCanceled();
                sendMessageOperationFinished(); //sempre prima di ogni return in onHandleIntent()
                return;
            }
            final FtpElement file = filesDaCancellare.get(i);
            final String message = String.format(getString(R.string.eliminazione_in_corso), file.getName());
            sendMessageUpdateProgress(message, i, filesDaCancellare.size());
            boolean deleted = false;
            if(file.isDirectory()){
                try {
                    deleted = ftpSession.getFtpClient().removeDirectory(file.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    deleted = ftpSession.getFtpClient().deleteFile(file.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(deleted){
                filesCancellati.add(file);
            } else {
                filesNonCancellati.add(file);
            }
        }

        if(isRunning()){
            if(filesNonCancellati.isEmpty()){
                //l'operazione è andata a buon fine
                sendMessageSuccessfully(String.format(getString(R.string.files_eliminati), String.valueOf(filesCancellati.size())));
            } else {
                //Mostro una dialog con la lista di files non processati
                final StringBuilder sb = new StringBuilder(getString(R.string.files_non_eliminati));
                sb.append("\n");
                for (FtpElement file : filesNonCancellati) {
                    sb.append(String.format("\n• %s", file.getFullPath()));
                }
                sendMessageError(sb.toString());
            }
        }

        sendMessageOperationFinished();
    }


    /**
     * Analisi ricorsiva dei files da cancellare
     * @param daAnalizzare Lista di files da analizzare
     */
    private void analisiRicorsiva(final List<FtpElement> daAnalizzare){
        List<FtpElement> listaOrdinata = new ArrayList<>(daAnalizzare);
        listaOrdinata = OrdinatoreFilesFtp.ordinaPerNome(listaOrdinata);
        for(FtpElement file : listaOrdinata){
            if (!isRunning()){
                sendMessageCanceled();
                return;
            }
            if(file.isDirectory()){
                final List<FtpElement> elementiNellaCartella = FtpFileUtils.explorePath(ftpSession, file.getAbsolutePath());
                analisiRicorsiva(elementiNellaCartella);
            }
            filesDaCancellare.add(file);
        }
    }




    @Override
    protected Serializable creaDatiPerListener() {
        final FtpEliminaHandler.ListenerData listenerData = new FtpEliminaHandler.ListenerData();
        listenerData.deletedFiles = filesCancellati;
        return listenerData;
    }
}
