package it.Ettore.egalfilemanager.ftp;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.utils.FileUtils;


/**
 * Classe di utilità per la gestione di Files FTP
 */
public class FtpFileUtils {


    /**
     * Verifica che la directory esista
     * @param ftpClient Client FTP
     * @param dirPath Path assoluto della directory
     * @return True se la directory esiste. False se non esiste o se avviene un errore
     */
    public static boolean directoryExists(@NonNull FTPClient ftpClient, @NonNull String dirPath) {
        try {
            final String startDirectory = ftpClient.printWorkingDirectory();
            ftpClient.changeWorkingDirectory(dirPath);
            int returnCode = ftpClient.getReplyCode();
            ftpClient.changeWorkingDirectory(startDirectory);
            return returnCode != 550;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Verifica che il file esista
     * @param ftpClient Client FTP
     * @param filePath Path assoluto del file
     * @return True se il file esiste. False se non esiste o se avviene un errore
     */
    public static boolean fileExists(@NonNull FTPClient ftpClient, @NonNull String filePath) {
        try {
            final String parentPath = getParentFromPath(filePath);
            final String[] files = ftpClient.listNames(parentPath);
            if(files != null && files.length > 2 && files[0].equals(".") && files[1].equals("..")){
                //server Unix: restituisce solo i nomi dei files contenuti nella cartella
                return Arrays.asList(files).contains(getNameFromPath(filePath));
            } else {
                //server Windows: restituisce l'intero path dei files contenuti nella cartella
                return Arrays.asList(files).contains(filePath);
            }
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Ottiene il nome del file dall'intero path
     * @param path Path completo
     * @return Nome file. Stringa vuota se il parametro passato non è valido
     */
    public static String getNameFromPath(String path){
        if(path == null || !path.contains("/")){
            return "";
        }
        int indiceUltimoSlash = path.lastIndexOf("/");
        return path.substring(indiceUltimoSlash + 1);
    }


    /**
     * Ottiene il path della cartella genitore
     * @param path Path completo
     * @return Path della cartella genitore. Null se il parametro passato non è valido o se non esiste una cartella superiore.
     */
    public static String getParentFromPath(String path){
        if(path == null || !path.contains("/") || path.isEmpty()){
            return null;
        }
        int indiceUltimoSlash = path.lastIndexOf("/");
        return path.substring(0, indiceUltimoSlash);
    }



    /**
     * Esplora il contenuto di una cartella
     * @param ftpSession Sessione FTP
     * @param path Path della cartella da esplorare. Null se è la cartella root.
     * @return Lista di elementi (file e cartelle) all'interno del path
     */
    public static List<FtpElement> explorePath(@NonNull FtpSession ftpSession, String path){
        try {
            final FTPFile[] ftpFiles = ftpSession.getFtpClient().listFiles(path);
            if(ftpFiles == null || ftpFiles.length == 0){
                return new ArrayList<>();
            } else {
                final List<FtpElement> listaElementi = new ArrayList<>(ftpFiles.length);
                for(FTPFile ftpFile : ftpFiles){
                    if(!ftpFile.getName().equals(".") && !ftpFile.getName().equals("..")){
                        listaElementi.add(new FtpElement(ftpFile, path, ftpSession.getServerFtp().getHost()));
                    }
                }
                return listaElementi;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }



    /**
     * Se il file di destinazione non esiste è possibile utilizzare il file di destinazione, altrimenti ritorna un file rinominatoi del tipo "Filedestinazione (1).ext"
     *
     * @param ftpClient Client FTP
     * @param fileDestinazioneOriginale Path del file originale di destinazione
     * @param isDirectory True se il path passato appartiene a una directory. False se appartiene a un file
     * @return Nuovo File di destinazione. Null in caso di errore
     */
    public static String rinominaFilePerEvitareSovrascrittura(@NonNull FTPClient ftpClient, String fileDestinazioneOriginale, boolean isDirectory){
        if(fileDestinazioneOriginale == null) return fileDestinazioneOriginale;
        if(isDirectory){
            if(!directoryExists(ftpClient, fileDestinazioneOriginale)) return fileDestinazioneOriginale;
        } else {
            if(!fileExists(ftpClient, fileDestinazioneOriginale)) return fileDestinazioneOriginale;
        }

        final String fileName = getNameFromPath(fileDestinazioneOriginale);
        final String nomeOriginale = FileUtils.getFileNameWithoutExt(fileName);
        final String ext = FileUtils.getFileExtension(fileName);
        for(int i = 1; i < 10000; i++){
            String nuovoNomeFile;
            if(ext != null && !ext.isEmpty()){
                nuovoNomeFile = String.format(Locale.ENGLISH, "%s (%d).%s", nomeOriginale, i, ext);
            } else {
                nuovoNomeFile = String.format(Locale.ENGLISH, "%s (%d)", nomeOriginale, i);
            }
            final String nuovoPathDestinazione = getParentFromPath(fileDestinazioneOriginale) + "/" + nuovoNomeFile;
            boolean fileExists = false;
            if(isDirectory){
                directoryExists(ftpClient, nuovoPathDestinazione);
            } else {
                fileExists(ftpClient, nuovoPathDestinazione);
            }
            if(!fileExists){
                return nuovoPathDestinazione;
            }
        }
        return null;
    }


    /**
     * Restituisce una lista di path a partire da una lista di files
     * @param listaFiles Lista di files
     * @return Lista di path
     */
    public static ArrayList<String> listFileToListPath(List<FtpElement> listaFiles){
        if(listaFiles != null) {
            final ArrayList<String> listaPaths = new ArrayList<>(listaFiles.size());
            for (FtpElement file : listaFiles) {
                if(file != null) {
                    listaPaths.add(file.getAbsolutePath());
                }
            }
            listaPaths.trimToSize();
            return listaPaths;
        } else {
            return new ArrayList<>();
        }
    }

}
