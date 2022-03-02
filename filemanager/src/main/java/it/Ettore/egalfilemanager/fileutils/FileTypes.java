package it.Ettore.egalfilemanager.fileutils;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import java.io.File;

import it.Ettore.androidutilsx.utils.FileUtils;


/**
 * Classe per la gestione dei tipi di files
 */
public class FileTypes {
    public static final int TYPE_SCONOSCIUTO = 0;
    public static final int TYPE_TESTO = 1;
    public static final int TYPE_AUDIO = 2;
    public static final int TYPE_VIDEO = 3;
    public static final int TYPE_IMMAGINE = 4;
    public static final int TYPE_PDF = 5;
    public static final int TYPE_ARCHIVIO = 6;
    public static final int TYPE_WORD = 7;
    public static final int TYPE_EXCEL = 8;
    public static final int TYPE_APK = 9;



    /**
     * Restituisce il tipo di file
     * @param file File da analizzare
     * @return Tipo di file (tra le costanti incluse in questa classe)
     */
    public static int getTypeForFile(File file){
        if(file == null) return TYPE_SCONOSCIUTO;
        return getTypeForFile(file.getName());
    }



    /**
     * Restituisce il tipo di file
     * @param fileName Nome del file da analizzare
     * @return Tipo di file (tra le costanti incluse in questa classe)
     */
    public static int getTypeForFile(String fileName){
        if(fileName == null) return TYPE_SCONOSCIUTO;
        final String mime = FileUtils.getMimeType(fileName);
        final String ext = FileUtils.getFileExtension(fileName);

        /*
         * ATTENZIONE!!!!!!!
         * Quando si aggiunge un'estenzione qui, probabilmente è necessario aggiungerla anche su FindAlbumTask.nonMediaFilesAlbums()
         */

        if(mime.startsWith("text/")){
            return TYPE_TESTO;
        } else if (mime.startsWith("audio/")){
            return TYPE_AUDIO;
        } else if (mime.startsWith("video/")){
            return TYPE_VIDEO;
        } else if (mime.startsWith("image/")) {
            return TYPE_IMMAGINE;
        } else if ("pdf".equalsIgnoreCase(ext)){
            return TYPE_PDF;
        } else if (mime.equals("application/zip") || mime.equals("x-zip-compressed") || mime.equals("application/rar") || mime.equals("application/x-rar-compressed") || mime.equals("application/java-archive")){
            return TYPE_ARCHIVIO;
        } else if("doc".equalsIgnoreCase(ext) || "docx".equalsIgnoreCase(ext)){
            return TYPE_WORD;
        } else if("xls".equalsIgnoreCase(ext) || "xlsx".equalsIgnoreCase(ext) || "xlsm".equalsIgnoreCase(ext)){
            return TYPE_EXCEL;
        } else if ("apk".equalsIgnoreCase(ext)) {
            return TYPE_APK;
        } else {
            return TYPE_SCONOSCIUTO;
        }
    }
}
