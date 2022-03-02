package it.Ettore.egalfilemanager.filemanager;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import java.io.File;

import it.Ettore.egalfilemanager.fileutils.MultirinominaFiles;


/**
 * Classe di utilità per ottenere i nomi progressivi dei files per il rinominamento multiplo
 */
public class MultirinominaFilesLocali extends MultirinominaFiles {

    /**
     *
     * @param nuovoNome Nuovo nome da assegnare al file. A questo verrà aggiunto il numero progressivo
     * @param maxCifre Cifre che avrà il numero progressivo. Solitamente passare il numero massimo di files da rinominare.
     */
    public MultirinominaFilesLocali(String nuovoNome, int maxCifre){ //passare lista files.size
        super(nuovoNome, maxCifre);
    }


    /**
     * Restituisce il nome del file completo di numero progressivo (da usare durante l'iterazione dei vari files)
     * @param fileDaRinominare File da rinominare
     * @return Nome del file completo di numero progressivo. Null in caso di errore
     */
    public String getNuovoNomeFileProgressivo(File fileDaRinominare){
        if(fileDaRinominare == null || getNuovoNome() == null) return null;

        int ultimoIndiceUsato = getUltimoIndiceUsato() + 1;
        for(int i = ultimoIndiceUsato; i < MAX_INDICE; i++) {
            final String nuovoNomeFile = nomeProgressivoFile(getNuovoNome(), i);
            final File possibileNuovoNome = new File(fileDaRinominare.getParentFile(), nuovoNomeFile);
            if(!possibileNuovoNome.exists()){
                setUltimoIndiceUsato(i);
                break;
            }
        }

        return nomeProgressivoFile(getNuovoNome(), getUltimoIndiceUsato());
    }
}
