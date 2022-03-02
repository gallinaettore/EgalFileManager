package it.Ettore.androidutilsx.utils;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    public static boolean copy(File inputFile, File outputFile){
        if(inputFile.toString().equals(outputFile.toString())){
            //se il percoso sorgente e destinazione sono uguali
            return true;
        }
        boolean success;
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(inputFile);
            out = new FileOutputStream(outputFile);
            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            success = true;
        } catch (Exception e){
            e.printStackTrace();
            success = false;
        } finally {
            try{
                out.close();
            } catch (Exception ignored){}
            try{
                in.close();
            } catch (Exception ignored){}
        }
        return success;
    }


    public static String getFileExtension(File file){
        if(file == null) return null;
        return getFileExtension(file.getName());
    }


    public static String getFileExtension(String fileName){
        if(fileName == null) return null;
        String ext = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            ext = fileName.substring(i+1);
        }
        return ext;
    }


    public static String getFileNameWithoutExt(File file){
        if(file == null) return null;
        return getFileNameWithoutExt(file.getName());
    }


    public static String getFileNameWithoutExt(String fileName){
        if(fileName == null) return null;
        final String ext = "." + getFileExtension(fileName);
        int extIndex = fileName.lastIndexOf(ext);
        if(extIndex != -1) {
            return fileName.substring(0, extIndex);
        } else {
            return fileName;
        }
    }


    @NonNull
    public static String getMimeType(String fileName){
        if(fileName == null || fileName.isEmpty()){
            return "*/*";
        }
        final String ext = getFileExtension(fileName).toLowerCase();
        String type;
        if(ext.equalsIgnoreCase("ogg")){
            type = "audio/ogg";
        } else if (ext.equalsIgnoreCase("flv")){
            type = "video/x-flv";
        } else {
            final MimeTypeMap map = MimeTypeMap.getSingleton();
            type = map.getMimeTypeFromExtension(ext);
        }
        if (type == null) {
            if("jar".equalsIgnoreCase(ext)){
                type = "application/java-archive";
            } else if("zipx".equalsIgnoreCase(ext)){
                type = "application/zip";
            } else {
                type = "*/*";
            }
        }
        return type;
    }


    public static String getMimeType(File file){
        if(file == null) return null;
        return getMimeType(file.getName());
    }


    public static String getMimeType(List<File> files){
        String mime = null;
        for(File file : files){
            final String currentMime = getMimeType(file);
            if(mime == null){
                //mime non ancora specificato (prima esecuzione del ciclo)
                mime = currentMime;
            } else {
                if(!mime.equals(currentMime)){
                    //se il mime è diverso controllo che almeno la prima parte sia uguale
                    try{
                        final String[] mimeSplit = mime.split("/");
                        final String[] currentMimeSplit = currentMime.split("/");
                        if(mimeSplit[0].equals(currentMimeSplit[0])){
                            //le prime parti dei mime sono uguali
                            mime = mimeSplit[0] + "/*";
                        } else {
                            //i mime sono completamente diversi
                            return "*/*";
                        }
                    } catch (Exception ignored){
                        //non è stato possibile confrontare i mime
                        return "*/*";
                    }
                }
            }
        }
        return mime;
    }


    public static Uri uriWithFileProvider(@NonNull Context context, File file){
        if(file == null){
            return null;
        } else {
            return FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".fileprovider", file);
        }
    }

    @Nullable
    public static File fileWithFileProviderUri(@NonNull Context context, Uri uri){
        if(uri == null){
            return null;
        } else {
            final String uriString;
            try {
                uriString = URLDecoder.decode(uri.toString(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }
            final String baseFileProvider = ".fileprovider/root";
            final String internalPath = "content://" + context.getApplicationContext().getPackageName() + ".fileprovider/external_files";
            final String otherPath = "content://" + context.getApplicationContext().getPackageName() + baseFileProvider;
            final String normalPath = "file://";
            final String mediaStorePath = "content://media/external";
            final String chromePath = "content://com.android.chrome.FileProvider/downloads";
            Log.w("Provauri", uriString);
            if(uriString.startsWith(internalPath)){
                //il file si trova nella memoria interna
                final String fileString = uriString.replace(internalPath, Environment.getExternalStorageDirectory().toString());
                return new File(fileString);
            } else if (uriString.startsWith(otherPath)){
                //il file si trova nella memoria esterna o in altri percorsi
                final String fileString = uriString.replace(otherPath, "");
                return new File(fileString);
            } else if (uriString.startsWith(normalPath)){
                //file passato da un'app esterna
                final String fileString = uriString.replace(normalPath, "");
                return new File(fileString);
            } else if (uriString.startsWith(mediaStorePath)){
                //file passato da un'app esterna tramite MediaStore
                Cursor cursor = null;
                try {
                    final String[] filePathColumn = {MediaStore.MediaColumns.DATA};
                    cursor = context.getContentResolver().query(uri, filePathColumn, null, null, null);
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    cursor.moveToFirst();
                    final String fileString = cursor.getString(columnIndex);
                    return new File(fileString);
                } catch (Exception ignored){} finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            } else if(uriString.startsWith(chromePath)){
                //file scaricato con Google Chrome
                final String fileString = uriString.replace(chromePath, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
                final File file = new File(fileString);
                if(file.exists()){
                    return file;
                } else {
                    return null;
                }
            }/* else if (uriString.startsWith("content://") && uriString.contains(baseFileProvider)){
                //files di altre mie app
                int providerStringEnd = uriString.indexOf(baseFileProvider) + baseFileProvider.length();
                final String fileString = uriString.substring(providerStringEnd);
                Log.w("Provauri", "filestring="+fileString);

                final File file = new File(fileString);
                Log.w("Provauri", "exist="+file.exists());
                if(file.exists()){
                    return file;
                } else {
                    return null;
                }
            }*/
            return null;
        }
    }


    public static boolean fileNameIsValid(String fileName){
        if(fileName == null || fileName.trim().isEmpty()) return false;
        final String[] reservedChars = {"|", "\\", "?", "*", "<", ">", "\"", ":", "/"};
        for(String character : reservedChars){
            if(fileName.contains(character)){
                return false;
            }
        }
        return true;
    }


    public static boolean isSymlink(File file) {
        if(file == null) return false;
        try {
            File canon;
            if (file.getParent() == null) {
                canon = file;
            } else {
                File canonDir = file.getParentFile().getCanonicalFile();
                canon = new File(canonDir, file.getName());
            }
            return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
        } catch (IOException e){
            return false;
        }
    }


    /**
     * Verifica se il file si trova all'interno di un percorso nascosto.
     * @param file File da verificare
     * @return True se il file è nascosto o si trova all'interno di una cartella nascosta. False se il file è visibile, se tutto il path è visibile e se il file è null.
     */
    public static boolean fileIsInHiddenPath(File file){
        if(file == null){
            return false;
        }
        if(file.isHidden()){
            return true;
        }
        File parent = file.getParentFile();
        while (parent != null){
            if(parent.isHidden()){
                return true;
            } else {
                parent = parent.getParentFile();
            }
        }
        return false;
    }



    /**
     * Verifica se il file si trova nella cartella cache dell'applicazione
     * @param file File da verificare
     * @param context Context
     * @return True se il file si trova nella cache, False in caso contrario o se il file è null.
     */
    public static boolean fileIsInCache(File file, Context context) {
        try {
            return file != null && file.getCanonicalPath().startsWith(context.getCacheDir().getCanonicalPath());
        } catch (IOException e) {
            return false;
        }
    }


    /**
     * Restituisce una lista di path a partire da una lista di files
     * @param listaFiles Lista di files
     * @return Lista di path
     */
    public static ArrayList<String> listFileToListPath(List<File> listaFiles){
        if(listaFiles != null) {
            final ArrayList<String> listaPaths = new ArrayList<>(listaFiles.size());
            for (File file : listaFiles) {
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


    /**
     * Restituisce una lista di files a partire da una lista di path
     * @param listaPaths Lista di path
     * @return Lista di files
     */
    public static List<File> listPathToListFile(List<String> listaPaths){
        if(listaPaths != null){
            final ArrayList<File> listaFiles = new ArrayList<>(listaPaths.size());
            for(String path : listaPaths){
                if(path != null){
                    listaFiles.add(new File(path));
                }
            }
            listaFiles.trimToSize();
            return listaFiles;
        } else {
            return new ArrayList<>();
        }
    }


    /**
     * Verifica se i files presenti nella lista appartengono tutti alla stessa cartella
     * @param files Lista di files
     * @return True se appartengono alla stessa cartella. False in caso contrario, se la lista è nulla o se è vuota
     */
    public static boolean filesHasSamePath(List<File> files){
        if(files == null || files.isEmpty()) return false;
        File parent = null;
        for(File file : files){
            if(parent == null){
                parent = file.getParentFile();
            } else {
                if(!parent.equals(file.getParentFile())){
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * Restituisce il nome di un file partendo da un URI.
     * Analizza prima il content resolver e se non trova risultati calcola il nome automaticamente a partire dal suo path
     * @param context Context
     * @param uri Uri del file
     * @return Nome del file
     */
    @Nullable
    public static String getFileName(@NonNull Context context, Uri uri) {
        if(uri == null) return null;
        String result = null;
        if (uri.getScheme().equals("content")) {
            final Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
