package it.Ettore.egalfilemanager.lan.thread;

import android.content.Context;
import android.content.Intent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService;
import it.Ettore.egalfilemanager.lan.SerializableSmbFileList;
import it.Ettore.egalfilemanager.lan.SmbFileUtils;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/



/**
 * Servizio che esegue l'eliminazione di files smb
 */
public class SmbEliminaService extends BaseProgressService {
    private static final String KEYBUNDLE_SMB_USER = "smb_user";
    private static final String KEYBUNDLE_SMB_PWD = "smb_password";

    private List<SmbFile> filesCancellati;
    private String smbUser, smbPassword;


    /**
     * Costruttore
     */
    public SmbEliminaService() {
        super("SmbEliminaService");
    }


    /**
     * Crea l'intent per l'esecuzione del service
     * @param context Context chiamante
     * @param listaFiles Lista di files da processare
     * @param smbUser User del server smb
     * @param smbPassword Password del server smb
     * @param handler Handler che permette al service di comunicare con la UI
     * @return Intent per l'esecuzione del service
     */
    public static Intent createStartIntent(@NonNull Context context, @NonNull List<SmbFile> listaFiles, String smbUser, String smbPassword, @NonNull SmbEliminaHandler handler){
        final ArrayList<String> listaPaths = SmbFileUtils.listFileToListPath(listaFiles);
        final Intent intent = makeStartBaseIntent(context, SmbEliminaService.class, listaPaths, handler);
        intent.putExtra(KEYBUNDLE_SMB_USER, smbUser);
        intent.putExtra(KEYBUNDLE_SMB_PWD, smbPassword);
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
        final NtlmPasswordAuthentication auth = SmbFileUtils.createAuth(smbUser, smbPassword);
        final List<SmbFile> listaFiles = SmbFileUtils.listPathToListFile(getListaPaths(), auth);

        //verifico la correttezza dei dati
        if (listaFiles == null || listaFiles.isEmpty()) {
            sendMessageOperationFinished();
            return;
        }

        final List<SmbFile> filesNonCancellati = new ArrayList<>();
        this.filesCancellati = new ArrayList<>();

        //notifico di avviare la progress dialog (e aggiorno la notifica)
        sendMessageStartOperation(getString(R.string.elimina), null);

        for(int i=0; i < listaFiles.size(); i++){
            if (!isRunning()){
                sendMessageCanceled();
                sendMessageOperationFinished(); //sempre prima di ogni return in onHandleIntent()
                return;
            }
            final SmbFile file = listaFiles.get(i);
            final String message = String.format(getString(R.string.eliminazione_in_corso), file.getName().replace("/", ""));
            sendMessageUpdateProgress(message, i, listaFiles.size());
            try {
                file.delete(); //cancella anche le cartelle che contengono files
                filesCancellati.add(file);
            } catch (SmbException e){
                e.printStackTrace();
                filesNonCancellati.add(file);
            }
        }

        if(isRunning()){
            if(filesNonCancellati.isEmpty()){
                //operazione completata con successo
                sendMessageSuccessfully(String.format(getString(R.string.files_eliminati), String.valueOf(filesCancellati.size())));
            } else {
                //Mostro una dialog con la lista di files non processati
                final StringBuilder sb = new StringBuilder(getString(R.string.files_non_eliminati));
                sb.append("\n");
                for (SmbFile file : filesNonCancellati) {
                    sb.append(String.format("\n• %s", file.toString()));
                }
                sendMessageError(sb.toString());
            }
        }


        sendMessageOperationFinished();
    }


    /**
     * Restituisce un oggetto serializzabile che contiene tutti i dati da passare poi al listener
     * (questo oggetto verrà passato al bundle dell'handler che a sua volta passerà i dati al listener)
     * @return Dati da passare al listener
     */
    @Override
    protected Serializable creaDatiPerListener() {
        final SmbEliminaHandler.ListenerData listenerData = new SmbEliminaHandler.ListenerData();
        listenerData.deletedFiles = SerializableSmbFileList.fromFileList(filesCancellati, smbUser, smbPassword);
        return listenerData;
    }
}
