package it.Ettore.egalfilemanager.lan.thread;

import android.app.Activity;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.lan.SmbFileUtils;
import it.Ettore.egalfilemanager.view.ViewProprieta;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Task per la visualizzazione delle proprietà di un file smb
 */
public class SmbProprietaTask extends AsyncTask <Void, String, Void> {
    private static final int FREQ_AGGIORNAMENTO = 300;
    private static final String AZIONE_PRIMO_AGGIORNAMENTO = "azione_primo_aggiornamento";
    private static final String AZIONE_AGGIORNAMENTI_SUCCESSIVI = "azione_aggiornamenti_successivi";
    private final WeakReference<Activity> activity;
    private final List<SmbFile> listaFiles;
    private AlertDialog dialog;
    private final SmbFile primoFile;
    private final AtomicBoolean annulla;
    private long totBytes, ultimoAggiornamento, sizePrimoFile, dataPrimoFile;
    private boolean primoFileNascosto, permessiLetturaPrimoFile, permessiScritturaPrimoFile;
    private int totFiles, totCartelle;
    private WeakReference<ViewProprieta> viewProprieta;



    /**
     *
     * @param activity Activity chiamante
     * @param listaFiles Lista files da analizzare
     */
    public SmbProprietaTask(@NonNull Activity activity, @NonNull List<SmbFile> listaFiles){
        this.activity = new WeakReference<>(activity);
        this.listaFiles = listaFiles;
        this.primoFile = listaFiles.get(0);
        this.annulla = new AtomicBoolean(false);
    }


    /**
     * Crea la dialog e mostra le informazioni subito disponibili
     */
    @Override
    protected void onPreExecute(){
        final CustomDialogBuilder builder = new CustomDialogBuilder(activity.get());
        builder.setTitle(R.string.proprieta);
        builder.hideIcon(true);

        viewProprieta = new WeakReference<>(new ViewProprieta(activity.get(), listaFiles.size() == 1));
        viewProprieta.get().setFile(primoFile.getName(), SmbFileUtils.isDirectory(primoFile), false);

        //analizza tutti i percorsi e imposta il percorso a null se sono differenti
        String percorsoComune = primoFile.getParent();
        for(int i=1; i < listaFiles.size(); i++){
            if(!listaFiles.get(i).getParent().equals(percorsoComune)){
                percorsoComune = null;
                break;
            }
        }
        viewProprieta.get().setPercorso(percorsoComune);

        builder.setView(viewProprieta.get());
        builder.setNeutralButton(android.R.string.ok, null);
        builder.setOnDismissListener(dialogInterface -> annulla.set(true));

        dialog = builder.create();
        dialog.show();
    }


    /**
     * Analizza in background e mostra le informazioni via via che vengono trovate
     * @param params Nessun parametro
     * @return Void
     */
    @Override
    protected Void doInBackground(Void... params) {

        //analizzo il primo file
        try {
            sizePrimoFile = primoFile.length();
            dataPrimoFile = primoFile.lastModified();
            primoFileNascosto = primoFile.isHidden();
            permessiLetturaPrimoFile = primoFile.canRead();
            permessiScritturaPrimoFile = primoFile.canWrite();
        } catch (SmbException e) {
            e.printStackTrace();
        }
        publishProgress(AZIONE_PRIMO_AGGIORNAMENTO);

        //analizzo file e directory
        for(SmbFile file : listaFiles){
            analizzaFileRicorsivo(file);
        }

        //le directory principali non vengono conteggiate (quindi le rimuovo dal totale)
        for(SmbFile file : listaFiles){
            if(SmbFileUtils.isDirectory(file)){
                totCartelle--;
            }
        }
        publishProgress(AZIONE_AGGIORNAMENTI_SUCCESSIVI);
        return null;
    }


    /**
     * Aggiorna la dialog
     * @param actions Azione da eseguire nell'aggiornamento.
     */
    @Override
    protected void onProgressUpdate(String... actions) {
        if (activity.get() != null && !activity.get().isFinishing() && dialog != null && dialog.isShowing() && viewProprieta.get() != null) {
            final String azione = actions[0];
            switch (azione){
                case AZIONE_PRIMO_AGGIORNAMENTO:
                    //azione primo file
                    viewProprieta.get().setDimensione(sizePrimoFile);
                    viewProprieta.get().setData(dataPrimoFile);
                    viewProprieta.get().setPermessiNormali(permessiLetturaPrimoFile, permessiScritturaPrimoFile);
                    viewProprieta.get().setNascosto(primoFileNascosto, false);
                    break;
                case AZIONE_AGGIORNAMENTI_SUCCESSIVI:
                    //aggiornamenti seguenti
                    viewProprieta.get().setDimensione(totBytes);
                    viewProprieta.get().setContenuto(totFiles, totCartelle);
                    break;
            }
        }
    }



    /**
     * Analizza i file ricorsivamente per calcolare anche la dimensione delle cartelle e il loro contenuto
     * @param file File o cartella da analizzare
     */
    private void analizzaFileRicorsivo(SmbFile file){
        if(annulla.get()) return;
        try {
            if (!file.isDirectory()) {
                totBytes += file.length();
                totFiles++;
                final long now = System.currentTimeMillis();
                if (now - ultimoAggiornamento > FREQ_AGGIORNAMENTO) {
                    publishProgress(AZIONE_AGGIORNAMENTI_SUCCESSIVI);
                    ultimoAggiornamento = now;
                }
            } else {
                totCartelle++;
                final SmbFile[] listaFile = file.listFiles();
                if (listaFile != null) {
                    for (SmbFile f : listaFile) {
                        analizzaFileRicorsivo(f);
                    }
                }
            }
        } catch (SmbException e){
            e.printStackTrace();
        }
    }
}
