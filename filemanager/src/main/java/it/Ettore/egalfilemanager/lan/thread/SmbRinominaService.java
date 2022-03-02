package it.Ettore.egalfilemanager.lan.thread;

import android.content.Context;
import android.content.Intent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService;
import it.Ettore.egalfilemanager.lan.MultirinominaFilesSmb;
import it.Ettore.egalfilemanager.lan.OrdinatoreFilesLan;
import it.Ettore.egalfilemanager.lan.SerializableSmbFileList;
import it.Ettore.egalfilemanager.lan.SmbFileUtils;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/



/**
 * Servizio che esegue la rinominazione di files locali
 */
public class SmbRinominaService extends BaseProgressService {
    private static final String KEYBUNDLE_SMB_USER = "smb_user";
    private static final String KEYBUNDLE_SMB_PWD = "smb_password";
    private static final String KEYBUNDLE_NUOVO_NOME = "nuovo_nome";

    private String smbUser, smbPassword;
    private NtlmPasswordAuthentication auth;
    private List<SmbFile> filesVecchioNome, filesNuovoNome, filesNonRinominati;



    /**
     * Costruttore
     */
    public SmbRinominaService() {
        super("SmbRinominaService");
    }


    /**
     * Crea l'intent per l'esecuzione del service
     * @param context Context chiamante
     * @param listaFiles Lista di files da processare
     * @param smbUser User del server smb
     * @param smbPassword Password del server smb
     * @param nuovoNome Nuovo nome
     * @param handler Handler che permette al service di comunicare con la UI
     * @return Intent per l'esecuzione del service
     */
    public static Intent createStartIntent(@NonNull Context context, @NonNull List<SmbFile> listaFiles, String smbUser, String smbPassword, @NonNull String nuovoNome, @NonNull SmbRinominaHandler handler){
        final ArrayList<String> listaPaths = SmbFileUtils.listFileToListPath(listaFiles);
        final Intent intent = makeStartBaseIntent(context, SmbRinominaService.class, listaPaths, handler);
        intent.putExtra(KEYBUNDLE_SMB_USER, smbUser);
        intent.putExtra(KEYBUNDLE_SMB_PWD, smbPassword);
        intent.putExtra(KEYBUNDLE_NUOVO_NOME, nuovoNome);
        return intent;
    }


    /**
     * Esecuzione in background
     * @param intent Intent
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        super.onHandleIntent(intent);

        smbUser = intent.getStringExtra(KEYBUNDLE_SMB_USER);
        smbPassword = intent.getStringExtra(KEYBUNDLE_SMB_PWD);
        auth = SmbFileUtils.createAuth(smbUser, smbPassword);
        List<SmbFile> listaFiles = SmbFileUtils.listPathToListFile(getListaPaths(), auth);
        final String nuovoNome = intent.getStringExtra(KEYBUNDLE_NUOVO_NOME);

        //verifico la correttezza dei dati
        if (listaFiles == null || listaFiles.isEmpty() || nuovoNome == null) {
            sendMessageOperationFinished();
            return;
        }

        this.filesNonRinominati = new ArrayList<>();
        this.filesVecchioNome = new ArrayList<>();
        this.filesNuovoNome = new ArrayList<>();

        //notifico di avviare la progress dialog (e aggiorno la notifica)
        sendMessageStartOperation(getString(R.string.rinomina), null);

        //verifico la correttezza del nome
        if(!FileUtils.fileNameIsValid(nuovoNome)){
            filesNonRinominati = listaFiles;
            sendMessageError(R.string.nome_non_valido);
            sendMessageOperationFinished(); //sempre prima di ogni return in onHandleIntent()
            return;
        }

        //rinomino i files fisicamente
        try {
            if(listaFiles.size() == 1){
                final SmbFile fileDaRinominare = listaFiles.get(0);
                final String message = String.format(getString(R.string.rinominazione_in_corso), fileDaRinominare.getName());
                sendMessageUpdateProgress(message, 1, 1);
                rinomina(fileDaRinominare, nuovoNome);
            } else {
                listaFiles = OrdinatoreFilesLan.ordinaPerNome(listaFiles);
                final MultirinominaFilesSmb multirinomina = new MultirinominaFilesSmb(nuovoNome, listaFiles.size());
                for(int i=0; i < listaFiles.size(); i++){
                    final SmbFile fileDaRinominare = listaFiles.get(i);
                    if(!isRunning()){
                        filesNonRinominati.add(fileDaRinominare);
                        continue;
                    }
                    final String message = String.format(getString(R.string.rinominazione_in_corso), fileDaRinominare.getName());
                    sendMessageUpdateProgress(message, i+1, listaFiles.size());

                    final String nuovoNomeFile = multirinomina.getNuovoNomeFileProgressivo(fileDaRinominare, auth);
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
                for (SmbFile file : filesNonRinominati) {
                    sb.append(String.format("\n• %s", file.toString()));
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
    private void rinomina(SmbFile fileDaRinominare, String nuovoNome){
        try {
            final SmbFile nuovoFile = new SmbFile(fileDaRinominare.getParent() + nuovoNome, auth);
            if (nuovoFile.exists()) {
                filesNonRinominati.add(fileDaRinominare);
                sendMessageError(R.string.file_gia_presente);
            } else {
                try {
                    fileDaRinominare.renameTo(nuovoFile);
                    filesVecchioNome.add(fileDaRinominare);
                    filesNuovoNome.add(nuovoFile);
                } catch (SmbException e) {
                    filesNonRinominati.add(fileDaRinominare);
                }
            }
        } catch (Exception ignored){
            filesNonRinominati.add(fileDaRinominare);
        }
    }



    /**
     * Restituisce un oggetto serializzabile che contiene tutti i dati da passare poi al listener
     * (questo oggetto verrà passato al bundle dell'handler che a sua volta passerà i dati al listener)
     * @return Dati da passare al listener
     */
    @Override
    protected Serializable creaDatiPerListener() {
        final SmbRinominaHandler.ListenerData listenerData = new SmbRinominaHandler.ListenerData();
        listenerData.oldFiles = SerializableSmbFileList.fromFileList(filesVecchioNome, smbUser, smbPassword);
        listenerData.newFiles = SerializableSmbFileList.fromFileList(filesNuovoNome, smbUser, smbPassword);
        return listenerData;
    }
}
