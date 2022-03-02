package it.Ettore.egalfilemanager.zipexplorer;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import java.util.zip.ZipEntry;

import androidx.annotation.NonNull;
import junrar.Archive;
import junrar.rarfile.FileHeader;


/**
 * Classe che rappresenta un elemento (file o cartella) all'interno di un archivio compresso
 */
public class ArchiveEntry implements Comparable<ArchiveEntry> {
    private ZipEntry zipEntry;
    private FileHeader rarEntry;


    /**
     *
     * @param zipEntry Elemento di un archivio zip
     */
    public ArchiveEntry(@NonNull ZipEntry zipEntry){
        this.zipEntry = zipEntry;
    }


    /**
     *
     * @param fileHeader Elemento di un archivio rar
     */
    public ArchiveEntry(@NonNull FileHeader fileHeader){
        this.rarEntry = fileHeader;
    }


    /**
     * Restituisce il path dell'elemento
     * @return Path dell'elemento
     */
    public String getPath(){
        if(zipEntry != null){
            return zipEntry.toString();
        } else if(rarEntry != null){
            return getRarPath(rarEntry);
        } else {
            return null;
        }
    }


    /**
     * Restituisce il path di un elemento di un archivio rar
     * @param fileHeader Elemento rar
     * @return Path dell'elemento
     */
    private String getRarPath(FileHeader fileHeader){
        String rarPath;
        if(fileHeader.isUnicode()){
            rarPath = fileHeader.getFileNameW().replace("\\", "/");
        } else {
            rarPath = fileHeader.getFileNameString().replace("\\", "/");
        }
        if(fileHeader.isDirectory() && !rarPath.endsWith("/")){
            rarPath += "/";
        }
        return rarPath;
    }


    /**
     * Verifica se l'elemento è una directory
     * @return True se l'elemento è una directory. False se l'elemento impostato è null.
     */
    public boolean isDirectory(){
        if(zipEntry != null){
            try {
                return zipEntry.isDirectory();
            } catch (Exception e){
                e.printStackTrace();
                return false;
            }
        } else if(rarEntry != null){
            return rarEntry.isDirectory();
        } else {
            return false;
        }
    }


    /**
     * Albero con la struttura delle directories
     * @return Struttura directory. Null se è un file che si trova nella directory principale.
     */
    public String[] getStrutturaDirectory(){
        if(getPath().contains("/")){
            //return getPath().split("/");
            final String[] splited = getPath().split("/");
            if(splited.length > 0){
                return splited;
            } else {
                return null;
            }
        } else {
            //si trova nella directory principale
            return null;
        }
    }


    /**
     * Elemento genitore
     * @return Path dell'elemento genitore. Null se non esiste un elemento genitore (si trova già nella cartella root)
     */
    public String getParent(){
        final String[] strutturaDirectory = getStrutturaDirectory();
        if(strutturaDirectory == null || strutturaDirectory.length == 1){
            //si trova già nella cartella root
            return null;
        } else {
            final String name = isDirectory() ? (getName() + "/") : getName();
            final String parent = getPath().replace(name, "");
            if(parent.equals("/")){
                return null; //cartella root
            } else {
                return parent;
            }
        }
    }


    /**
     * Nome dell'elemento
     * @return Nome dell'elemento
     */
    public String getName(){
        final String[] strutturaDirectory = getStrutturaDirectory();
        if(strutturaDirectory == null){
            //file sulla cartella principale
            return getPath();
        } else {
            return strutturaDirectory[strutturaDirectory.length-1];
        }
    }


    /**
     * Verifica se è un elemento di un archivio zip
     * @return True se è un elemento di un archivio zip
     */
    public boolean isZip(){
        return zipEntry != null;
    }


    /**
     * Verifica se è un elemento di un archivio rar
     * @return True se è un elemento di un archivio rar
     */
    public boolean isRar(){
        return rarEntry != null;
    }


    /**
     * Restituisce l'elemento zip
     * @return Elemento zip
     */
    public ZipEntry getZipEntry(){
        return zipEntry;
    }


    /**
     * Restituisce l'elemento rar
     * @param rarFile File rar che contiene l'elemento
     * @return Elemento rar
     */
    public FileHeader getRarFileHeader(Archive rarFile){
        FileHeader currentFileHeader;
        while ((currentFileHeader = rarFile.nextFileHeader()) != null) {
            if(getRarPath(currentFileHeader).equals(getRarPath(rarEntry))){
                return currentFileHeader;
            }
        }
        return null;
    }




    /**
     * Comparatore per ordinare gli elementi per percorso
     * @param entry Elemento
     * @return comparazione
     */
    @Override
    public int compareTo(@NonNull ArchiveEntry entry) {
        return getPath().compareTo(entry.getPath());
    }

}
