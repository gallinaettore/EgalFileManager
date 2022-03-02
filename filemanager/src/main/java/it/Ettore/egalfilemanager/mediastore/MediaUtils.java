package it.Ettore.egalfilemanager.mediastore;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.utils.FileUtils;


/**
 *
 *  Classe di utilità per la gestione del Media Store
 *  @author Ettore
 */

public class MediaUtils {
    public static final int MEDIA_TYPE_INVALID = 0;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final int MEDIA_TYPE_AUDIO = 3;
    public static final int MEDIA_TYPE_FILES = 4;
    public static final int MEDIA_TYPE_PLAYLIST = 5;

    private final Context context;


    /**
     *
     * @param context Il context deve essere sempre un'activity qualora si utilizzi il metodo addFilesToMediaLibrary()
     */
    public MediaUtils(@NonNull Context context){
        this.context = context;
    }



    /**
     *
     * @param mediaType intero da valorizzare con una delle costanti MEDIA_TYPE di questa classe
     * @return L'uri relativo al tipo di media
     * @throws IllegalArgumentException Se il mediaType non è riconosciuto
     *
     */
    protected Uri uriForType(int mediaType){
        switch (mediaType){
            case MEDIA_TYPE_IMAGE:
                return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            case MEDIA_TYPE_VIDEO:
                return MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            case MEDIA_TYPE_AUDIO:
                return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            case MEDIA_TYPE_PLAYLIST:
                return MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
            case MEDIA_TYPE_FILES:
                return MediaStore.Files.getContentUri("external");
            default:
                throw new IllegalArgumentException("Media Type non gestito");
        }
    }



    /**
     *
     * @param mediaType intero da valorizzare con una delle costanti MEDIA_TYPE di questa classe.
     *                  Attenzione: non è possibile utilizzare MEDIA_TYPE_FILES. Per ottenere il numero di media files utilizzare il metodo getMediaCount(List<Album> listaAlbums)
     *                  E' molto consigliabile utilizzare questo metodo all'interno di un task perchè potrebbe generare ANR in alcuni dispositivi.
     * @return Il numero di elementi totali per quel tipo di media. 0 se non è possibile stabilire il numero di media presenti
     */
    public int getMediaCount(int mediaType){
        final Uri uri = uriForType(mediaType);
        final String[] colums = {MediaStore.MediaColumns.DATA};
        int count = 0;
        try (Cursor cursor = context.getContentResolver().query(uri, colums, null, null, null)){
            count = cursor.getCount();
        } catch (Exception ignored){} //Security exception se non è stato concesso il permesso "storage"
        return count;
    }



    /**
     *
     * @param listaAlbums lista contenente tutti gli ambums ottenuta con findAlbums
     * @return il numero di elementi totali presenti in quella lista
     */
    public int getMediaCount(List<Album> listaAlbums){
        int count = 0;
        for(Album album : listaAlbums){
            count += album.size();
        }
        return count;
    }



    /**
     *
     * @param mime mime type del file
     * @return Una delle costanti MEDIA_TYPE di questa classe. MEDIA_TYPE_INVALID se il mime type è null
     */
    private int mediaTypeFromMime(String mime){
        if(mime == null){
            return MEDIA_TYPE_INVALID;
        } else if(mime.startsWith("image/")){
            return MEDIA_TYPE_IMAGE;
        } else if (mime.startsWith("video/")){
            return MEDIA_TYPE_VIDEO;
        } else if (mime.startsWith("audio/")){
            return MEDIA_TYPE_AUDIO;
        } else {
            return MEDIA_TYPE_FILES;
        }
    }


    /**
     * Rimuove i files o le cartelle dalla media library
     * @param files lista di files da rimuovere dal media store
     *
     */
    public void removeFilesFromMediaLibrary(List<File> files){
        if(files == null || files.isEmpty()) return;
        final ContentResolver resolver = context.getContentResolver();
        final ArrayList<ContentProviderOperation> operationList = new ArrayList<>();
        final String selection = MediaStore.MediaColumns.DATA + "=?";
        final String selectionDirectory = MediaStore.MediaColumns.DATA + " LIKE ?";

        ContentProviderOperation contentProviderOperation;
        //Uri uri;
        for(File file : files){
            if(file.isFile()) {
                int mediaType = mediaTypeFromMime(FileUtils.getMimeType(file));
                final Uri uri = uriForType(mediaType);
                contentProviderOperation = ContentProviderOperation.newDelete(uri).withSelection(selection, new String[]{file.getAbsolutePath()}).build();
                operationList.add(contentProviderOperation);
            } else {
                //se è una directory cancella tutti i files che iniziano con quel path

                //cancello le immagini che iniziano con quel path (se ci sono)
                final Uri uriImmagini = uriForType(MEDIA_TYPE_IMAGE);
                contentProviderOperation = ContentProviderOperation.newDelete(uriImmagini).withSelection(selectionDirectory, new String[]{file.getAbsolutePath() + "%"}).build();
                operationList.add(contentProviderOperation);

                //cancello i video che iniziano con quel path (se ci sono)
                final Uri uriVideo = uriForType(MEDIA_TYPE_VIDEO);
                contentProviderOperation = ContentProviderOperation.newDelete(uriVideo).withSelection(selectionDirectory, new String[]{file.getAbsolutePath() + "%"}).build();
                operationList.add(contentProviderOperation);

                //cancello i files audio che iniziano con quel path (se ci sono)
                final Uri uriAudio = uriForType(MEDIA_TYPE_AUDIO);
                contentProviderOperation = ContentProviderOperation.newDelete(uriAudio).withSelection(selectionDirectory, new String[]{file.getAbsolutePath() + "%"}).build();
                operationList.add(contentProviderOperation);

                //cancello gli altri files che iniziano con quel path (se ci sono)
                final Uri uriFiles = uriForType(MEDIA_TYPE_FILES);
                contentProviderOperation = ContentProviderOperation.newDelete(uriFiles).withSelection(selectionDirectory, new String[]{file.getAbsolutePath() + "%"}).build();
                operationList.add(contentProviderOperation);
            }
        }

        try {
            resolver.applyBatch(MediaStore.AUTHORITY, operationList);
        } catch (Exception e){
            e.printStackTrace();
        }
    }



    /**
     * Un po' più lento del nuovo metodo removeFilesFromMediaLibrary()
     *
     * @param files lista di files da rimuovere dal media store
     * @return true se l'operazione ha successo
     * @deprecated
     */
    @Deprecated
    public boolean removeFilesFromMediaLibraryOldMethod(List<File> files){
        if(files == null || files.isEmpty()) return false;
        final ContentResolver resolver = context.getContentResolver();
        int righeCancellate = 0;
        for(File file : files){
            int mediaType = mediaTypeFromMime(FileUtils.getMimeType(file));
            if(mediaType == MEDIA_TYPE_INVALID) continue;
            final Uri uri = uriForType(mediaType);
            righeCancellate += resolver.delete(uri, MediaStore.MediaColumns.DATA + "=?", new String[]{file.getAbsolutePath()});
        }
        return righeCancellate == files.size();
    }



    /**
     *  Il context passato al costruttore della classe MediaUtils deve essere un'activity!
     *
     * @param files lista di files da aggiungere al media store
     * @param mediaScannerListener listener eseguito al termine dell'aggiunta di tutti i files
     */
    public void addFilesToMediaLibrary(List<File> files, MediaScannerUtil.MediaScannerListener mediaScannerListener){
        final MediaScannerUtil mediaScannerUtil = new MediaScannerUtil(context);
        mediaScannerUtil.scanFiles(files, mediaScannerListener);
    }



    /**
     *  Il context passato al costruttore della classe MediaUtils deve essere un'activity o un IntentService!
     *
     * @param file file da aggiungere al media store
     * @param mediaScannerListener listener eseguito al termine dell'aggiunta di tutti i files
     */
    public void addFileToMediaLibrary(File file, MediaScannerUtil.MediaScannerListener mediaScannerListener){
        if(file != null) {
            final MediaScannerUtil mediaScannerUtil = new MediaScannerUtil(context);
            final List<File> files = new ArrayList<>(1);
            files.add(file);
            mediaScannerUtil.scanFiles(files, mediaScannerListener);
        }
    }


    /**
     * Restituisce il nome del tipo di media da usare su crashlytics
     * @param mediaType MediaType
     * @return Nome comprensibile
     */
    public static String getNomeMediaType(int mediaType){
        switch (mediaType){
            case MEDIA_TYPE_INVALID:
                return "invalid";
            case MEDIA_TYPE_IMAGE:
                return "image";
            case MEDIA_TYPE_VIDEO:
                return "video";
            case MEDIA_TYPE_AUDIO:
                return "audio";
            case MEDIA_TYPE_FILES:
                return "files";
            case MEDIA_TYPE_PLAYLIST:
                return "playlist";
            default:
                return "";
        }
    }
}
