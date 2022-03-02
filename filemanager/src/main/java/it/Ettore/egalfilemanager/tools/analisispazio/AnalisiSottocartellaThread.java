package it.Ettore.egalfilemanager.tools.analisispazio;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/



import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import it.Ettore.egalfilemanager.fileutils.FileTypes;
import it.Ettore.egalfilemanager.filemanager.FileManager;



/**
 * Thread per l'analisi di una cartella. Chiamato dal pool di thread gestito dalla classe AnalisiSpazioThread
 */
class AnalisiSottocartellaThread implements Callable<AnalisiCartella> {
    private final long totSpazioOccupato;
    private final FileManager fileManager;
    private final File file;
    private final AtomicBoolean annulla;


    /**
     *
     * @param totSpazioOccupato Spazio totale occupato dai files nella cartella genitore
     * @param fileManager FileManager
     * @param file File o sottocartella da analizzare
     * @param annulla Booleano per gestire l'interruzione del thread
     */
    AnalisiSottocartellaThread(long totSpazioOccupato, FileManager fileManager, File file, AtomicBoolean annulla) {
        this.totSpazioOccupato = totSpazioOccupato;
        this.fileManager = fileManager;
        this.file = file;
        this.annulla = annulla;
    }


    /**
     * Esegue l'analisi in background e restituisce i risultati
     * @return Risultati dell'analisi
     */
    @Override
    public AnalisiCartella call() {
        return analizza(file);
    }


    /**
     * Analisi ricorsiva
     * @param file File o cartella da analizzare
     * @return Risultati dell'analisi
     */
    private AnalisiCartella analizza(File file){
        long totBytes = 0L, totBytesImmagini = 0L, totBytesVideo = 0L, totBytesAudio = 0L;
        int totFiles = 0, totCartelle = 0, totImmagini = 0, totVideo = 0, totAudio = 0;
        if(!file.isDirectory()){
            totBytes = file.length();
            totFiles = 1;
            int fileType = FileTypes.getTypeForFile(file);
            switch (fileType){
                case FileTypes.TYPE_IMMAGINE:
                    totImmagini = 1;
                    totBytesImmagini = totBytes;
                    break;
                case FileTypes.TYPE_VIDEO:
                    totVideo = 1;
                    totBytesVideo = totBytes;
                    break;
                case FileTypes.TYPE_AUDIO:
                    totAudio = 1;
                    totBytesAudio = totBytes;
                    break;
            }
        } else {
            if (annulla.get()) {
                return null;
            }
            final List<File> listaFile = fileManager.ls(file);
            if(listaFile != null) {
                for (File f : listaFile) {
                    final AnalisiCartella analisiResult = analizza(f);
                    if(analisiResult == null) return null;
                    if(f.isDirectory()){
                        totCartelle++;
                    }
                    totBytes += analisiResult.totBytes;
                    totFiles += analisiResult.totFiles;
                    totCartelle += analisiResult.totCartelle;
                    totImmagini += analisiResult.totImmagini;
                    totVideo += analisiResult.totVideo;
                    totAudio += analisiResult.totAudio;
                    totBytesImmagini += analisiResult.totBytesImmagini;
                    totBytesVideo += analisiResult.totBytesVideo;
                    totBytesAudio += analisiResult.totBytesAudio;
                }
            }
        }
        final AnalisiCartella analisiCartella = new AnalisiCartella();
        analisiCartella.file = file;
        analisiCartella.totBytes = totBytes;
        analisiCartella.totFiles = totFiles;
        analisiCartella.totCartelle = totCartelle;
        analisiCartella.percentualeOccupamento = (double)totBytes * 100 / totSpazioOccupato;
        analisiCartella.totImmagini = totImmagini;
        analisiCartella.totVideo = totVideo;
        analisiCartella.totAudio = totAudio;
        analisiCartella.totBytesImmagini = totBytesImmagini;
        analisiCartella.totBytesVideo = totBytesVideo;
        analisiCartella.totBytesAudio = totBytesAudio;
        return analisiCartella;
    }

}
