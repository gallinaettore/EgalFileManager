package it.Ettore.egalfilemanager.mediastore;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.collection.LongSparseArray;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.fileutils.FileTypes;

import static it.Ettore.egalfilemanager.mediastore.MediaUtils.MEDIA_TYPE_AUDIO;
import static it.Ettore.egalfilemanager.mediastore.MediaUtils.MEDIA_TYPE_FILES;
import static it.Ettore.egalfilemanager.mediastore.MediaUtils.MEDIA_TYPE_IMAGE;
import static it.Ettore.egalfilemanager.mediastore.MediaUtils.MEDIA_TYPE_PLAYLIST;
import static it.Ettore.egalfilemanager.mediastore.MediaUtils.MEDIA_TYPE_VIDEO;


/**
 * Ricerca degli albums in un task separato
 */
public class FindAlbumsTask extends AsyncTask<Void, Void, List<Album>> {
    private final WeakReference<Activity> activity;
    private final int mediaType;
    private final MediaUtils mediaUtils;
    private final AlbumsSearchListener listener;
    private boolean mostraCartellePerFilesAudio;


    /**
     *
     * @param activity Activity chiamante
     * @param mediaType Tipo di media ga gestire. Una delle costanti della classe MediaUtils.
     * @param listener Listener eseguito al termine della ricerca
     */
    public FindAlbumsTask(@NonNull Activity activity, int mediaType, AlbumsSearchListener listener){
        this.activity = new WeakReference<>(activity);
        this.mediaType = mediaType;
        this.mediaUtils = new MediaUtils(activity);
        this.listener = listener;
    }


    public void setMostraCartellePerFilesAudio(boolean mostraCartellePerFilesAudio) {
        this.mostraCartellePerFilesAudio = mostraCartellePerFilesAudio;
    }


    @Override
    protected List<Album> doInBackground(Void... voids) {
        try{
            switch (mediaType){
                case MEDIA_TYPE_IMAGE:
                case MEDIA_TYPE_VIDEO:
                    return mediaFilesAlbums(mediaType);
                case MEDIA_TYPE_AUDIO:
                    if(mostraCartellePerFilesAudio){
                        final List<Album> albumsAudio = audioFilesAlbumsPerCartella(MEDIA_TYPE_AUDIO);
                        final List<Album> albumsPlaylist = audioFilesAlbumsPerCartella(MEDIA_TYPE_PLAYLIST);
                        return mergeAlbumList(albumsAudio, albumsPlaylist);
                    } else {
                        final List<Album> listaAlbums = mediaFilesAlbums(MEDIA_TYPE_AUDIO);
                        listaAlbums.addAll(mediaFilesAlbums(MEDIA_TYPE_PLAYLIST));
                        return listaAlbums;
                    }
                case MEDIA_TYPE_FILES:
                    return nonMediaFilesAlbums();
                default:
                    return null;
            }
        } catch (Exception e){
            return null;
        }
    }


    @Override
    protected void onPostExecute(List<Album> albums) {
        super.onPostExecute(albums);
        if(albums != null && activity.get() != null && !activity.get().isFinishing() && listener != null) {
            listener.onAlbumsFound(albums);
        }
    }


    /**
     * Restituisce una lista di albums contenenti files classificati come non media. Se utilizzato dall'esterno chiamare questo metodo un thread diverso da quello dell'UI
     * @return lista di albums contenenti files classificati come non media
     */
    public List<Album> nonMediaFilesAlbums(){
        final Map<String, Album> mapAlbum = new HashMap<>();
        final List<Album> listaCartelle = new ArrayList<>();
        if(activity.get() == null || activity.get().isFinishing()){
            return listaCartelle;
        }
        final Uri uri = mediaUtils.uriForType(MEDIA_TYPE_FILES);
        final String[] colums = { MediaStore.MediaColumns.DATA };
        //final String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_NONE; //esclude media files

        //selection: tutti i files con una determinata estenzione o un determinato mime
        final String selection =
                        MediaStore.Files.FileColumns.MIME_TYPE + " LIKE 'text/%'" + " OR " +
                        MediaStore.MediaColumns.DATA + " LIKE '%.pdf'" + " OR " +
                        MediaStore.MediaColumns.DATA + " LIKE '%.doc'" + " OR " +
                        MediaStore.MediaColumns.DATA + " LIKE '%.docx'" + " OR " +
                        MediaStore.MediaColumns.DATA + " LIKE '%.xls'" + " OR " +
                        MediaStore.MediaColumns.DATA + " LIKE '%.xlsx'" + " OR " +
                        MediaStore.MediaColumns.DATA + " LIKE '%.xlsm'" + " OR " +
                        MediaStore.MediaColumns.DATA + " LIKE '%.apk'" + " OR " +
                        MediaStore.MediaColumns.DATA + " LIKE '%.jar'" + " OR " +
                        MediaStore.MediaColumns.DATA + " LIKE '%.zipx'" + " OR " +
                        MediaStore.MediaColumns.DATA + " LIKE '%.rar'" + " OR " +
                        MediaStore.MediaColumns.DATA + " LIKE '%.java'" + " OR " +
                        MediaStore.MediaColumns.DATA + " LIKE '%.xml'" + " OR " +
                        MediaStore.Files.FileColumns.MIME_TYPE + " LIKE 'application/zip'" + " OR " +
                        MediaStore.Files.FileColumns.MIME_TYPE + " LIKE 'x-zip-compressed'" + " OR " +
                        MediaStore.Files.FileColumns.MIME_TYPE + " LIKE 'application/rar'" + " OR " +
                        MediaStore.Files.FileColumns.MIME_TYPE + " LIKE 'application/x-rar-compressed'";


        Cursor cursor = null;
        try {
            cursor = activity.get().getContentResolver().query(uri, colums, selection, null, null);

            if (cursor != null) {
                int columnIndexData = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

                String filePath, albumName;
                Album album;
                while (cursor.moveToNext()) {
                    if (isCancelled()) {
                        return listaCartelle;
                    }
                    filePath = cursor.getString(columnIndexData);
                    albumName = albumNameFromFile(filePath);
                    if (albumName == null) {
                        continue; //il tipo di file non è da inserire nella lista e lo salto
                    }
                    album = mapAlbum.get(albumName);
                    if (album != null) {
                        //album esistente
                        album.addElement(filePath);
                        int indiceLista = listaCartelle.indexOf(album);
                        listaCartelle.set(indiceLista, album);
                    } else {
                        //nuovo album
                        long id = albumIdFromFile(filePath);
                        album = new Album(id, albumName, MEDIA_TYPE_FILES);
                        album.addElement(filePath);
                        listaCartelle.add(album);
                        mapAlbum.put(albumName, album);
                    }
                }
            }
        } catch (Exception ignored){
        } finally {
            try {
                cursor.close();
            } catch (Exception ignored){}
        }
        return listaCartelle;
    }


    @Deprecated
    public List<Album> nonMediaFilesAlbumsOld(){
        final Map<String, Album> mapAlbum = new HashMap<>();
        final List<Album> listaCartelle = new ArrayList<>();
        if(activity.get() == null || activity.get().isFinishing()){
            return listaCartelle;
        }
        final Uri uri = mediaUtils.uriForType(MEDIA_TYPE_FILES);
        final String[] colums = { MediaStore.MediaColumns.DATA };
        final String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_NONE; //esclude media files

        Cursor cursor = null;
        try {
            cursor = activity.get().getContentResolver().query(uri, colums, selection, null, null);

            if (cursor != null) {
                int columnIndexData = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

                String filePath, albumName;
                Album album;
                while (cursor.moveToNext()) {
                    if (isCancelled()) {
                        return listaCartelle;
                    }
                    filePath = cursor.getString(columnIndexData);
                    albumName = albumNameFromFile(filePath);
                    if (albumName == null) {
                        continue; //il tipo di file non è da inserire nella lista e lo salto
                    }
                    album = mapAlbum.get(albumName);
                    if (album != null) {
                        //album esistente
                        album.addElement(filePath);
                        int indiceLista = listaCartelle.indexOf(album);
                        listaCartelle.set(indiceLista, album);
                    } else {
                        //nuovo album
                        long id = albumIdFromFile(filePath);
                        album = new Album(id, albumName, MEDIA_TYPE_FILES);
                        album.addElement(filePath);
                        listaCartelle.add(album);
                        mapAlbum.put(albumName, album);
                    }
                }
            }
        } catch (SecurityException ignored){
        } finally {
            try {
                cursor.close();
            } catch (Exception ignored){}
        }
        return listaCartelle;
    }


    /**
     * Restituisce una lista di albums contenenti files multimediali (Immagini, Video o Audio). Ogni album rappresenta una cartella per immagini e video; per i files audio invece rappresenta proprio il nome dell'album
     * @param mediaType intero da valorizzare con una delle costanti MEDIA_TYPE di questa classe.
     *                  Attenzione: non è possibile utilizzare MEDIA_TYPE_FILES.
     * @return lista di albums contenenti files classificati come media (immagini, video o audio)
     */
    private List<Album> mediaFilesAlbums(int mediaType){
        final LongSparseArray<Album> albumSparseArray = new LongSparseArray<>();
        final List<Album> listaCartelle = new ArrayList<>();
        if(activity.get() == null || activity.get().isFinishing()){
            return listaCartelle;
        }
        final Uri uri = mediaUtils.uriForType(mediaType);
        final String folderNameColumn = getFolderNameColumn(mediaType);
        final String folderIdColumn = getFolderIdColumn(mediaType);
        final String[] colums = { MediaStore.MediaColumns.DATA, folderNameColumn, folderIdColumn };
        Cursor cursor = null;
        try {
            cursor = activity.get().getContentResolver().query(uri, colums, null, null, null);

            if (cursor != null) {
                int columnIndexData = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                int columnIndexFolderName = cursor.getColumnIndexOrThrow(folderNameColumn);
                int columnIndexFolderId = cursor.getColumnIndexOrThrow(folderIdColumn);

                long id;
                String filePath, folderName;
                Album album;
                while (cursor.moveToNext()) {
                    if (isCancelled()) {
                        return listaCartelle;
                    }
                    id = cursor.getLong(columnIndexFolderId);
                    filePath = cursor.getString(columnIndexData);
                    if(filePath == null){
                        continue;
                    }
                    folderName = cursor.getString(columnIndexFolderName);
                    if (albumSparseArray.get(id) != null) {
                        //album esistente
                        album = albumSparseArray.get(id);
                        album.addElement(filePath);
                        int indiceLista = listaCartelle.indexOf(album);
                        listaCartelle.set(indiceLista, album);
                    } else {
                        //nuovo album
                        album = new Album(id, folderName, mediaType);
                        album.addElement(filePath);
                        listaCartelle.add(album);
                        albumSparseArray.put(id, album);
                    }
                }

            }
        } catch (SecurityException ignored){
        } finally {
            try {
                cursor.close();
            } catch (Exception ignored){}
        }
        return listaCartelle;
    }


    /**
     * Restituisce una lista di cartelle (Albums) per i files audio
     * @return Lista di albums (cartelle) contenenti i files audio
     */
    private List<Album> audioFilesAlbumsPerCartella(int mediaType){
        final Map<String, Album> mapAlbums = new HashMap<>();
        final List<Album> listaCartelle = new ArrayList<>();
        if(activity.get() == null || activity.get().isFinishing()){
            return listaCartelle;
        }
        final Uri uri = mediaUtils.uriForType(mediaType);
        final String[] colums = { MediaStore.MediaColumns.DATA };
        Cursor cursor = null;
        try {
            cursor = activity.get().getContentResolver().query(uri, colums, null, null, null);

            if (cursor != null) {
                int columnIndexData = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

                File folder;
                String folderPath, filePath;
                Album album;
                while (cursor.moveToNext()) {
                    if (isCancelled()) {
                        return listaCartelle;
                    }
                    filePath = cursor.getString(columnIndexData);
                    if(filePath == null){
                        continue;
                    }
                    folder = new File(filePath).getParentFile();
                    folderPath = folder.getAbsolutePath();
                    album = mapAlbums.get(folderPath);
                    if (album != null) {
                        //album esistente
                        album.addElement(filePath);
                        int indiceLista = listaCartelle.indexOf(album);
                        listaCartelle.set(indiceLista, album);
                    } else {
                        //nuovo album
                        final long id = folderPath.hashCode();
                        album = new Album(id, folder.getName(), mediaType);
                        album.addElement(filePath);
                        listaCartelle.add(album);
                        mapAlbums.put(folderPath, album);
                    }
                }

            }
        } catch (SecurityException ignored){
        } finally {
            try {
                cursor.close();
            } catch (Exception ignored){}
        }
        return listaCartelle;
    }


    /**
     *
     * @param mediaType intero da valorizzare con una delle costanti MEDIA_TYPE di questa classe.
     *                   Attenzione: non è possibile utilizzare MEDIA_TYPE_FILES.
     *  @return Il nome della colonna che conterrà gli ids delle cartelle nel content resolver
     *  @throws IllegalArgumentException Se il mediaType non è riconosciuto
     */
    private String getFolderIdColumn(int mediaType){
        switch (mediaType){
            case MEDIA_TYPE_IMAGE:
                return MediaStore.Images.Media.BUCKET_ID;
            case MEDIA_TYPE_VIDEO:
                return MediaStore.Video.Media.BUCKET_ID;
            case MEDIA_TYPE_AUDIO:
                return MediaStore.Audio.Media.ALBUM_ID;
            case MEDIA_TYPE_PLAYLIST:
                return MediaStore.Audio.Playlists._ID;
            default:
                throw new IllegalArgumentException("Media Type non gestito");
        }
    }


    /**
     *
     *  @param mediaType intero da valorizzare con una delle costanti MEDIA_TYPE di questa classe.
     *                   Attenzione: non è possibile utilizzare MEDIA_TYPE_FILES.
     *  @return Il nome della colonna che conterrà i nomi delle cartelle nel content resolver
     *  @throws IllegalArgumentException Se il mediaType non è riconosciuto
     */
    private String getFolderNameColumn(int mediaType){
        switch (mediaType){
            case MEDIA_TYPE_IMAGE:
                return MediaStore.Images.Media.BUCKET_DISPLAY_NAME;
            case MEDIA_TYPE_VIDEO:
                return MediaStore.Video.Media.BUCKET_DISPLAY_NAME;
            case MEDIA_TYPE_AUDIO:
                return MediaStore.Audio.Media.ALBUM;
            case MEDIA_TYPE_PLAYLIST:
                return MediaStore.Audio.Playlists.NAME;
            default:
                throw new IllegalArgumentException("Media Type non gestito");
        }
    }


    /**
     *
     * @param filePath path del file
     * @return Nome dell'album (altri media) che contiene i files di quel tipo. Null se il path è una directory, se il path è null, se il path si trova nella directory /Android/data o se è un tipo di file non gestito
     */
    private String albumNameFromFile(String filePath){
        if(filePath == null || filePath.contains("/Android/data/")){
            return null;
        }
        final File file = new File(filePath);
        if(file.isDirectory() || activity.get() == null || activity.get().isFinishing()) return null;
        switch (FileTypes.getTypeForFile(file)){
            case FileTypes.TYPE_TESTO:
                return activity.get().getString(R.string.album_testo);
            case FileTypes.TYPE_PDF:
                return activity.get().getString(R.string.album_pdf);
            case FileTypes.TYPE_ARCHIVIO:
                return activity.get().getString(R.string.album_compressi);
            case FileTypes.TYPE_WORD:
                return activity.get().getString(R.string.album_documenti);
            case FileTypes.TYPE_EXCEL:
                return activity.get().getString(R.string.album_fogli_calcolo);
            case FileTypes.TYPE_APK:
                return activity.get().getString(R.string.album_apk);
            default:
                return null;
        }
    }


    /**
     *
     * @param filePath path del file
     * @return Id dell'album (altri media) che contiene i files di quel tipo. 0L se è un tipo di file non gestito
     */
    private long albumIdFromFile(String filePath){
        if(filePath == null){
            return 0L;
        }
        final File file = new File(filePath);
        switch (FileTypes.getTypeForFile(file)){
            case FileTypes.TYPE_TESTO:
                return 4752165987400L;
            case FileTypes.TYPE_PDF:
                return 4752165987401L;
            case FileTypes.TYPE_ARCHIVIO:
                return 4752165987402L;
            case FileTypes.TYPE_WORD:
                return 4752165987403L;
            case FileTypes.TYPE_EXCEL:
                return 4752165987404L;
            case FileTypes.TYPE_APK:
                return 4752165987405L;
            default:
                return 0L;
        }
    }


    /**
     * Unisce 2 liste di albums. Se la lista 1 contiene un album della lista 2, gli elementi dell'album sono uniti
     * @param list1 Lista di album
     * @param list2 Altra lista di album
     * @return Lista con gli album uniti
     */
    private List<Album> mergeAlbumList(final List<Album> list1, final List<Album> list2){
        final List<Album> fullList = new ArrayList<>(list1);
        final LongSparseArray<Album> sparseArray = new LongSparseArray<>(fullList.size());
        for(Album album : fullList){
            sparseArray.put(album.getId(), album);
        }
        for(Album album : list2){
            final Album albumOfList1 = sparseArray.get(album.getId());
            if(albumOfList1 == null){
                //album non presente nella lista1, lo aggiungo
                fullList.add(album);
            } else {
                //album già presente, aggiungo solo gli elementi all'album
                albumOfList1.addElements(album.getElementi());
            }
        }
        return fullList;
    }



    /**
     *  Interfaccia chiamata al termine del task che riporta la lista di tutti gli album trovati
     */
    public interface AlbumsSearchListener {
        void onAlbumsFound(List<Album> albums);
    }
}
