package it.Ettore.egalfilemanager.tools.analisispazio;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.app.Activity;

import androidx.annotation.NonNull;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import it.Ettore.egalfilemanager.filemanager.FileManager;



/**
 * Thread che avvia l'analisi di una cartella. Il thread avvierà altri thread che analizzeranno le sottocartelle contemporaneamente per accelerare l'analisi.
 */
public class AnalisiSpazioThread implements Runnable {
    private final WeakReference<Activity> activity;
    private final FileManager fileManager;
    private final File cartella;
    private final AnalisiSpazioListener listener;
    private final AtomicBoolean annulla;


    /**
     *
     * @param activity Activity
     * @param cartella Cartella da analizzare
     * @param listener Listener da eseguire al termine dell'analisi
     */
    public AnalisiSpazioThread(@NonNull Activity activity, @NonNull File cartella, @NonNull AnalisiSpazioListener listener){
        this.activity = new WeakReference<>(activity);
        this.fileManager = new FileManager(activity);
        this.cartella = cartella;
        this.listener = listener;
        this.annulla = new AtomicBoolean(false);
    }


    /**
     * Avvia l'analisi
     */
    public void start(){
        if(cartella != null && cartella.isDirectory()) {
            new Thread(this).start();
        }
    }


    /**
     * Interrompe l'analisi in corso
     */
    public void interrompi(){
        annulla.set(true);
    }


    /**
     * Il thread avvierà altri thread che analizzeranno le sottocartelle contemporaneamente per accelerare l'analisi.
     */
    @Override
    public void run() {

        long totSpazioOccupato = cartella.getTotalSpace() - cartella.getUsableSpace();
        final List<File> listaFiles = fileManager.ls(cartella);

        //creo una lista di thread da eseguire (uno per ogni sottocartella)
        final List<AnalisiSottocartellaThread> listaThreads = new ArrayList<>(listaFiles.size());
        for(File file : listaFiles){
            listaThreads.add(new AnalisiSottocartellaThread(totSpazioOccupato, fileManager, file, annulla));
        }

        //final ExecutorService executorService = Executors.newCachedThreadPool();
        final ExecutorService executorService = Executors.newFixedThreadPool(8);
        try {
            //eseguo i thread contemporaneamente (facendoli gestire dal pool), ottedendo una lista di risultati
            final List<Future<AnalisiCartella>> listaRisultati = executorService.invokeAll(listaThreads);

            //se annullo interrompo il pool
            if(annulla.get()){
                executorService.shutdownNow();
                return;
            }

            //analizzo la lista di risultati
            final List<AnalisiCartella> listaRisultatiAnalisi = new ArrayList<>(listaRisultati.size());
            for(Future<AnalisiCartella> future : listaRisultati){
                listaRisultatiAnalisi.add(future.get());
            }

            //al termine dell'analisi, comunico che non sarà più possibile aggiungere thread al pool
            executorService.shutdown();

            //ordino i risultati
            listaRisultatiAnalisi.removeAll(Collections.singleton(null)); //rimuovo tutti i valori null se presenti
            Collections.sort(listaRisultatiAnalisi);
            Collections.reverse(listaRisultatiAnalisi);

            //eseguo il listener nel thread principale
            if(activity.get() != null && !activity.get().isFinishing() && listener != null){
                activity.get().runOnUiThread(() -> listener.onAnalysisFinished(listaRisultatiAnalisi));
            }
        } catch (InterruptedException | ExecutionException ignored) {}
    }



    /**
     * Listener per ottenere i risultati dell'analisi
     */
    public interface AnalisiSpazioListener {

        /**
         * Chiamato al termine dell'analisi
         * @param listaRisultati Lista con i risultati dell'analisi
         */
        void onAnalysisFinished(List<AnalisiCartella> listaRisultati);
    }
}
