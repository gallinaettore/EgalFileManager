package it.Ettore.egalfilemanager.ftp.thread;
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
import it.Ettore.egalfilemanager.filemanager.FileManager;
import it.Ettore.egalfilemanager.filemanager.OrdinatoreFiles;
import it.Ettore.egalfilemanager.ftp.FtpFileUtils;
import it.Ettore.egalfilemanager.ftp.FtpSession;


/**
 * Task per l'analisi dei files prima della copia di files del dispositivon all'interno di un percorso FTP
 */
public class FtpAnalisiPreUploadTask extends BaseAnalisiTask implements SovrascritturaFiles.SovrascritturaListener<File> {
    private final WeakReference<Activity> activity;
    private List<File> listaFiles;
    private final String pathDestinazione;
    private AnalisiResult<File> analisiResult;
    private final FileManager fileManager;
    private final CopyHandler handler;
    private final FtpSession ftpSession;


    /**
     *
     * @param activity Activity chiamante
     * @param listafiles Lista files da copiare
     * @param destinazione Path della cartella di destinazione
     * @param ftpSession Sessione FTP
     * @param handler Handler di copia per mostrare i dati ricevuti dal service
     * @param cancellaOrigine True se in modalità taglia. False se in modalità copia
     */
    public FtpAnalisiPreUploadTask(@NonNull Activity activity, List<File> listafiles, String destinazione, FtpSession ftpSession, CopyHandler handler, boolean cancellaOrigine){
        super(activity);
        this.fileManager = new FileManager(activity);
        this.fileManager.ottieniStatoRootExplorer();
        this.activity = new WeakReference<>(activity);
        this.listaFiles = listafiles;
        this.ftpSession = ftpSession;
        this.pathDestinazione = destinazione;
        this.handler = handler;
        setCancellaOrigine(cancellaOrigine);
    }


    /**
     * Analisi in background
     * @param voids Nessun parametro
     * @return False solo se ci sono errori di tipo nullpointer
     */
    @Override
    protected Boolean doInBackground(Void... voids) {
        if(listaFiles == null || listaFiles.isEmpty() || pathDestinazione == null || ftpSession == null || ftpSession.getFtpClient() == null || ftpSession.getServerFtp() == null) return false;

        //Ordino i files per nome crescente
        listaFiles = OrdinatoreFiles.ordinaPerNome(listaFiles);

        //Analizzo i files da copiare, non calcolo lo spazio disponibile su FTP perchè non è possibile
        analisiResult = analizza(listaFiles, pathDestinazione);

        return true;
    }


    /**
     * Se l'analisi ha avuto un esito positivo avvia la copia altrimenti vengono mostrate le dialogs di errore
     * @param success Successo dell'analisi
     */
    @Override
    protected void onPostExecute(Boolean success){
        dismissWaitDialog();
        if(listaFiles == null || listaFiles.isEmpty() || pathDestinazione == null || !success || analisiResult == null) return;

        if(activity.get() != null && !activity.get().isFinishing()){
            final List<String> nomiFiles = new ArrayList<>(analisiResult.filesGiaEsistenti.size());
            for(File f : analisiResult.filesGiaEsistenti){
                nomiFiles.add(f.getName().replace("/", ""));
            }
            final SovrascritturaFiles<File> sovrascritturaFiles = new SovrascritturaFiles<>(activity.get(), analisiResult.filesGiaEsistenti, nomiFiles, FtpAnalisiPreUploadTask.this);
            if(!listaFiles.isEmpty() && listaFiles.get(0).getParent().equals(pathDestinazione)){
                //Se la directory di destinazione è la stessa di quella della copia, duplico i files rinominandoli
                sovrascritturaFiles.applicaATuttiRestanti(SovrascritturaFiles.AZIONE_RINOMINA);
            } else if(analisiResult.filesGiaEsistenti.isEmpty()){
                //destinazione diversa senza file da copiare già presenti
                avviaCopia(new HashMap<>());
            } else {
                //destinazione diversa che contiene però già alcuni files
                sovrascritturaFiles.mostraDialogSovrascrittura();
            }
            //il listener sarà richiamato dalla copia vera e propria
        }
    }


    /**
     * Analizza ricorsivamente i dati passati per ottenere la dimensione totale, il numero totale di files e la lista di files già presenti nella destinazione
     * @param listaFiles Lista files da analizzare
     * @param directoryDestinazione Directory in cui dovrebbero essere copiati
     * @return Dati con la dimensione totale, il numero totale di files e la lista di files già presenti nella destinazione
     */
    private AnalisiResult<File> analizza(List<File> listaFiles, String directoryDestinazione){
        if(listaFiles == null) return null;
        long totalSize = 0L;
        int totalFiles = 0;
        final List<File> filesGiaEsistenti = new ArrayList<>();
        for(File file : listaFiles){
            if(file == null) return null;
            final String nuovaDestinazione = directoryDestinazione + "/" + file.getName();
            if(!file.isDirectory()){
                totalSize += file.length();
                totalFiles++;
                if(FtpFileUtils.fileExists(ftpSession.getFtpClient(), nuovaDestinazione)){
                    filesGiaEsistenti.add(file);
                }
            } else {
                List<File> listaFilesSottodirectory = fileManager.ls(file); //utilizzando il file manager posso usare anche i permessi di root
                if(listaFilesSottodirectory != null) {
                    listaFilesSottodirectory = OrdinatoreFiles.ordinaPerNome(listaFilesSottodirectory);
                    final AnalisiResult<File> result = analizza(listaFilesSottodirectory, nuovaDestinazione);
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
            if(!FtpUploadService.isRunning()) {
                final Intent serviceIntent = FtpUploadService.createStartIntent(activity.get(), listaFiles, ftpSession.getServerFtp(), pathDestinazione, analisiResult.totalSize,
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
            copyHandlerListener.onCopyServiceFinished(false, pathDestinazione, new ArrayList<>(), CopyService.COPY_LOCAL_TO_FTP);
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
