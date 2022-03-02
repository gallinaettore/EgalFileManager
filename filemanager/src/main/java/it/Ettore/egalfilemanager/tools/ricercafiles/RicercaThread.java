package it.Ettore.egalfilemanager.tools.ricercafiles;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.app.Activity;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import it.Ettore.egalfilemanager.filemanager.FileManager;


/**
 * Thread generale per l'avvio della ricerca. Avvia più thread che cercano contemporaneamente in percorsi diversi.
 */
public class RicercaThread implements Runnable {
    private final WeakReference<Activity> activity;
    private final ParametriRicerca parametriRicerca;
    private final FileManager fileManager;
    private final RicercaFilesListener listener;
    private final AtomicBoolean interrompi;


    /**
     *
     * @param activity Activity
     * @param parametriRicerca Parametri di ricerca
     * @param listener Listener chiamato al termine della ricerca
     */
    public RicercaThread(Activity activity, ParametriRicerca parametriRicerca, RicercaFilesListener listener) {
        this.activity = new WeakReference<>(activity);
        this.fileManager = new FileManager(activity);
        this.fileManager.ottieniStatoRootExplorer();
        this.parametriRicerca = parametriRicerca;
        this.listener = listener;
        this.interrompi = new AtomicBoolean(false);
    }


    /**
     * Avvia la ricerca
     */
    public void start(){
        new Thread(this).start();
    }


    /**
     * Interrompe la ricerca
     */
    public void interrompi(){
        interrompi.set(true);
    }


    /**
     * Avvia più thread che cercano contemporaneamente in percorsi diversi.
     */
    @Override
    public void run() {
        //long inizio = System.currentTimeMillis();

        //Lista di tutte le sottocartelle degli storages per avviare la ricerca con più threads
        final List<File> listaPercorsiRicerca = new ArrayList<>();
        for(File storage : parametriRicerca.getPercorsiDiRicerca()){
            final List<File> sottocartelleStorage = fileManager.ls(storage);
            listaPercorsiRicerca.addAll(sottocartelleStorage);
        }

        //creo una lista di thread da eseguire (uno per ogni sottocartella)
        final List<RicercaSottoThread> listaThreads = new ArrayList<>(listaPercorsiRicerca.size());
        for(File file : listaPercorsiRicerca){
            listaThreads.add(new RicercaSottoThread(fileManager, parametriRicerca, file, interrompi));
        }

        final ExecutorService executorService = Executors.newFixedThreadPool(8);
        try {
            //eseguo i thread contemporaneamente (facendoli gestire dal pool), ottedendo una lista di risultati
            final List<Future<List<File>>> listaRisultati = executorService.invokeAll(listaThreads);

            //se annullo interrompo il pool
            if(interrompi.get()){
                executorService.shutdownNow();
                return;
            }

            //analizzo la lista di risultati
            final List<File> filesTrovati = new ArrayList<>();
            for(Future<List<File>> future : listaRisultati){
                filesTrovati.addAll(future.get());
            }

            //al termine dell'analisi, comunico che non sarà più possibile aggiungere thread al pool
            executorService.shutdown();

            //eseguo il listener nel thread principale
            if(activity.get() != null && !activity.get().isFinishing() && listener != null){
                activity.get().runOnUiThread(() -> listener.onSearchFinished(filesTrovati));
            }
        } catch (InterruptedException | ExecutionException ignored) {}

        //Log.w("durata thread", ""+(System.currentTimeMillis()-inizio));
    }




    /**
     * Listener della ricerca
     */
    @FunctionalInterface
    public interface RicercaFilesListener {
        /**
         * Chiamato al termine della ricerca
         * @param filesTrovati Lista di files trovati
         */
        void onSearchFinished(List<File> filesTrovati);
    }
}
