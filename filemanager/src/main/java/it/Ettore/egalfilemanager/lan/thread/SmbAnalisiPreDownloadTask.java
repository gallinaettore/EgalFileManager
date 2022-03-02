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
import java.util.ArrayList;
import java.util.Arrays;
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
import it.Ettore.egalfilemanager.lan.OrdinatoreFilesLan;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;


/**
 * Task per l'analisi dei files prima della copia di files smb all'interno di un percorso locale
 */
public class SmbAnalisiPreDownloadTask extends BaseAnalisiTask implements SovrascritturaFiles.SovrascritturaListener<SmbFile> {
    private final WeakReference<Activity> activity;
    private List<SmbFile> listaFiles;
    private final File destinazione;
    private AnalisiResult<SmbFile> analisiResult;
    private long freeSpace;
    private final CopyHandler handler;
    private final String user;
    private final String password;
    private final FileManager fileManager;


    /**
     *
     * @param activity Activity chiamante
     * @param listafiles Lista files da copiare
     * @param user Username del server smb
     * @param password Password del server smb
     * @param destinazione Cartella di destinazione
     * @param handler Handler di copia per mostrare i dati ricevuti dal service
     */
    public SmbAnalisiPreDownloadTask(@NonNull Activity activity, List<SmbFile> listafiles, String user, String password, File destinazione, CopyHandler handler){
        super(activity);
        this.activity = new WeakReference<>(activity);
        this.listaFiles = listafiles;
        this.destinazione = destinazione;
        this.handler = handler;
        this.user = user;
        this.password = password;
        this.fileManager = new FileManager(activity);
        this.fileManager.ottieniStatoRootExplorer();
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
        listaFiles = OrdinatoreFilesLan.ordinaPerNome(listaFiles);

        //Calcolo la dimensione totale dei files da copiare
        analisiResult = analizza(listaFiles, destinazione);
        freeSpace = fileManager.getFreeSpace(destinazione);

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
                for(SmbFile f : analisiResult.filesGiaEsistenti){
                    nomiFiles.add(f.getName().replace("/", ""));
                }
                final SovrascritturaFiles<SmbFile> sovrascritturaFiles = new SovrascritturaFiles<>(activity.get(), analisiResult.filesGiaEsistenti, nomiFiles, SmbAnalisiPreDownloadTask.this);
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
     * @return Dati con la dimensione totale, il numero totale di files e la lista di files già presenti nella destinazione
     */
    private AnalisiResult<SmbFile> analizza(List<SmbFile> listaFiles, File directoryDestinazione){
        if(listaFiles == null) return null;
        long totalSize = 0L;
        int totalFiles = 0;
        final List<SmbFile> filesGiaEsistenti = new ArrayList<>();
        for(SmbFile file : listaFiles){
            try {
                if(file == null || !file.exists()) return null;
            } catch (SmbException e) {
                return null;
            }
            final File nuovaDestinazione = new File(directoryDestinazione, file.getName());
            try {
                if(!file.isDirectory()){
                    totalSize += file.length();
                    totalFiles++;
                    if(fileManager.fileExists(nuovaDestinazione)){
                        filesGiaEsistenti.add(file);
                    }
                } else {
                    final SmbFile[] arrayFiles = file.listFiles();
                    if(arrayFiles != null) {
                        List<SmbFile> listaFilesSottodirectory = new ArrayList<>(Arrays.asList(arrayFiles));
                        listaFilesSottodirectory = OrdinatoreFilesLan.ordinaPerNome(listaFilesSottodirectory);
                        final AnalisiResult<SmbFile> result = analizza(listaFilesSottodirectory, nuovaDestinazione);
                        if(result == null) return null;
                        totalSize += result.totalSize;
                        totalFiles += result.totalFiles;
                        filesGiaEsistenti.addAll(result.filesGiaEsistenti);
                    }
                }
            } catch (SmbException e) {
                e.printStackTrace();
                return null;
            }
        }
        return new AnalisiResult<>(totalSize, totalFiles, filesGiaEsistenti);
    }


    /**
     * Avvia il task di copia
     * @param azioniFilesGiaPresenti Map contenente le associazioni files-azione da eseguire
     */
    private void avviaCopia(Map<SmbFile, Integer> azioniFilesGiaPresenti){
        if(listaFiles != null && !listaFiles.isEmpty()) {
            if(!SmbDownloadService.isRunning()) {
                final Intent serviceIntent = SmbDownloadService.createStartIntent(activity.get(), listaFiles, user, password, destinazione, analisiResult.totalSize,
                        analisiResult.totalFiles, azioniFilesGiaPresenti, handler);
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
            copyHandlerListener.onCopyServiceFinished(false, destinazione.getAbsolutePath(), new ArrayList<>(), CopyService.COPY_SMB_TO_LOCAL);
        }
    }


    /**
     * Chiamato dopo che l'utente (tramite le dialogs) ha scelto l'operazione da eseguire sui files (rinomina, sovrascrivi...)
     * @param azioniFilesGiaPresenti Map che contiene l'associazione file-azione da eseguire
     */
    @Override
    public void onDialogOverwriteFinished(Map<SmbFile, Integer> azioniFilesGiaPresenti) {
        avviaCopia(azioniFilesGiaPresenti);
    }
}
