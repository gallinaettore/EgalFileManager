package it.Ettore.egalfilemanager.tools.analisispazio;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import androidx.annotation.NonNull;

import java.io.File;
import java.util.List;


/**
 * Classe wrapper che racchiude i risultati di analisi di una cartella o di un file
 */
public class AnalisiCartella implements Comparable<AnalisiCartella> {
    public File file;
    public int totFiles, totCartelle, totImmagini, totVideo, totAudio;
    public long totBytes, totBytesImmagini, totBytesVideo, totBytesAudio;
    public double percentualeOccupamento;


    /**
     * Calcola il numero totale di files non immagine, video o audio
     * @return Numero di altri files
     */
    public int totAltriFiles(){
        return  totFiles - totImmagini - totVideo - totAudio;
    }


    /**
     * Calcola la dimensione totale di files non immagine, video o audio
     * @return Dimensione totale altri files
     */
    public long totBytesAltriFiles(){
        return totBytes - totBytesImmagini - totBytesVideo - totBytesAudio;
    }


    /**
     * Unisce una lista di analisi in un unico risultato
     * @param listaAnalisi Lista di analisi
     * @return AnalisiSpazioThread complessiva. Null se la lista analisi e null o se è vuota
     */
    public static AnalisiCartella mergeList(List<AnalisiCartella> listaAnalisi){
        if(listaAnalisi == null || listaAnalisi.isEmpty()) return null;
        final AnalisiCartella analisiCartella = new AnalisiCartella();
        analisiCartella.file = listaAnalisi.get(0).file.getParentFile();
        for(AnalisiCartella analisiCorrente : listaAnalisi){
            analisiCartella.totFiles += analisiCorrente.totFiles;
            analisiCartella.totImmagini += analisiCorrente.totImmagini;
            analisiCartella.totVideo += analisiCorrente.totVideo;
            analisiCartella.totAudio += analisiCorrente.totAudio;
            analisiCartella.totBytes += analisiCorrente.totBytes;
            analisiCartella.totBytesImmagini += analisiCorrente.totBytesImmagini;
            analisiCartella.totBytesVideo += analisiCorrente.totBytesVideo;
            analisiCartella.totBytesAudio += analisiCorrente.totBytesAudio;
            analisiCartella.percentualeOccupamento += analisiCorrente.percentualeOccupamento;
        }
        return analisiCartella;
    }


    /**
     * Metodo per l'ordinamento dei risultati
     * @param analisiCartella Altro oggetto da comparare
     * @return Risultato della comparazione
     */
    @Override
    public int compareTo(@NonNull AnalisiCartella analisiCartella) {
        int result = Long.compare(totBytes, analisiCartella.totBytes);
        if(result == 0){
            //se le dimensioni sono uguali ordino per nome
            if(file == null && analisiCartella.file == null) return 0;
            if(file == null) return -1;
            if(analisiCartella.file == null) return 1;
            return file.getName().compareTo(analisiCartella.file.getName());
        } else {
            return result;
        }
    }
}




