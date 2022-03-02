package it.Ettore.egalfilemanager.copyutils;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import java.util.List;


/**
 * Classe wrapper che contiene i risultati dell'analisi pre-copia
 * @param <T> Tipo di files che si desidera gestire (File, SmbFile...)
 */
public class AnalisiResult<T> {
    public final long totalSize;
    public final int totalFiles;
    public final List<T> filesGiaEsistenti;

    public AnalisiResult(long totalSize, int totalFiles, List<T> filesGiaEsistenti){
        this.totalSize = totalSize;
        this.totalFiles = totalFiles;
        this.filesGiaEsistenti = filesGiaEsistenti;
    }

}
