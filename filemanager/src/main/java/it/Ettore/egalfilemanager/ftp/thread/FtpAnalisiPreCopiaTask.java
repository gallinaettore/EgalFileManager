package it.Ettore.egalfilemanager.ftp.thread;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

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
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.ftp.FtpElement;
import it.Ettore.egalfilemanager.ftp.FtpFileUtils;
import it.Ettore.egalfilemanager.ftp.FtpSession;
import it.Ettore.egalfilemanager.ftp.OrdinatoreFilesFtp;


/**
 * Task per l'analisi dei files prima della copia di files FTP all'interno di un altro percorso FTP
 */
public class FtpAnalisiPreCopiaTask extends BaseAnalisiTask implements SovrascritturaFiles.SovrascritturaListener<FtpElement> {
    private final WeakReference<Activity> activity;
    private List<FtpElement> listaFiles;
    private final String pathDestinazione;
    private boolean destinazioneInterna;
    private AnalisiResult<FtpElement> analisiResult;
    private final CopyHandler handler;
    private final FtpSession ftpSessionOrigine;
    private final FtpSession ftpSessionDestinazione;


    /**
     *
     * @param activity Activity chiamante
     * @param listafiles Lista files da copiare
     * @param ftpSessionOrigine Sessione FTP di origine
     * @param ftpSessionDestinazione Sessione FTP di destinazione
     * @param destinazione Path della cartella di destinazione
     * @param handler Handler di copia per mostrare i dati ricevuti dal service
     */
    public FtpAnalisiPreCopiaTask(@NonNull Activity activity, List<FtpElement> listafiles, @NonNull FtpSession ftpSessionOrigine, String destinazione,
                                  @NonNull FtpSession ftpSessionDestinazione, CopyHandler handler){
        super(activity);
        this.activity = new WeakReference<>(activity);
        this.listaFiles = listafiles;
        this.pathDestinazione = destinazione;
        this.ftpSessionOrigine = ftpSessionOrigine;
        this.ftpSessionDestinazione = ftpSessionDestinazione;
        this.handler = handler;
    }


    /**
     * Analisi in background
     * @param voids Nessun parametro
     * @return False solo se ci sono errori di tipo nullpointer
     */
    @Override
    protected Boolean doInBackground(Void... voids) {
        if(listaFiles == null || listaFiles.isEmpty() || pathDestinazione == null || ftpSessionOrigine == null || ftpSessionOrigine.getFtpClient() == null || ftpSessionOrigine.getServerFtp() == null
                || ftpSessionDestinazione == null || ftpSessionDestinazione.getFtpClient() == null || ftpSessionDestinazione.getServerFtp() == null) return false;

        //controllo se qualcuno dei files deve essere copiato in una cartella interna
        for(FtpElement file : listaFiles){
            if(destinationIsInternalToOrigin(pathDestinazione, file)){
                destinazioneInterna = true;
                return true;
            }
        }

        //Ordino i files per nome crescente
        listaFiles = OrdinatoreFilesFtp.ordinaPerNome(listaFiles);

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
        if(listaFiles == null || listaFiles.isEmpty() || pathDestinazione == null || !success) return;

        if(activity.get() != null && !activity.get().isFinishing()){
            if(destinazioneInterna){
                CustomDialogBuilder.make(activity.get(), R.string.cartella_interna, CustomDialogBuilder.TYPE_ERROR).show();
            } else if(analisiResult != null){
                //procedi
                final List<String> nomiFiles = new ArrayList<>(analisiResult.filesGiaEsistenti.size());
                for(FtpElement f : analisiResult.filesGiaEsistenti){
                    nomiFiles.add(f.getName());
                }
                final SovrascritturaFiles<FtpElement> sovrascritturaFiles = new SovrascritturaFiles<>(activity.get(), analisiResult.filesGiaEsistenti, nomiFiles, FtpAnalisiPreCopiaTask.this);
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
                return; //il listener sarà richiamato dalla copia vera e propria
            } else if (analisiResult == null){
                CustomDialogBuilder.make(activity.get(), R.string.impossibile_completare_operazione, CustomDialogBuilder.TYPE_ERROR).show();
            }

            eseguiListenerCopiaNonCompletata();
        }
    }


    /**
     * Verifica se la cartella di destinazione si trova all'interno di quella di origine
     * @param destination Path della cartella di destinazione
     * @param origin Cartella di origine
     * @return True se la destinazione è all'interno
     */
    private boolean destinationIsInternalToOrigin(String destination, FtpElement origin){
        if(destination == null || origin == null) return false;
        String currentFilePath = destination;
        boolean internal = false;
        while (currentFilePath != null){
            if(currentFilePath.equals(origin.getAbsolutePath())){
                internal = true;
                break;
            } else {
                currentFilePath = FtpFileUtils.getParentFromPath(currentFilePath);
            }
        }
        return internal;
    }


    /**
     * Analizza ricorsivamente i dati passati per ottenere la dimensione totale, il numero totale di files e la lista di files già presenti nella destinazione
     * @param listaFiles Lista files da analizzare
     * @param directoryDestinazione Path della directory in cui dovrebbero essere copiati
     * @return Dati con la dimensione totale, il numero totale di files e la lista di files già presenti nella destinazione
     */
    private AnalisiResult<FtpElement> analizza(List<FtpElement> listaFiles, String directoryDestinazione){
        if(listaFiles == null) return null;
        long totalSize = 0L;
        int totalFiles = 0;
        final List<FtpElement> filesGiaEsistenti = new ArrayList<>();
        for(FtpElement file : listaFiles){
            if(file == null) return null;
            final String nuovaDestinazione = directoryDestinazione + "/" + file.getName();
            if(!file.isDirectory()){
                totalSize += file.getSize();
                totalFiles++;
                if(FtpFileUtils.fileExists(ftpSessionDestinazione.getFtpClient(), nuovaDestinazione)){
                    filesGiaEsistenti.add(file);
                }
            } else {
                List<FtpElement> listaFilesSottodirectory = FtpFileUtils.explorePath(ftpSessionOrigine, file.getAbsolutePath());
                listaFilesSottodirectory = OrdinatoreFilesFtp.ordinaPerNome(listaFilesSottodirectory);
                final AnalisiResult<FtpElement> result = analizza(listaFilesSottodirectory, nuovaDestinazione);
                if(result == null) return null;
                totalSize += result.totalSize;
                totalFiles += result.totalFiles;
                filesGiaEsistenti.addAll(result.filesGiaEsistenti);
            }
        }
        return new AnalisiResult<>(totalSize, totalFiles, filesGiaEsistenti);
    }


    /**
     * Avvia il task di copia
     * @param azioniFilesGiaPresenti Map contenente le associazioni files-azione da eseguire
     */
    private void avviaCopia(Map<FtpElement, Integer> azioniFilesGiaPresenti){
        if(listaFiles != null && !listaFiles.isEmpty()) {
            if(!FtpCopiaService.isRunning()) {
                final Intent serviceIntent = FtpCopiaService.createStartIntent(activity.get(), listaFiles, ftpSessionOrigine.getServerFtp(), pathDestinazione, ftpSessionDestinazione.getServerFtp(),
                        analisiResult.totalSize, analisiResult.totalFiles, azioniFilesGiaPresenti, handler);
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
            copyHandlerListener.onCopyServiceFinished(false, pathDestinazione, new ArrayList<>(), CopyService.COPY_FTP_TO_FTP);
        }
    }


    /**
     * Chiamato dopo che l'utente (tramite le dialogs) ha scelto l'operazione da eseguire sui files (rinomina, sovrascrivi...)
     * @param azioniFilesGiaPresenti Map che contiene l'associazione file-azione da eseguire
     */
    @Override
    public void onDialogOverwriteFinished(Map<FtpElement, Integer> azioniFilesGiaPresenti) {
        avviaCopia(azioniFilesGiaPresenti);
    }
}
