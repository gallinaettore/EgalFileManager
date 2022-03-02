package it.Ettore.egalfilemanager.ftp;
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
import it.Ettore.egalfilemanager.ftp.thread.FtpAnalisiPreCopiaTask;
import it.Ettore.egalfilemanager.ftp.thread.FtpAnalisiPreDownloadTask;
import it.Ettore.egalfilemanager.ftp.thread.FtpAnalisiPreUploadTask;
import it.Ettore.egalfilemanager.ftp.thread.FtpCreaCartellaTask;
import it.Ettore.egalfilemanager.ftp.thread.FtpEliminaHandler;
import it.Ettore.egalfilemanager.ftp.thread.FtpEliminaService;
import it.Ettore.egalfilemanager.ftp.thread.FtpLsTask;
import it.Ettore.egalfilemanager.ftp.thread.FtpProprietaTask;
import it.Ettore.egalfilemanager.ftp.thread.FtpRinominaHandler;
import it.Ettore.egalfilemanager.ftp.thread.FtpRinominaService;
import it.Ettore.egalfilemanager.mediastore.MediaScannerUtil;


/**
 * Classe per la gestione dei files su server smb
 */
public class FtpFileManager {
    private final FtpSession ftpSession;
    private final Activity activity;


    /**
     *
     * @param activity Activity chiamante
     * @param ftpSession Sessione FTP
     */
    public FtpFileManager(@NonNull Activity activity, @NonNull FtpSession ftpSession){
        this.activity = activity;
        this.ftpSession = ftpSession;
    }


    /**
     * Esegue un task per l'analisi del percorso
     * @param workingDirectoryPath Path della directory da analizzare (nel formato: /dir1/dir2/dir3). Null per la directory root
     * @param lsListener Listener eseguito al termine dell'operazione
     */
    public void ls(final String workingDirectoryPath, final FtpLsTask.FtpLsListener lsListener){
        if(ftpSession == null) return;
        ftpSession.verifyConnection(isConnected -> {
            if(isConnected){
                new FtpLsTask(ftpSession, workingDirectoryPath, lsListener).execute();
            } else {
                //se non connesso eseguo il task dopo la connessione
                ftpSession.connect(ftpClient -> new FtpLsTask(ftpSession, workingDirectoryPath, lsListener).execute());
            }
        });
    }


    /**
     * Esegue un task che crea una cartella
     * @param percorso Percorso in cui creare la cartella
     * @param nomeCartella Nome della cartella da creare
     * @param listener Listener eseguito al termine dell'operazione
     */
    public void creaCartella(final String percorso, final String nomeCartella, final FtpCreaCartellaTask.FtpNuovaCartellaListener listener){
        if(ftpSession == null) return;
        ftpSession.verifyConnection(isConnected -> {
            if(isConnected){
                new FtpCreaCartellaTask(activity, percorso, nomeCartella, ftpSession, listener).execute();
            } else {
                //se non connesso eseguo il task dopo la connessione
                ftpSession.connect(ftpClient -> new FtpCreaCartellaTask(activity, percorso, nomeCartella, ftpSession, listener).execute());
            }
        });
    }


    /**
     * Esegue un service che elimina i files o le cartelle selezionate
     * @param listaFiles Files o cartelle da eliminare
     * @param handler Handler per far comunicare il service con la UI
     */
    public void elimina(@NonNull List<FtpElement> listaFiles, @NonNull FtpEliminaHandler handler){
        if(ftpSession == null || listaFiles == null) return;

        final List<FtpElement> daCancellare = new ArrayList<>(listaFiles);
        final Intent serviceIntent = FtpEliminaService.createStartIntent(activity, daCancellare, ftpSession.getServerFtp(), handler);
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
        builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> ftpSession.verifyConnection(isConnected -> {
            if(isConnected){
                startService(serviceIntent);
            } else {
                //se non connesso eseguo il task dopo la connessione
                ftpSession.connect(ftpClient -> startService(serviceIntent));
            }
        }));
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.create().show();
    }



    /**
     * Esegue un task che rinomina i files
     * @param listaFiles Files o cartelle da rinominare
     * @param nuovoNome Nuovo nome da assegnare
     * @param handler Handler per far comunicare il service con la UI
     */
    public void rinomina(@NonNull List<FtpElement> listaFiles, @NonNull final String nuovoNome, @NonNull final FtpRinominaHandler handler){
        if(ftpSession == null || listaFiles == null || nuovoNome == null) return;
        final List<FtpElement> daRinominare = new ArrayList<>(listaFiles);
        final Intent serviceIntent = FtpRinominaService.createStartIntent(activity, daRinominare, nuovoNome, ftpSession.getServerFtp(), handler);
        ftpSession.verifyConnection(isConnected -> {
            if(isConnected){
                startService(serviceIntent);
            } else {
                //se non connesso eseguo il task dopo la connessione
                ftpSession.connect(ftpClient -> startService(serviceIntent));
            }
        });
    }



    /**
     * Esegue un task per mostrare le proprietà
     * @param listaFiles Files o cartelle da analizzare
     */
    public void mostraProprieta(@NonNull  List<FtpElement> listaFiles){
        if(ftpSession == null) return;
        final List<FtpElement> daAnalizzare = new ArrayList<>(listaFiles);
        ftpSession.verifyConnection(isConnected -> {
            if(isConnected){
                new FtpProprietaTask(activity, daAnalizzare, ftpSession).execute();
            } else {
                //se non connesso eseguo il task dopo la connessione
                ftpSession.connect(ftpClient -> new FtpProprietaTask(activity, daAnalizzare, ftpSession).execute());
            }
        });
    }


    /**
     * Esegue un task per copiare files che si trovano su percorsi FTP, dentro cartelle sul dispositivo
     * @param listaFiles Lista files FTP
     * @param destinazione Cartella locale di destinazione
     * @param copyHandler Handler per far comunicare il service con la UI
     * @param mediaScannerListener Listener del media Scanner
     */
    public void download(@NonNull List<FtpElement> listaFiles, final File destinazione, final CopyHandler copyHandler, MediaScannerUtil.MediaScannerListener mediaScannerListener) {
        if(ftpSession == null) return;
        copyHandler.setMediaScannerListener(mediaScannerListener);
        final List<FtpElement> daCopiare = new ArrayList<>(listaFiles);
        ftpSession.verifyConnection(isConnected -> {
            if(isConnected){
                new FtpAnalisiPreDownloadTask(activity, daCopiare, ftpSession, destinazione, copyHandler).execute();
            } else {
                //se non connesso eseguo il task dopo la connessione
                ftpSession.connect(ftpClient -> new FtpAnalisiPreDownloadTask(activity, daCopiare, ftpSession, destinazione, copyHandler).execute());
            }
        });
    }


    /**
     * Esegue un task per copiare files che si trovano sul dispositivo, dentro percorsi FTP
     * @param listaFiles Files o cartelle da copiare
     * @param pathDestinazione Path della cartella FTP di destinazione
     * @param cancellaOrigine True se si voglioni cancellare i files originali (modalità "taglia"). False modalità "copia"
     * @param copyHandler Handler per far comunicare il service con la UI
     */
    public void upload(@NonNull List<File> listaFiles, final String pathDestinazione, final boolean cancellaOrigine, final CopyHandler copyHandler){
        if(ftpSession == null) return;
        final List<File> daCopiare = new ArrayList<>(listaFiles);
        ftpSession.verifyConnection(isConnected -> {
            if(isConnected){
                new FtpAnalisiPreUploadTask(activity, daCopiare, pathDestinazione, ftpSession, copyHandler, cancellaOrigine).execute();
            } else {
                //se non connesso eseguo il task dopo la connessione
                ftpSession.connect(ftpClient -> new FtpAnalisiPreUploadTask(activity, daCopiare, pathDestinazione, ftpSession, copyHandler, cancellaOrigine).execute());
            }
        });
    }


    /**
     * Esegue un task per copiare files che si trovano su percorsi smb, dentro altri percorsi smb
     * @param listaFiles Files o cartelle da copiare
     * @param serverFtpOrigine Dati del server che ospita i files da copiare
     * @param pathDestinazione Path della cartella FTP di destinazione
     * @param copyHandler Handler per far comunicare il service con la UI
     */
    public void copia(@NonNull List<FtpElement> listaFiles, final ServerFtp serverFtpOrigine, final String pathDestinazione, final CopyHandler copyHandler){
        if(ftpSession == null || serverFtpOrigine == null) return;
        final FtpSession ftpSessionOrigine = new FtpSession(ftpSession.getContext(), serverFtpOrigine);
        final List<FtpElement> daCopiare = new ArrayList<>(listaFiles);
        //verifica la connessione del server di destinazione
        ftpSession.verifyConnection(isConnected -> {
            if(isConnected){
                //se la destinazione è connessa, connetto anche il server di origine e avvio il task
                connettiOrigineEAvviaAnalisiPreCopia(daCopiare, ftpSessionOrigine, pathDestinazione, copyHandler);
            } else {
                //se la destinazione non è connessa, la connetto, connetto anche il server di origine e avvio il task
                ftpSession.connect(ftpClient -> {
                    if(ftpClient != null){
                        connettiOrigineEAvviaAnalisiPreCopia(daCopiare, ftpSessionOrigine, pathDestinazione, copyHandler);
                    }
                });
            }
        });
    }


    /**
     * Connette al server FTP che contiene i files da copiare e avvia il task di analisi pre-copia
     * @param listaFiles Files o cartelle da copiare
     * @param ftpSessionOrigine Sessione del server che ospita i files da copiare
     * @param pathDestinazione Path della cartella FTP di destinazione
     * @param copyHandler Handler per far comunicare il service con la UI
     */
    private void connettiOrigineEAvviaAnalisiPreCopia(final List<FtpElement> listaFiles, final FtpSession ftpSessionOrigine, final String pathDestinazione, final CopyHandler copyHandler){
        ftpSessionOrigine.connect(ftpClient -> {
            if(ftpClient != null){
                //se anche l'origine è connessa avvio il task
                new FtpAnalisiPreCopiaTask(activity, listaFiles, ftpSessionOrigine, pathDestinazione, ftpSession, copyHandler).execute();
            }
        });
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
