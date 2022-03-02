package it.Ettore.egalfilemanager.fileutils;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import androidx.annotation.NonNull;


/**
 *  Classe di utilità per la gestione dei files/cartelle preferiti
 *  @author Ettore Gallina
 */
public class PreferitiManager {
    private static final int MAX_ELEMENTI_DA_AGGIUNGERE = 10;
    private static final String KEY_PREFS_PREFERITI = "favorites";
    private final SharedPreferences prefs;



    /**
     *
     * @param prefs SharedPreferences in cui saranno salvati i riferimenti ai preferiti
     */
    public PreferitiManager(@NonNull SharedPreferences prefs){
        this.prefs = prefs;
    }



    /**
     * Aggiunge un file ai preferiti
     * @param file File da aggiungere ai preferiti
     */
    public void aggiungiPreferito(File file){
        if(file != null && file.exists()){
            final Set<String> preferiti = prefs.getStringSet(KEY_PREFS_PREFERITI, getPreferitiDefault());
            final Set<String> nuovoSetPreferiti = new HashSet<>(preferiti); //creo un nuovo set, perchè se nelle prefs faccio il put con lo stesso set letto, non si aggiorna
            nuovoSetPreferiti.add(file.getAbsolutePath());
            prefs.edit().putStringSet(KEY_PREFS_PREFERITI, nuovoSetPreferiti).apply();
        }
    }



    /**
     * Aggiunge una lista di files ai preferiti
     * @param listaFiles Lista di files da aggiungere ai preferiti
     */
    public void aggiungiPreferiti(List<File> listaFiles) throws TroppiElementiException {
        if(listaFiles != null && !listaFiles.isEmpty()){
            if (listaFiles.size() > MAX_ELEMENTI_DA_AGGIUNGERE) throw new TroppiElementiException();
            final Set<String> preferiti = prefs.getStringSet(KEY_PREFS_PREFERITI, getPreferitiDefault());
            final Set<String> nuovoSetPreferiti = new HashSet<>(preferiti); //creo un nuovo set, perchè se nelle prefs faccio il put con lo stesso set letto, non si aggiorna
            for(File file : listaFiles){
                nuovoSetPreferiti.add(file.getAbsolutePath());
            }
            prefs.edit().putStringSet(KEY_PREFS_PREFERITI, nuovoSetPreferiti).apply();
        }
    }



    /**
     * Cancella un file dai preferiti
     * @param file File da rimuovere dai preferiti
     */
    public void cancellaPreferito(File file){
        final Set<String> preferiti = prefs.getStringSet(KEY_PREFS_PREFERITI, getPreferitiDefault());
        final Set<String> nuovoSetPreferiti = new HashSet<>(preferiti); //creo un nuovo set, perchè se nelle prefs faccio il put con lo stesso set letto, non si aggiorna
        if(file != null && preferiti.contains(file.getAbsolutePath())){
            nuovoSetPreferiti.remove(file.getAbsolutePath());
            prefs.edit().putStringSet(KEY_PREFS_PREFERITI, nuovoSetPreferiti).apply();
        }
    }



    /**
     * Restituisce un set ordinato per nome con tutti i preferiti salvati
     * @return Set con tutti i preferiti
     */
    public Set<File> getPreferiti(){
        final Set<String> pathsPreferiti = prefs.getStringSet(KEY_PREFS_PREFERITI, getPreferitiDefault());
        final Set<File> preferiti = new TreeSet<>(comparatorNomiFilesCrescenti);
        for(String path : pathsPreferiti){
            preferiti.add(new File(path));
        }
        return preferiti;
    }


    /**
     * All'avvio dell'app alcuni preferiti saranno già impostati di default.
     * Utilizzare quando si leggono i preferiti dalle preferences e i dati sono ancora null
     * @return Set con i preferiti di default
     */
    private Set<String> getPreferitiDefault(){
        final Set<String> setPreferitiDefault = new HashSet<>();
        setPreferitiDefault.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
        setPreferitiDefault.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath());
        return setPreferitiDefault;
    }


    /**
     *  Comparator per ordinare i preferiti per nome. I file che hanno lo stesso nome saranno ordinati per percorso.
     */
    private final Comparator<File> comparatorNomiFilesCrescenti = new Comparator<File>() {
        public int compare(File file1, File file2) {
            final String fileName1 = file1.getName().toLowerCase();
            final String fileName2 = file2.getName().toLowerCase();
            //ascending order
            int result = fileName1.compareTo(fileName2);
            if(result == 0){
                //se i nomi sono sono uguali ordino i files per path
                return file1.getAbsolutePath().compareTo(file2.getAbsolutePath());
            } else {
                return result;
            }
        }
    };


}
