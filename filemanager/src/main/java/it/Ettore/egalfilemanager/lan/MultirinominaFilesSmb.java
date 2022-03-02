package it.Ettore.egalfilemanager.lan;


import it.Ettore.egalfilemanager.fileutils.MultirinominaFiles;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Classe di utilità per ottenere i nomi progressivi dei files per il rinominamento multiplo
 */
public class MultirinominaFilesSmb extends MultirinominaFiles {


    /**
     *
     * @param nuovoNome Nuovo nome da assegnare al file. A questo verrà aggiunto il numero progressivo
     * @param maxCifre Cifre che avrà il numero progressivo. Solitamente passare il numero massimo di files da rinominare.
     */
    public MultirinominaFilesSmb(String nuovoNome, int maxCifre) {
        super(nuovoNome, maxCifre);
    }


    /**
     * Restituisce il nome del file completo di numero progressivo (da usare durante l'iterazione dei vari files)
     * @param fileDaRinominare File da rinominare
     * @return Nome del file completo di numero progressivo. Null in caso di errore
     */
    public String getNuovoNomeFileProgressivo(SmbFile fileDaRinominare, NtlmPasswordAuthentication auth){
        if(fileDaRinominare == null || getNuovoNome() == null) return null;

        int ultimoIndiceUsato = getUltimoIndiceUsato() + 1;
        for(int i = ultimoIndiceUsato; i < MAX_INDICE; i++) {
            final String nuovoNomeFile = nomeProgressivoFile(getNuovoNome(), i);
            try {
                final SmbFile possibileNuovoNome = new SmbFile(fileDaRinominare.getParent() + nuovoNomeFile, auth);
                if(!possibileNuovoNome.exists()){
                    setUltimoIndiceUsato(i);
                    break;
                }
            } catch (Exception e) {
                setUltimoIndiceUsato(0);
                e.printStackTrace();
            }
        }

        return nomeProgressivoFile(getNuovoNome(), getUltimoIndiceUsato());
    }
}
