package it.Ettore.egalfilemanager.iconmanager.glide;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.graphics.Bitmap;

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import androidx.annotation.NonNull;


public class GlideApkModelLoadingFactory implements ModelLoaderFactory<GlideApkFile, Bitmap> {

    @NonNull
    @Override
    public ModelLoader<GlideApkFile, Bitmap> build(@NonNull MultiModelLoaderFactory unused) {
        return new GlideApkModel();
    }

    @Override
    public void teardown() {
        // Do nothing.
    }
}
