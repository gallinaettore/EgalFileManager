package it.Ettore.egalfilemanager.iconmanager.glide;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.Context;

import java.io.File;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.utils.FileUtils;

public class GlideApkFile {
    private final File file;
    private final Context context;


    public GlideApkFile(@NonNull Context context, @NonNull File file){
        this.context = context;
        this.file = file;
    }


    public File getFile(){
        return file;
    }


    public Context getContext() {
        return context;
    }


    public boolean isApkFile(){
        return FileUtils.getFileExtension(file).equalsIgnoreCase("apk");
    }
}
