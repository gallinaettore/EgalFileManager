package it.Ettore.egalfilemanager.lan;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.egalfilemanager.fileutils.OrdinatoreFilesBase;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import static it.Ettore.egalfilemanager.fileutils.OrdinatoreFilesBase.TipoOrdinamento.CRESCENTE;


/**
 * Classe che gestisce l'ordinamento dei files Smb
 */
public class OrdinatoreFilesLan extends OrdinatoreFilesBase {


    /**
     *
     * @param context Context chiamante da cui ricavare le preferences
     */
    public OrdinatoreFilesLan(@NonNull Context context) {
        super(context);
    }


    /**
     *
     * @param prefs Preferences in cui salvare o caricare le impostazioni
     */
    public OrdinatoreFilesLan(@NonNull SharedPreferences prefs) {
        super(prefs);
    }


    /**
     * Costruttore solo per uso interno
     */
    private OrdinatoreFilesLan(){}


    /**
     * Ordina la lista di files (in un thread separato)
     * @param listaFiles Lista da ordinare
     * @return Lista ordinata
     */
    public List<SmbFile> ordinaListaFiles(List<SmbFile> listaFiles){
        if(listaFiles == null) return null;

        Executor executor = Executors.newFixedThreadPool(1);
        CompletionService<List<SmbFile>> completionService = new ExecutorCompletionService<>(executor);

        completionService.submit(() -> {
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
        });

        try {
            Future<List<SmbFile>> resultFuture = completionService.take(); //blocks if none available
            return resultFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }


    public List<SmbFile> ordinaListaFilesBackup(List<SmbFile> listaFiles){
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
    private List<SmbFile> ordinaListaFiles(List<SmbFile> listaFiles, Comparator<SmbFile> comparator){
        final List<SmbFile> cartelle = new ArrayList<>();
        final List<SmbFile> files = new ArrayList<>();
        for(SmbFile file : listaFiles){
            try {
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
            } catch (SmbException e) {
                e.printStackTrace();
                return listaFiles;
            }
        }

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








    /* COMPARATORS */


    /**
     * Compara i files per nome in ordine crescente. Se le dimensioni sono uguali ordino i files per percorso
     */
    private int comparaPerNome(SmbFile file1, SmbFile file2) {
        final String fileName1 = file1.getName();
        final String fileName2 = file2.getName();
        //ascending order
        int result = fileName1.compareToIgnoreCase(fileName2);
        if(result == 0){
            //se i nomi sono uguali ordino i files per percorso
            return file1.getPath().compareTo(file2.getPath());
        } else {
            return result;
        }
    }


    /**
     * Compara i files per dimensione in ordine crescente. Se le dimensioni sono uguali (solitamente cartelle vuote) compara per nome.
     */
    private int comparaPerDimensione(SmbFile file1, SmbFile file2) {
        try {
            final long fileSize1 = file1.length();
            final long fileSize2 = file2.length();
            //ascending order
            int result = Long.compare(fileSize1, fileSize2);
            if(result == 0){
                //se le dimensioni sono uguali (solitamente cartelle vuote) ordino i files per nome
                return comparaPerNome(file1, file2);
            } else {
                return result;
            }
        } catch (SmbException e) {
            e.printStackTrace();
            return 0;
        }
    }


    /**
     * Compara i files per data in ordine crescente. Se le date sono uguali compara per nome.
     */
    private int comparaPerData(SmbFile file1, SmbFile file2) {
        try {
            final long data1 = file1.lastModified();
            final long data2 = file2.lastModified();
            //ascending order
            int result = Long.compare(data1, data2);
            if (result == 0) {
                //se le date sono uguali ordino i files per nome
                return comparaPerNome(file1, file2);
            } else {
                return result;
            }
        } catch (SmbException e) {
            e.printStackTrace();
            return 0;
        }
    }


    /**
     * Compara i files per tipo (estensione) in ordine crescente. Se le estensioni sono uguali compara per nome.
     */
    private int comparaPerTipo(SmbFile file1, SmbFile file2){
        final String ext1 = FileUtils.getFileExtension(file1.getName());
        final String ext2 = FileUtils.getFileExtension(file2.getName());
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
    public static List<SmbFile> ordinaPerNome(List<SmbFile> daOrdinare){
        final OrdinatoreFilesLan ordinatore = new OrdinatoreFilesLan();
        ordinatore.setMostraNascosti(true);
        return ordinatore.ordinaListaFiles(daOrdinare);
    }
}
