package it.Ettore.egalfilemanager.iconmanager.glide;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.graphics.Bitmap;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

import androidx.annotation.NonNull;


@GlideModule
public class MyAppGlideModule extends AppGlideModule {


    /**
     * Registra i componenti personalizzati
     * @param context .
     * @param glide .
     * @param registry .
     */
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry.prepend(GlideAudioFile.class, Bitmap.class, new GlideAudioModelLoadingFactory());
        registry.prepend(GlideApkFile.class, Bitmap.class, new GlideApkModelLoadingFactory());
    }
}
