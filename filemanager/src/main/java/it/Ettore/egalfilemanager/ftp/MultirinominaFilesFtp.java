package it.Ettore.egalfilemanager.ftp;

import org.apache.commons.net.ftp.FTPClient;

import it.Ettore.egalfilemanager.fileutils.MultirinominaFiles;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/



/**
 * Classe di utilità per ottenere i nomi progressivi dei files per il rinominamento multiplo
 */
public class MultirinominaFilesFtp extends MultirinominaFiles {


    /**
     * @param nuovoNome Nuovo nome da assegnare al file. A questo verrà aggiunto il numero progressivo
     * @param maxCifre  Cifre che avrà il numero progressivo. Solitamente passare il numero massimo di files da rinominare.
     */
    public MultirinominaFilesFtp(String nuovoNome, int maxCifre) {
        super(nuovoNome, maxCifre);
    }


    /**
     * Restituisce il nome del file completo di numero progressivo (da usare durante l'iterazione dei vari files)
     * @param fileDaRinominare File da rinominare
     * @return Nome del file completo di numero progressivo. Null in caso di errore
     */
    public String getNuovoNomeFileProgressivo(FTPClient ftpClient, FtpElement fileDaRinominare){
        if(fileDaRinominare == null || getNuovoNome() == null || ftpClient == null) return null;

        int ultimoIndiceUsato = getUltimoIndiceUsato() + 1;
        for(int i = ultimoIndiceUsato; i < MAX_INDICE; i++) {
            final String nuovoNomeFile = nomeProgressivoFile(getNuovoNome(), i);
            final String possibileNuovoPath = fileDaRinominare.getParent() + "/" + nuovoNomeFile;
            if(!FtpFileUtils.fileExists(ftpClient, possibileNuovoPath)){
                setUltimoIndiceUsato(i);
                break;
            }
        }

        return nomeProgressivoFile(getNuovoNome(), getUltimoIndiceUsato());
    }
}
