package it.Ettore.egalfilemanager.lan;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/



import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import it.Ettore.androidutilsx.utils.FileUtils;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;


/**
 * Classe di utilità per i SmbFile
 */
public class SmbFileUtils {

    /**
     * Se il file di destinazione non esiste è possibile utilizzare il file di destinazione, altrimenti ritorno un file rinominatoi del tipo "Filedestinazione (1).ext"
     *
     * @param fileDestinazioneOriginale File originale di destinazione
     * @return Nuovo File di destinazione. Null in caso di errore
     */
    public static SmbFile rinominaFilePerEvitareSovrascrittura(SmbFile fileDestinazioneOriginale, NtlmPasswordAuthentication auth){
        try {
            if(fileDestinazioneOriginale == null || !fileDestinazioneOriginale.exists()) return fileDestinazioneOriginale;
        } catch (SmbException e) {
            e.printStackTrace();
            return null;
        }
        final String nomeOriginale = FileUtils.getFileNameWithoutExt(fileDestinazioneOriginale.getName()).replace("/", "");
        final String ext = FileUtils.getFileExtension(fileDestinazioneOriginale.getName());
        for(int i = 1; i < 10000; i++){
            String nuovoNomeFile;
            if(ext != null && !ext.isEmpty()){
                nuovoNomeFile = String.format(Locale.ENGLISH, "%s (%d).%s", nomeOriginale, i, ext);
            } else {
                nuovoNomeFile = String.format(Locale.ENGLISH, "%s (%d)", nomeOriginale, i);
            }
            if(fileDestinazioneOriginale.getName().endsWith("/")){
                nuovoNomeFile += "/";
            }
            SmbFile nuovoFileDestinazione = null;
            try {
                nuovoFileDestinazione = new SmbFile(fileDestinazioneOriginale.getParent() + nuovoNomeFile, auth);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            boolean fileExists = false;
            if(nuovoFileDestinazione != null){
                try {
                    fileExists = nuovoFileDestinazione.exists();
                } catch (SmbException e) {
                    e.printStackTrace();
                }
            }
            if(!fileExists){
                return nuovoFileDestinazione;
            }
        }
        return null;
    }


    /**
     * Verifica se il path rappresenta una directory (path finisce con /)
     * @param smbFile File da anlizzare
     * @return True se rappresenta una directory (path finisce con /)
     */
    public static boolean isDirectory(SmbFile smbFile){
        return smbFile.getName().endsWith("/");
    }


    /**
     * Verifica se il file passato rappresenta la directory root
     * @param smbFile File da anlizzare
     * @return True se è la directory root
     */
    public static boolean isRoot(SmbFile smbFile){
        return smbFile.getPath().equals("smb://") || smbFile.getPath().equals("smb:////");
    }


    /**
     * Crea un oggetto NtlmPasswordAuthentication a partire da username e password
     * @param username User
     * @param password Pwd
     * @return Autenticazione. Null se user o password sono null.
     */
    public static NtlmPasswordAuthentication createAuth(String username, String password){
        NtlmPasswordAuthentication auth = null;
        if(username != null && password != null){
            final String userpwd = username + ":" + password;
            auth = new NtlmPasswordAuthentication(userpwd);
        }
        return auth;
    }


    /**
     * Ottiene il nome del file dall'intero path
     * @param path Path completo
     * @return Nome file. Stringa vuota se il parametro passato non è valido
     */
    public static String getNameFromPath(String path){
        if(path == null || !path.contains("/") || path.equals("smb:/") || path.equals("smb://") || path.equals("smb:////")){
            return "";
        }
        int indiceUltimoSlash = path.lastIndexOf("/");
        return path.substring(indiceUltimoSlash + 1);
    }


    /**
     * Restituisce una lista di path a partire da una lista di files
     * @param listaFiles Lista di files
     * @return Lista di path
     */
    public static ArrayList<String> listFileToListPath(List<SmbFile> listaFiles){
        if(listaFiles != null) {
            final ArrayList<String> listaPaths = new ArrayList<>(listaFiles.size());
            for (SmbFile smbFile : listaFiles) {
                listaPaths.add(smbFile.getPath());
            }
            return listaPaths;
        } else {
            return new ArrayList<>();
        }
    }


    /**
     * Restituisce una lista di files a partire da una lista di path
     * @param listaPaths Lista di path
     * @param auth Dati autenticazione. Null se non occorre autenticazione
     * @return Lista di files
     */
    public static List<SmbFile> listPathToListFile(List<String> listaPaths, NtlmPasswordAuthentication auth){
        if(listaPaths != null){
            final ArrayList<SmbFile> listaFiles = new ArrayList<>(listaPaths.size());
            for(String path : listaPaths){
                try {
                    listaFiles.add(new SmbFile(path, auth));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
            listaFiles.trimToSize();
            return listaFiles;
        } else {
            return new ArrayList<>();
        }
    }
}
