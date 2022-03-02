package it.Ettore.egalfilemanager.fileutils;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import java.text.NumberFormat;
import java.util.Locale;

import it.Ettore.androidutilsx.utils.FileUtils;


/**
 * Classe base di utilità per ottenere i nomi progressivi dei files per il rinominamento multiplo
 */
public abstract class MultirinominaFiles {
    protected static final int MAX_INDICE = 10000;
    private final String nuovoNome;
    private final int maxCifre;
    private int ultimoIndiceUsato;


    /**
     *
     * @param nuovoNome Nuovo nome da assegnare al file. A questo verrà aggiunto il numero progressivo
     * @param maxCifre Cifre che avrà il numero progressivo. Solitamente passare il numero massimo di files da rinominare.
     */
    protected MultirinominaFiles(String nuovoNome, int maxCifre){
        this.nuovoNome = nuovoNome;
        this.maxCifre = maxCifre;
    }


    /**
     * Crea il nome file seguito da un numero. Utile per generare i nomi del multirinomina
     * @param nomeFile Nome del file in cui inserire il numero progressivo
     * @param num Numero da inserire nel nome del file
     * @return Nuovo nome del file nel formato "nomefile (num).ext" o  "nomefile (num)" se è senza estenzione. Null se il nome del file è null o se il numero progressivo non è valido (inferiore a 1)
     */
    protected String nomeProgressivoFile(String nomeFile, int num){
        if(nomeFile == null || num <= 0) return null;
        final String primaParteNome = FileUtils.getFileNameWithoutExt(nomeFile);
        final String ext = FileUtils.getFileExtension(nomeFile);
        final NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
        nf.setGroupingUsed(false);
        int cifre = String.valueOf(maxCifre).length();
        nf.setMinimumIntegerDigits(cifre);
        nf.setMaximumIntegerDigits(cifre);
        final String numString = nf.format(num);
        String nuovoNomeFile;
        if(ext != null && !ext.isEmpty()){
            nuovoNomeFile = String.format(Locale.ENGLISH, "%s (%s).%s", primaParteNome, numString, ext);
        } else {
            nuovoNomeFile = String.format(Locale.ENGLISH, "%s (%s)", primaParteNome, numString);
        }
        return nuovoNomeFile;
    }


    /**
     * Restituisce il nuovo nome del file impostato
     * @return Nuovo nome del file impostato
     */
    protected String getNuovoNome(){
        return this.nuovoNome;
    }


    /**
     * Restituisce l'ultimo indice (numero progressivo) usato nel file
     * @return Ultimo indice usato
     */
    protected int getUltimoIndiceUsato(){
        return this.ultimoIndiceUsato;
    }


    /**
     * Imposta l'indice usato nel file
     * @param ultimoIndiceUsato Indice usato
     */
    protected void setUltimoIndiceUsato(int ultimoIndiceUsato){
        this.ultimoIndiceUsato = ultimoIndiceUsato;
    }
}
