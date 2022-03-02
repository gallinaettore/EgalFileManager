package it.Ettore.egalfilemanager.lan;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.copyutils.CopyHandler;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.lan.thread.SmbAnalisiPreCopiaTask;
import it.Ettore.egalfilemanager.lan.thread.SmbAnalisiPreDownloadTask;
import it.Ettore.egalfilemanager.lan.thread.SmbAnalisiPreUploadTask;
import it.Ettore.egalfilemanager.lan.thread.SmbCreaCartellaTask;
import it.Ettore.egalfilemanager.lan.thread.SmbCreaFileTask;
import it.Ettore.egalfilemanager.lan.thread.SmbEliminaHandler;
import it.Ettore.egalfilemanager.lan.thread.SmbEliminaService;
import it.Ettore.egalfilemanager.lan.thread.SmbLsTask;
import it.Ettore.egalfilemanager.lan.thread.SmbProprietaTask;
import it.Ettore.egalfilemanager.lan.thread.SmbRinominaHandler;
import it.Ettore.egalfilemanager.lan.thread.SmbRinominaService;
import it.Ettore.egalfilemanager.mediastore.MediaScannerUtil;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;



/**
 * Classe per la gestione dei files su server smb
 */
public class SmbFileManager {
    private final Activity activity;
    private final NtlmPasswordAuthentication auth;
    private final String user, password;


    /**
     *
     * @param activity Activity chiamante
     * @param user Username (null se anonimo)
     * @param password Password (null se anonimo)
     */
    public SmbFileManager(@NonNull Activity activity, String user, String password){
        this.activity = activity;
        this.auth = SmbFileUtils.createAuth(user, password);
        this.user = user;
        this.password = password;
    }


    /**
     * Esegue un task per l'analisi del percorso
     * @param path Percorso da analizzare  (es. smb://192.168.1.x/)
     * @param listener Listener eseguito al termine dell'operazione
     */
    public void ls(@NonNull String path, SmbLsTask.SmbLsListener listener){
        new SmbLsTask(activity, path, auth, listener).execute();
    }


    /**
     * Esegue un task che crea una cartella
     * @param percorso Percorso in cui creare la cartella
     * @param nomeCartella Nome della cartella da creare
     * @param listener Listener eseguito al termine dell'operazione
     */
    public void creaCartella(String percorso, @NonNull String nomeCartella, SmbCreaCartellaTask.SmbNuovaCartellaListener listener){
        new SmbCreaCartellaTask(activity, percorso, nomeCartella, auth, listener).execute();
    }


    /**
     * Esegue un task che crea un file
     * @param percorso Percorso in cui creare il file
     * @param nomeFile Nome del file da creare
     * @param listener Listener eseguito al termine dell'operazione
     */
    public void creaFile(String percorso, String nomeFile, SmbCreaFileTask.SmbNuovoFileListener listener){
        new SmbCreaFileTask(activity, percorso, nomeFile, auth, listener).execute();
    }


    /**
     * Esegue un service che elimina i files o le cartelle selezionate
     * @param listaFiles Files o cartelle da eliminare
     * @param handler Handler per far comunicare il service con la UI
     */
    public void elimina(final List<SmbFile> listaFiles, @NonNull final SmbEliminaHandler handler){
        if(listaFiles == null) return;
        final List<SmbFile> daCancellare = new ArrayList<>(listaFiles);
        final CustomDialogBuilder builder = new CustomDialogBuilder(activity);
        builder.setType(CustomDialogBuilder.TYPE_WARNING);
        String message;
        if(daCancellare.size() == 1){
            message = String.format(activity.getString(R.string.conferma_eliminazione1), daCancellare.get(0).getName().replace("/",""));
        } else if (daCancellare.size() > 1){
            message = String.format(activity.getString(R.string.conferma_eliminazione2), String.valueOf(daCancellare.size()));
        } else {
            return;
        }
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
            final Intent intentService = SmbEliminaService.createStartIntent(activity, daCancellare, user, password, handler);
            startService(intentService);
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.create().show();
    }



    /**
     * Esegue un service che rinomina i files
     * @param listaFiles Files o cartelle da rinominare
     * @param nuovoNome Nuovo nome da assegnare
     * @param handler Handler per far comunicare il service con la UI
     */
    public void rinomina(@NonNull List<SmbFile> listaFiles, @NonNull String nuovoNome, @NonNull SmbRinominaHandler handler){
        if(listaFiles == null || nuovoNome == null) return;
        final Intent serviceIntent = SmbRinominaService.createStartIntent(activity, new ArrayList<>(listaFiles), user, password, nuovoNome, handler);
        startService(serviceIntent);
    }


    /**
     * Esegue un task per mostrare le proprietà
     * @param listaFiles Files o cartelle da analizzare
     */
    public void mostraProprieta(@NonNull List<SmbFile> listaFiles){
        new SmbProprietaTask(activity, new ArrayList<>(listaFiles)).execute();
    }


    /**
     * Esegue un task per copiare files che si trovano su percorsi smb, dentro altri percorsi smb
     * @param listaFiles Files o cartelle da copiare
     * @param userSorgente Username del server smb che ospita i files da copiare
     * @param pwdSorgente Password del server smb che ospita i files da copiare
     * @param pathDestinazione Path (cartella smb) di destinazione
     * @param copyHandler Handler per far comunicare il service con la UI
     */
    public void copia(@NonNull List<SmbFile> listaFiles, String userSorgente, String pwdSorgente, String pathDestinazione, CopyHandler copyHandler){
        new SmbAnalisiPreCopiaTask(activity, new ArrayList<>(listaFiles), userSorgente, pwdSorgente, pathDestinazione, user, password, copyHandler).execute();
    }


    /**
     * Esegue un task per copiare files che si trovano sul dispositivo, dentro percorsi smb
     * @param listaFiles Files o cartelle da copiare
     * @param pathDestinazione Path (cartella smb) di destinazione
     * @param cancellaOrigine True se si voglioni cancellare i files originali (modalità "taglia"). False modalità "copia"
     * @param copyHandler Handler per far comunicare il service con la UI
     */
    public void upload(@NonNull List<File> listaFiles, String pathDestinazione, boolean cancellaOrigine, CopyHandler copyHandler){
        final SmbAnalisiPreUploadTask task = new SmbAnalisiPreUploadTask(activity, new ArrayList<>(listaFiles), pathDestinazione, user, password, copyHandler);
        task.setCancellaOrigine(cancellaOrigine);
        task.execute();
    }



    /**
     * Esegue un task per copiare files che si trovano su percorsi smb, dentro cartelle sul dispositivo
     * @param listaFiles Lista files smb
     * @param destinazione Cartella di destinazione
     * @param copyHandler Handler per far comunicare il service con la UI
     * @param mediaScannerListener Listener del media Scanner
     */
    public void download(@NonNull List<SmbFile> listaFiles, File destinazione, CopyHandler copyHandler, MediaScannerUtil.MediaScannerListener mediaScannerListener){
        copyHandler.setMediaScannerListener(mediaScannerListener);
        new SmbAnalisiPreDownloadTask(activity, new ArrayList<>(listaFiles), user, password, destinazione, copyHandler).execute();
    }


    /**
     * Avvia il service, catturando l'eccezione se ci sono troppi elementi da gestire
     * @param serviceIntent Intent del service da avviare
     */
    private void startService(@NonNull Intent serviceIntent){
        try {
            ContextCompat.startForegroundService(activity, serviceIntent);
        } catch (Exception e){
            Log.e(getClass().getSimpleName(), e.getMessage());
            ColoredToast.makeText(activity, R.string.troppi_elementi_da_gestire, Toast.LENGTH_LONG).show();
        }
    }
}
