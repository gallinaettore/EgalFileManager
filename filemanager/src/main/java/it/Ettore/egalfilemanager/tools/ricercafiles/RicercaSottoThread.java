package it.Ettore.egalfilemanager.tools.ricercafiles;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.Ettore.egalfilemanager.fileutils.FileTypes;
import it.Ettore.egalfilemanager.filemanager.FileManager;

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


/**
 * Thread che ricerca in un determinato percorso i file in base ai parametri passati
 */
class RicercaSottoThread implements Callable<List<File>> {
    private final FileManager fileManager;
    private final ParametriRicerca parametriRicerca;
    private final File percorsoRicerca;
    private final Pattern pattern;
    private final AtomicBoolean interrompi;
    private final boolean ricercaTutto; //ricerca solo per nome (tutti i tipi e tutte le dimensioni)


    /**
     *
     * @param fileManager FileManager
     * @param parametriRicerca Parametri di ricerca
     * @param percorsoRicerca Percorso in cui cercare i files
     * @param interrompi True per interrompere il thread
     */
    RicercaSottoThread(FileManager fileManager, ParametriRicerca parametriRicerca, File percorsoRicerca, AtomicBoolean interrompi) {
        this.fileManager = fileManager;
        if(parametriRicerca.isCaseSensistive()){
            this.pattern = Pattern.compile(parametriRicerca.getRegex());
        } else {
            this.pattern = Pattern.compile(parametriRicerca.getRegex(), Pattern.CASE_INSENSITIVE);
        }
        this.parametriRicerca = parametriRicerca;
        this.ricercaTutto = parametriRicerca.ricercaTutto() && parametriRicerca.getTipoDimensione() == ParametriRicerca.DIMENSIONI_TUTTE;
        this.interrompi = interrompi;
        this.percorsoRicerca = percorsoRicerca;
    }


    /**
     * Ricerca i files in base ai parametri passati
     * @return Lista dei files trovati
     */
    @Override
    public List<File> call() {
        return ricerca(percorsoRicerca);
    }


    /**
     * Ricerca ricorsiva all'interno del percorso
     * @param percorsoDiRicerca Percorso in cui cercare i files
     * @return Lista dei files trovati
     */
    private List<File> ricerca(File percorsoDiRicerca){
        final List<File> filesTrovati = new ArrayList<>();
        if(test(percorsoDiRicerca)){
            filesTrovati.add(percorsoDiRicerca);
        }
        if(percorsoDiRicerca.isDirectory()){
            final List<File> listaFiles = fileManager.ls(percorsoDiRicerca);
            for(File f : listaFiles){
                if(interrompi.get()) return null;
                filesTrovati.addAll(ricerca(f));
            }
        }
        return filesTrovati;
    }


    /**
     * Verifica che il file corrisponda ai parametri di ricerca
     * @param file File da valutare
     * @return True se il file corrisponde ai parametri di ricerca
     */
    private boolean test(File file){
        final Matcher matcher = pattern.matcher(file.getName());
        boolean stringMatch = matcher.matches();
        if(stringMatch && ricercaTutto){
            //se c'è una ricerca solo per nome
            return true;
        }
        //se c'è una ricerca più affinata
        if(stringMatch){
            //se la stringa corrisponde effettuo le altre verifiche
            boolean typeMatch = testTipo(file);
            if(typeMatch){
                //se anche il tipo corrisponde continuo ad effettuare le altre verifiche
                return testDimensione(file);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }


    /**
     * Verifica che il tipo di file sia tra quelli selezionati nei parametri di ricerca
     * @param file File da valutare
     * @return True se tipo di file è tra quelli selezionati
     */
    private boolean testTipo(File file){
        int type = FileTypes.getTypeForFile(file);
        if(type == FileTypes.TYPE_IMMAGINE && parametriRicerca.ricercaImmagini()){
            return true;
        } else if(type == FileTypes.TYPE_VIDEO && parametriRicerca.ricercaVideo()){
            return true;
        } else if (type == FileTypes.TYPE_AUDIO && parametriRicerca.ricercaAudio()){
            return true;
        } else if (type != FileTypes.TYPE_IMMAGINE && type != FileTypes.TYPE_VIDEO && type != FileTypes.TYPE_AUDIO && parametriRicerca.ricercaAltri()){
            return true;
        } else {
            return false;
        }
    }


    /**
     * Verifica che la dimensione del file sia conforme a quanto espresso nei parametri di ricerca
     * @param file File da valutare
     * @return True se la dimensione è adeguata
     */
    private boolean testDimensione(File file){
        if(parametriRicerca.getTipoDimensione() == ParametriRicerca.DIMENSIONI_TUTTE){
            return true;
        } else if (parametriRicerca.getTipoDimensione() == ParametriRicerca.DIMENSIONI_MINORI){
            return file.length() <= parametriRicerca.getDimensione();
        } else if (parametriRicerca.getTipoDimensione() == ParametriRicerca.DIMENSIONI_MAGGIORI){
            return file.length() >= parametriRicerca.getDimensione();
        } else {
            return false;
        }
    }
}
