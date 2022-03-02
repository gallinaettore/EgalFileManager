package it.Ettore.egalfilemanager.iconmanager.glide;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import androidx.annotation.NonNull;
import it.Ettore.egalfilemanager.iconmanager.IconManager;


public class GlideApkDataFetcher implements DataFetcher<Bitmap> {
    private final GlideApkFile glideApkFile;


    public GlideApkDataFetcher(@NonNull GlideApkFile glideApkFile){
        this.glideApkFile = glideApkFile;
    }


    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super Bitmap> callback) {
        //estraggo la bitmap del file
        Bitmap thumbnail = null;
        final PackageInfo packageInfo = glideApkFile.getContext().getPackageManager().getPackageArchiveInfo(glideApkFile.getFile().getPath(), PackageManager.GET_ACTIVITIES);
        if(packageInfo != null) {
            final ApplicationInfo appInfo = packageInfo.applicationInfo;
            appInfo.sourceDir = glideApkFile.getFile().getPath();
            appInfo.publicSourceDir = glideApkFile.getFile().getPath();
            final Drawable icon = appInfo.loadIcon(glideApkFile.getContext().getPackageManager());
            thumbnail = IconManager.getBitmapFromDrawable(icon);
        }

        //notifico la creazione della bitmap
        callback.onDataReady(thumbnail);
    }


    /**
     * Chiude le risorse aperte
     */
    @Override
    public void cleanup() {}


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
