package it.Ettore.egalfilemanager.filemanager;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.egalfilemanager.fileutils.OrdinatoreFilesBase;

import static it.Ettore.egalfilemanager.fileutils.OrdinatoreFilesBase.TipoOrdinamento.CRESCENTE;


/**
 * Classe che gestisce l'ordinamento dei files
 */
public class OrdinatoreFiles extends OrdinatoreFilesBase {


    /**
     *
     * @param context Context chiamante da cui ricavare le preferences
     */
    public OrdinatoreFiles(@NonNull Context context) {
        super(context);
    }


    /**
     *
     * @param prefs Preferences in cui salvare o caricare le impostazioni
     */
    public OrdinatoreFiles(@NonNull SharedPreferences prefs) {
        super(prefs);
    }


    /**
     * Costruttore solo per uso interno
     */
    private OrdinatoreFiles(){}


    /**
     * Ordina la lista di files
     * @param listaFiles Lista da ordinare
     * @return Lista ordinata
     */
    public List<File> ordinaListaFiles(List<File> listaFiles){
        if(listaFiles == null) return null;
        switch (getOrdinaPer()){
            case NOME:
                if(getTipoOrdinamento() == CRESCENTE){
                    return ordinaListaFiles(listaFiles, (file1, file2) -> comparaPerNome(file1, file2));
                } else {
                    return ordinaListaFiles(listaFiles, (file1, file2) -> -comparaPerNome(file1, file2));
                }
            case DIMENSIONE:
                if(getTipoOrdinamento() == CRESCENTE){
                    return ordinaListaFiles(listaFiles, (file1, file2) -> comparaPerDimensione(file1, file2));
                } else {
                    return ordinaListaFiles(listaFiles, (file1, file2) -> -comparaPerDimensione(file1, file2));
                }
            case DATA:
                if(getTipoOrdinamento() == CRESCENTE){
                    return ordinaListaFiles(listaFiles, (file1, file2) -> comparaPerData(file1, file2));
                } else {
                    return ordinaListaFiles(listaFiles, (file1, file2) -> -comparaPerData(file1, file2));
                }
            case TIPO:
                if(getTipoOrdinamento() == CRESCENTE){
                    return ordinaListaFiles(listaFiles, (file1, file2) -> comparaPerTipo(file1, file2));
                } else {
                    return ordinaListaFiles(listaFiles, (file1, file2) -> -comparaPerTipo(file1, file2));
                }
            default:
                throw new IllegalArgumentException("Valore dell'enum OrdinaPer non gestito");
        }
    }


    /**
     * Ordina la lista di files
     * @param listaFiles Lista da ordinare
     * @param comparator Oggetto comparator utilizzato per l'ordinamento
     * @return Lista ordinata
     */
    private List<File> ordinaListaFiles(List<File> listaFiles, Comparator<File> comparator){
        final List<File> cartelle = new ArrayList<>();
        final List<File> files = new ArrayList<>();
        for(File file : listaFiles){
            if(file.isDirectory()){
                if(!file.isHidden()){
                    cartelle.add(file);
                } else if (mostraNascosti()){
                    cartelle.add(file);
                }
            } else {
                if(!file.isHidden()){
                    files.add(file);
                } else if (mostraNascosti()){
                    files.add(file);
                }
            }
        }

        removeNullValues(cartelle);
        removeNullValues(files);
        try {
            Collections.sort(cartelle, comparator);
            Collections.sort(files, comparator);
        } catch (IllegalArgumentException e){
            //in rari casi l'ordinamento può dare un errore "Comparison method violates its general contract!"
            e.printStackTrace();
        }
        //aggiungo anche i file e ritorno la lista completa
        cartelle.addAll(files);
        return cartelle;
    }


    /**
     * Rimuovo i valori null se presenti nella lista (non dovrebbero essercene a priori)
     * @param files Lista di files da analizzare
     */
    private void removeNullValues(@NonNull List<File> files){
        files.removeAll(Collections.singleton(null));
    }





    /* COMPARATORS */


    /**
     * Compara i files per nome in ordine crescente. Se le dimensioni sono uguali ordino i files per percorso
     */
    private int comparaPerNome(File file1, File file2){
        final String fileName1 = file1.getName();
        final String fileName2 = file2.getName();
        //ascending order
        int result = fileName1.compareToIgnoreCase(fileName2);
        if(result == 0){
            //se le dimensioni sono uguali ordino i files per percorso
            return file1.getAbsolutePath().compareTo(file2.getAbsolutePath());
        } else {
            return result;
        }
    }


    /**
     * Compara i files per dimensione in ordine crescente. Se le dimensioni sono uguali (solitamente cartelle) compara per nome.
     */
    private int comparaPerDimensione(File file1, File file2){
        final long fileSize1 = file1.length();
        final long fileSize2 = file2.length();
        //ascending order
        int result = Long.compare(fileSize1, fileSize2);
        if(result == 0){
            //se le dimensioni sono uguali (solitamente cartelle) ordino i files per nome
            return comparaPerNome(file1, file2);
        } else {
            return result;
        }
    }


    /**
     * Compara i files per data in ordine crescente. Se le date sono uguali compara per nome.
     */
    private int comparaPerData(File file1, File file2) {
        final long data1 = file1.lastModified();
        final long data2 = file2.lastModified();
        //ascending order
        int result = Long.compare(data1, data2);
        if(result == 0){
            //se le date sono uguali ordino i files per nome
            return comparaPerNome(file1, file2);
        } else {
            return result;
        }
    }


    /**
     * Compara i files per tipo (estensione) in ordine crescente. Se le estensioni sono uguali compara per nome.
     */
    private int comparaPerTipo(File file1, File file2){
        final String ext1 = FileUtils.getFileExtension(file1);
        final String ext2 = FileUtils.getFileExtension(file2);
        //ascending order
        int result = ext1.compareToIgnoreCase(ext2);
        if(result == 0){
            //se le estenzioni sono uguali ordino i files per nome
            return comparaPerNome(file1, file2);
        } else {
            return result;
        }
    }


    /**
     * Ordina i files per nome (include anche i files nascosti se passati nella lista)
     * @param daOrdinare Lista files da ordinare
     * @return Lista ordinata
     */
    public static List<File> ordinaPerNome(List<File> daOrdinare){
        final OrdinatoreFiles ordinatore = new OrdinatoreFiles();
        ordinatore.setMostraNascosti(true);
        return ordinatore.ordinaListaFiles(daOrdinare);
    }
}
