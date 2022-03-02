package it.Ettore.egalfilemanager.filemanager.thread;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
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
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.filemanager.FileManager;
import it.Ettore.egalfilemanager.filemanager.OrdinatoreFiles;


/**
 * Classe che effettua un'analisi in un task separato prima della copia dei files
 */
public class AnalisiPreCopiaTask extends BaseAnalisiTask implements SovrascritturaFiles.SovrascritturaListener<File> {
    private final FileManager fileManager;
    private final WeakReference<Activity> activity;
    private final File destinazione;
    private final CopyHandler handler;
    private List<File> listaFiles;
    private long freeSpace = 0L;
    private boolean destinazioneInterna;
    private AnalisiResult<File> analisiResult;


    /**
     *
     * @param activity Activity chiamante
     * @param listaFiles Lista di files da analizzare
     * @param destinazione Destinazione su cui al temine dovrebbero essere copiati i files
     * @param handler Handler di copia per mostrare i dati ricevuti dal service
     */
    public AnalisiPreCopiaTask(@NonNull Activity activity, List<File> listaFiles, File destinazione, CopyHandler handler){
        super(activity);
        this.fileManager = new FileManager(activity);
        this.fileManager.ottieniStatoRootExplorer();
        this.activity = new WeakReference<>(activity);
        this.listaFiles = listaFiles;
        this.destinazione = destinazione;
        this.handler = handler;
    }



    /**
     *
     * @param params Nessun parametro in ingresso
     * @return  True se l'operazione di analisi va a buonfine. False se la lista files è null, se è vuota, se la destinazione è null, se ci sono errori durante l'analisi.
     */
    @Override
    protected Boolean doInBackground(Void... params){
        if(listaFiles == null || listaFiles.isEmpty() || destinazione == null) return false;

        //controllo se qualcuno dei files deve essere copiato in una cartella interna
        for(File file : listaFiles){
            if(destinationIsInternalToOrigin(destinazione, file)){
                destinazioneInterna = true;
                return true;
            }
        }

        //Ordino i files per nome crescente
        listaFiles = OrdinatoreFiles.ordinaPerNome(listaFiles);

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
        if(listaFiles == null || listaFiles.isEmpty() || destinazione == null || !success) return;

        if(activity.get() != null && !activity.get().isFinishing()){
            if(destinazioneInterna){
                CustomDialogBuilder.make(activity.get(), R.string.cartella_interna, CustomDialogBuilder.TYPE_ERROR).show();
            } else if(analisiResult != null){
                if(analisiResult.totalSize >= freeSpace){
                    CustomDialogBuilder.make(activity.get(), R.string.spazio_insufficiente, CustomDialogBuilder.TYPE_ERROR).show();
                } else {
                    //procedi
                    final List<String> nomiFiles = new ArrayList<>(analisiResult.filesGiaEsistenti.size());
                    for(File f : analisiResult.filesGiaEsistenti){
                        nomiFiles.add(f.getName());
                    }
                    final SovrascritturaFiles<File> sovrascritturaFiles = new SovrascritturaFiles<>(activity.get(), analisiResult.filesGiaEsistenti, nomiFiles, AnalisiPreCopiaTask.this);
                    if(!listaFiles.isEmpty() && listaFiles.get(0).getParentFile().equals(destinazione)){
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
            } else { //analisi result == null
                CustomDialogBuilder.make(activity.get(), R.string.impossibile_completare_operazione, CustomDialogBuilder.TYPE_ERROR).show();
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
    private AnalisiResult<File> analizza(List<File> listaFiles, File directoryDestinazione){
        if(listaFiles == null) return null;
        long totalSize = 0L;
        int totalFiles = 0;
        final List<File> filesGiaEsistenti = new ArrayList<>();
        for(File file : listaFiles){
            if(file == null) return null;
            final File nuovaDestinazione = new File(directoryDestinazione, file.getName());
            if(!file.isDirectory()){
                totalSize += file.length();
                totalFiles++;
                if(fileManager.fileExists(nuovaDestinazione)){
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
            if(!CopiaLocaleService.isRunning()) {
                final Intent serviceIntent = CopiaLocaleService.createStartIntent(activity.get(), listaFiles, destinazione, analisiResult.totalSize, analisiResult.totalFiles, azioniFilesGiaPresenti,
                        handler, isCancellaOrigine());
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
            copyHandlerListener.onCopyServiceFinished(false, destinazione.getPath(), new ArrayList<>(), CopyService.COPY_LOCAL_TO_LOCAL);
        }
    }


    /**
     * Verifica se la cartella di destinazione si trova all'interno di quella di origine
     * @param destination Cartella di destinazione
     * @param origin Cartella di origine
     * @return True se la destinazione è all'interno
     */
    private boolean destinationIsInternalToOrigin(File destination, File origin){
        if(destination == null || origin == null) return false;
        File currentFile = destination;
        while (currentFile != null){
            if(currentFile.equals(origin)){
                return true;
            }
            currentFile = currentFile.getParentFile();
        }
        return false;
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
