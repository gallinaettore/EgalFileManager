package it.Ettore.egalfilemanager.lan.thread;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.copyutils.AnalisiResult;
import it.Ettore.egalfilemanager.copyutils.BaseAnalisiTask;
import it.Ettore.egalfilemanager.copyutils.CopyHandler;
import it.Ettore.egalfilemanager.copyutils.CopyHandlerListener;
import it.Ettore.egalfilemanager.copyutils.CopyService;
import it.Ettore.egalfilemanager.copyutils.SovrascritturaFiles;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.filemanager.FileManager;
import it.Ettore.egalfilemanager.filemanager.OrdinatoreFiles;
import it.Ettore.egalfilemanager.lan.SmbFileUtils;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;


/**
 * Task per l'analisi dei files prima della copia di files del dispositivon all'interno di un percorso smb
 */
public class SmbAnalisiPreUploadTask extends BaseAnalisiTask implements SovrascritturaFiles.SovrascritturaListener<File> {
    private final WeakReference<Activity> activity;
    private List<File> listaFiles;
    private SmbFile destinazione;
    private final NtlmPasswordAuthentication auth;
    private AnalisiResult<File> analisiResult;
    private long freeSpace;
    private final FileManager fileManager;
    private final String smbUser, smbPassword;
    private final CopyHandler handler;


    /**
     *
     * @param activity Activity chiamante
     * @param listafiles Lista files da copiare
     * @param destinazione Path della cartella di destinazione
     * @param smbUser Username del server smb
     * @param smbPassword Password del server smb
     * @param handler Handler di copia per mostrare i dati ricevuti dal service
     */
    public SmbAnalisiPreUploadTask(@NonNull Activity activity, List<File> listafiles, String destinazione, String smbUser, String smbPassword, CopyHandler handler){
        super(activity);
        this.fileManager = new FileManager(activity);
        this.fileManager.ottieniStatoRootExplorer();
        this.activity = new WeakReference<>(activity);
        this.listaFiles = listafiles;
        this.smbUser = smbUser;
        this.smbPassword = smbPassword;
        this.auth = SmbFileUtils.createAuth(smbUser, smbPassword);
        try {
            this.destinazione = new SmbFile(destinazione, auth);
        } catch (MalformedURLException ignored) {}
        this.handler = handler;
    }


    /**
     * Analisi in background
     * @param voids Nessun parametro
     * @return False solo se ci sono errori di tipo nullpointer
     */
    @Override
    protected Boolean doInBackground(Void... voids) {
        if(listaFiles == null || listaFiles.isEmpty() || destinazione == null) return false;

        //Ordino i files per nome crescente
        listaFiles = OrdinatoreFiles.ordinaPerNome(listaFiles);

        //Calcolo la dimensione totale dei files da copiare
        analisiResult = analizza(listaFiles, destinazione, auth);
        try {
            freeSpace = destinazione.getDiskFreeSpace();
        } catch (SmbException e) {
            e.printStackTrace();
        }

        return true;
    }


    /**
     * Se l'analisi ha avuto un esito positivo avvia la copia altrimenti vengono mostrate le dialogs di errore
     * @param success Successo dell'analisi
     */
    @Override
    protected void onPostExecute(Boolean success){
        dismissWaitDialog();
        if(listaFiles == null || listaFiles.isEmpty() || destinazione == null || !success || analisiResult == null) return;

        if(activity.get() != null && !activity.get().isFinishing()){
            if(analisiResult.totalSize >= freeSpace){
                CustomDialogBuilder.make(activity.get(), R.string.spazio_insufficiente, CustomDialogBuilder.TYPE_ERROR).show();
            } else {
                //procedi
                final List<String> nomiFiles = new ArrayList<>(analisiResult.filesGiaEsistenti.size());
                for(File f : analisiResult.filesGiaEsistenti){
                    nomiFiles.add(f.getName().replace("/", ""));
                }
                final SovrascritturaFiles<File> sovrascritturaFiles = new SovrascritturaFiles<>(activity.get(), analisiResult.filesGiaEsistenti, nomiFiles, SmbAnalisiPreUploadTask.this);
                if(!listaFiles.isEmpty() && listaFiles.get(0).getParent().equals(destinazione.getPath())){
                    //Se la directory di destinazione è la stessa di quella della copia, duplico i files rinominandoli
                    sovrascritturaFiles.applicaATuttiRestanti(SovrascritturaFiles.AZIONE_RINOMINA);
                } else if(analisiResult.filesGiaEsistenti.isEmpty()){
                    //destinazione diversa senza file da copiare già presenti
                    avviaCopia(new HashMap<>());
                } else {
                    //destinazione diversa che contiene però già alcuni files
                    sovrascritturaFiles.mostraDialogSovrascrittura();
                }
                return; //il listener sarà richiamato dalla copia vera e propria
            }

            eseguiListenerCopiaNonCompletata();
        }
    }


    /**
     * Analizza ricorsivamente i dati passati per ottenere la dimensione totale, il numero totale di files e la lista di files già presenti nella destinazione
     * @param listaFiles Lista files da analizzare
     * @param directoryDestinazione Directory in cui dovrebbero essere copiati
     * @param auth Credenziali della directory di destinazione
     * @return Dati con la dimensione totale, il numero totale di files e la lista di files già presenti nella destinazione
     */
    private AnalisiResult<File> analizza(List<File> listaFiles, SmbFile directoryDestinazione, NtlmPasswordAuthentication auth){
        if(listaFiles == null) return null;
        long totalSize = 0L;
        int totalFiles = 0;
        final List<File> filesGiaEsistenti = new ArrayList<>();
        for(File file : listaFiles){
            if(file == null) return null;
            final SmbFile nuovaDestinazione;
            try {
                nuovaDestinazione = new SmbFile(directoryDestinazione + file.getName() + "/", auth);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }
            if(!file.isDirectory()){
                totalSize += file.length();
                totalFiles++;
                try {
                    if(nuovaDestinazione.exists()){
                        filesGiaEsistenti.add(file);
                    }
                } catch (SmbException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                List<File> listaFilesSottodirectory = fileManager.ls(file); //utilizzando il file manager posso usare anche i permessi di root
                if(listaFilesSottodirectory != null) {
                    listaFilesSottodirectory = OrdinatoreFiles.ordinaPerNome(listaFilesSottodirectory);
                    final AnalisiResult<File> result = analizza(listaFilesSottodirectory, nuovaDestinazione, auth);
                    if(result == null) return null;
                    totalSize += result.totalSize;
                    totalFiles += result.totalFiles;
                    filesGiaEsistenti.addAll(result.filesGiaEsistenti);
                }
            }
        }
        return new AnalisiResult<>(totalSize, totalFiles, filesGiaEsistenti);
    }


    /**
     * Avvia il task di copia
     * @param azioniFilesGiaPresenti Map contenente le associazioni files-azione da eseguire
     */
    private void avviaCopia(Map<File, Integer> azioniFilesGiaPresenti){
        if(listaFiles != null && !listaFiles.isEmpty()) {
            if(!SmbUploadService.isRunning()) {
                final Intent serviceIntent = SmbUploadService.createStartIntent(activity.get(), listaFiles, smbUser, smbPassword, destinazione, analisiResult.totalSize,
                        analisiResult.totalFiles, azioniFilesGiaPresenti, handler, isCancellaOrigine());
                try {
                    ContextCompat.startForegroundService(activity.get(), serviceIntent);
                } catch (Exception e){
                    Log.e(getClass().getSimpleName(), e.getMessage());
                    ColoredToast.makeText(activity.get(), R.string.troppi_elementi_da_gestire, Toast.LENGTH_LONG).show();
                    eseguiListenerCopiaNonCompletata();
                }
            }
        } else {
            eseguiListenerCopiaNonCompletata();
        }
    }


    /**
     * Metodo da chiamare quando non è possibile avviare la copia. Esegue il listener.
     */
    private void eseguiListenerCopiaNonCompletata(){
        final CopyHandlerListener copyHandlerListener = handler.getCopyListener();
        if(copyHandlerListener != null){
            copyHandlerListener.onCopyServiceFinished(false, destinazione.getPath(), new ArrayList<>(), CopyService.COPY_LOCAL_TO_SMB);
        }
    }


    /**
     * Chiamato dopo che l'utente (tramite le dialogs) ha scelto l'operazione da eseguire sui files (rinomina, sovrascrivi...)
     * @param azioniFilesGiaPresenti Map che contiene l'associazione file-azione da eseguire
     */
    @Override
    public void onDialogOverwriteFinished(Map<File, Integer> azioniFilesGiaPresenti) {
        avviaCopia(azioniFilesGiaPresenti);
    }
}
