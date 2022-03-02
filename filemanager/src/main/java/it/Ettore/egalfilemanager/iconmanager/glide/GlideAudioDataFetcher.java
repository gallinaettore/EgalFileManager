package it.Ettore.egalfilemanager.iconmanager.glide;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import androidx.annotation.NonNull;
import it.Ettore.egalfilemanager.iconmanager.IconManager;


public class GlideAudioDataFetcher implements DataFetcher<Bitmap> {
    private GlideAudioFile audioFile;
    private MediaMetadataRetriever metadataRetriever;


    public GlideAudioDataFetcher(@NonNull GlideAudioFile audioFile){
        this.audioFile = audioFile;
        this.metadataRetriever = new MediaMetadataRetriever();
    }


    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super Bitmap> callback) {
        //estraggo la bitmap del file
        Bitmap thumbnail = null;
        try{
            metadataRetriever.setDataSource(audioFile.getFile().getAbsolutePath());
            final byte[] art = metadataRetriever.getEmbeddedPicture();

            if(art != null) {
                int maxSize = 500; //cover di grandezza massima 500px
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(art, 0, art.length, options);
                options.inSampleSize = IconManager.calculateInSampleSize(options, maxSize, maxSize);

                options.inJustDecodeBounds = false;
                final Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length, options);

                thumbnail = ThumbnailUtils.extractThumbnail(bitmap, maxSize, maxSize);
            }
        } catch (Exception ignored){}

        //notifico la creazione della bitmap
        callback.onDataReady(thumbnail);
    }


    /**
     * Chiude le risorse aperte
     */
    @Override
    public void cleanup() {
        try {
            metadataRetriever.release();
        } catch (Exception ignored) {}
    }


    /**
     * Annulla un'operazione se lavoro in remoto
     */
    @Override
    public void cancel() {}


    @NonNull
    @Override
    public Class<Bitmap> getDataClass() {
        return Bitmap.class;
    }


    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.LOCAL;
    }
}
