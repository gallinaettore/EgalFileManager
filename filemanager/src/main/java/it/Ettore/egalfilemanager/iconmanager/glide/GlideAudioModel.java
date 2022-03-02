package it.Ettore.egalfilemanager.iconmanager.glide;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.graphics.Bitmap;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

import java.io.InputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


/**
 * Loads an {@link InputStream} from a Base 64 encoded String.
 */
public final class GlideAudioModel implements ModelLoader<GlideAudioFile, Bitmap> {

    @Nullable
    @Override
    public LoadData<Bitmap> buildLoadData(@NonNull GlideAudioFile file, int width, int height, @NonNull Options options) {
        return new LoadData<>(new ObjectKey(file), new GlideAudioDataFetcher(file));
    }


    /**
     * Verifica che il tipo di file passato sia corretto
     * @param file File audio
     * @return True se è un file audio
     */
    @Override
    public boolean handles(@NonNull GlideAudioFile file) {
        return file.isAudioFile();
    }
}
