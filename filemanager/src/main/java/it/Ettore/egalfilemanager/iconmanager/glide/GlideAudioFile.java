package it.Ettore.egalfilemanager.iconmanager.glide;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import java.io.File;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.utils.FileUtils;

public class GlideAudioFile {
    private File file;


    public GlideAudioFile(@NonNull File file){
        this.file = file;
    }


    public File getFile(){
        return file;
    }


    public boolean isAudioFile(){
        return FileUtils.getMimeType(file).startsWith("audio/");
    }
}
