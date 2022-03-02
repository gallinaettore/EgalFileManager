package it.Ettore.egalfilemanager.ftp.thread;

import android.content.Context;
import android.content.Intent;

import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService;
import it.Ettore.egalfilemanager.ftp.FtpElement;
import it.Ettore.egalfilemanager.ftp.FtpFileUtils;
import it.Ettore.egalfilemanager.ftp.FtpSession;
import it.Ettore.egalfilemanager.ftp.MultirinominaFilesFtp;
import it.Ettore.egalfilemanager.ftp.OrdinatoreFilesFtp;
import it.Ettore.egalfilemanager.ftp.ServerFtp;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


public class FtpRinominaService extends BaseProgressService {
    private static final String KEYBUNDLE_SERVER_FTP = "server_ftp";
    private static final String KEYBUNDLE_FTP_ELEMENTS = "ftp_elements";
    private static final String KEYBUNDLE_NUOVO_NOME = "nuovo_nome";

    private FTPClient ftpClient;
    private List<FtpElement> filesNonRinominati, filesVecchioNome;
    private List<String> pathsNuovoNome;




    /**
     * Costruttore
     */
    public FtpRinominaService() {
        super("FtpRinominaService");
    }


    /**
     * Crea l'intent per l'esecuzione del service
     * @param context Context chiamante
     * @param listaFiles Lista di files da processare
     * @param serverFtp Dati del server FTP
     * @param handler Handler che permette al service di comunicare con la UI
     * @return Intent per l'esecuzione del service
     */
    public static Intent createStartIntent(@NonNull Context context, @NonNull List<FtpElement> listaFiles, @NonNull String nuovoNome, @NonNull ServerFtp serverFtp, @NonNull FtpRinominaHandler handler){
        final Intent intent = makeStartBaseIntent(context, FtpRinominaService.class, null, handler);
        intent.putExtra(KEYBUNDLE_FTP_ELEMENTS, new ArrayList<>(listaFiles));
        intent.putExtra(KEYBUNDLE_SERVER_FTP, serverFtp.toString());
        intent.putExtra(KEYBUNDLE_NUOVO_NOME, nuovoNome);
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
        final FtpSession ftpSession = new FtpSession(this, serverFtp);
        List<FtpElement> listaFiles = (List<FtpElement>) intent.getSerializableExtra(KEYBUNDLE_FTP_ELEMENTS);
        final String nuovoNome = intent.getStringExtra(KEYBUNDLE_NUOVO_NOME);

        //verifico la correttezza dei dati
        if (listaFiles == null || listaFiles.isEmpty() || nuovoNome == null) {
            sendMessageOperationFinished();
            return;
        }

        this.filesNonRinominati = new ArrayList<>();
        this.filesVecchioNome = new ArrayList<>();
        this.pathsNuovoNome = new ArrayList<>();


        //notifico di avviare la progress dialog (e aggiorno la notifica)
        sendMessageStartOperation(getString(R.string.rinomina), null);

        //connessione
        ftpSession.connectInTheSameThread();

        //verifico la connessione
        ftpClient = ftpSession.getFtpClient();
        if (ftpClient == null) {
            sendMessageError(R.string.files_non_eliminati);
            sendMessageOperationFinished(); //sempre prima di ogni return in onHandleIntent()
            return;
        }

        //verifico la validità del nome
        if(!FileUtils.fileNameIsValid(nuovoNome)){
            filesNonRinominati = listaFiles;
            sendMessageError(R.string.nome_non_valido);
            sendMessageOperationFinished(); //sempre prima di ogni return in onHandleIntent()
            return;
        }

        //rinomino i files fisicamente
        try {
            if(listaFiles.size() == 1){
                final FtpElement fileDaRinominare = listaFiles.get(0);
                final String message = String.format(getString(R.string.rinominazione_in_corso), fileDaRinominare.getName());
                sendMessageUpdateProgress(message, 1, 1);
                rinomina(fileDaRinominare, nuovoNome);
            } else {
                listaFiles = OrdinatoreFilesFtp.ordinaPerNome(listaFiles);
                final MultirinominaFilesFtp multirinomina = new MultirinominaFilesFtp(nuovoNome, listaFiles.size());
                for(int i=0; i < listaFiles.size(); i++){
                    final FtpElement fileDaRinominare = listaFiles.get(i);
                    if(!isRunning()){
                        filesNonRinominati.add(fileDaRinominare);
                        continue;
                    }
                    final String message = String.format(getString(R.string.rinominazione_in_corso), fileDaRinominare.getName());
                    sendMessageUpdateProgress(message, i+1, listaFiles.size());

                    final String nuovoNomeFile = multirinomina.getNuovoNomeFileProgressivo(ftpClient, fileDaRinominare);
                    if(nuovoNomeFile != null){
                        rinomina(fileDaRinominare, nuovoNomeFile);
                    } else {
                        filesNonRinominati.add(fileDaRinominare);
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
            filesNonRinominati = listaFiles;
        }

        if(isRunning()){
            if(filesNonRinominati.isEmpty()) {
                //tutti i files sono stati rinominati
                sendMessageSuccessfully(String.format(getString(R.string.files_rinominati), String.valueOf(listaFiles.size())));
            } else {
                //Mostro una dialogRinomina con la lista di files non processati
                final StringBuilder sb = new StringBuilder(getString(R.string.files_non_rinominati));
                sb.append("\n");
                for (FtpElement file : filesNonRinominati) {
                    sb.append(String.format("\n• %s", file.getFullPath()));
                }
                sendMessageError(sb.toString());
            }
        }

        sendMessageOperationFinished();
    }


    /**
     * Rinomina il file
     * @param fileDaRinominare File
     * @param nuovoNome Nome da assegnare al file
     */
    private void rinomina(FtpElement fileDaRinominare, String nuovoNome){
        final String nuovoFilePath = fileDaRinominare.getParent() + "/" + nuovoNome;
        if(FtpFileUtils.fileExists(ftpClient, nuovoFilePath)){
            filesNonRinominati.add(fileDaRinominare);
            sendMessageError(R.string.file_gia_presente);
        } else {
            try {
                boolean renamed = ftpClient.rename(fileDaRinominare.getAbsolutePath(), nuovoFilePath);
                if (renamed) {
                    filesVecchioNome.add(fileDaRinominare);
                    pathsNuovoNome.add(nuovoFilePath);
                } else {
                    filesNonRinominati.add(fileDaRinominare);
                }
            } catch (IOException e){
                e.printStackTrace();
                filesNonRinominati.add(fileDaRinominare);
            }
        }
    }




    @Override
    protected Serializable creaDatiPerListener() {
        final FtpRinominaHandler.ListenerData listenerData = new FtpRinominaHandler.ListenerData();
        listenerData.oldFiles = filesVecchioNome;
        listenerData.newFilesPaths = pathsNuovoNome;
        return listenerData;
    }
}
