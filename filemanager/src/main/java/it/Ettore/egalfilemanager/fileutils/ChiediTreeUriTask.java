package it.Ettore.egalfilemanager.fileutils;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;

import java.io.File;
import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.ui.ColoredProgressDialog;
import it.Ettore.egalfilemanager.activity.BaseActivity;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/



/**
 * Task che verifica se il file si trova in uno storage esterno e chiede il tree uri da Lollipop in poi
 */
public class ChiediTreeUriTask extends AsyncTask<Void, Void, Boolean> {
    private final WeakReference<BaseActivity> baseActivity;
    private final File file;
    private final boolean showProgressDialog;
    private ColoredProgressDialog progress;


    /**
     *
     * @param baseActivity Activity principale che richiede il tree uri
     * @param file File da verificare
     */
    public ChiediTreeUriTask(@NonNull BaseActivity baseActivity, File file, boolean showProgressDialog){
        this.baseActivity = new WeakReference<>(baseActivity);
        this.file = file;
        this.showProgressDialog = showProgressDialog;
    }


    /**
     *
     * @param baseActivity Activity principale che richiede il tree uri
     * @param directory Directory da verificare
     */
    public ChiediTreeUriTask(@NonNull BaseActivity baseActivity, File directory){
        this(baseActivity, directory, false);
    }


    /**
     * Mostro la dialog
     */
    @Override
    protected void onPreExecute(){
        if(showProgressDialog) {
            progress = ColoredProgressDialog.show(baseActivity.get(), null, null);
        }
    }


    /**
     * Verifica in backgroung
     * @param obj Nessun parametro
     * @return True se è necessario chiedere il tree uri
     */
    @Override
    protected Boolean doInBackground(Void... obj) {
        //return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && new StoragesUtils(baseActivity.get()).isOnExtSdCard(directory) && !SAFUtils.isWritableNormalOrSaf(baseActivity.get(), directory);

        final StoragesUtils storagesUtils = new StoragesUtils(baseActivity.get());
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && storagesUtils.isOnExtSdCard(file)){
            final File externalStorage = storagesUtils.getExtStorageForFile(file);
            return !SAFUtils.isWritableNormalOrSaf(baseActivity.get(), externalStorage);
        }
        return false;
    }


    /**
     * Chiede il tree uri
     * @param success True se è necessario chiedere il tree uri
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onPostExecute(Boolean success){
        try {
            if (progress != null && progress.isShowing()) {
                progress.dismiss();
            }
        } catch (final IllegalArgumentException ignored) {}

        if(success && baseActivity.get() != null && !baseActivity.get().isFinishing()){
            baseActivity.get().chiediTreeUriSdEsterna(file);
        }
    }
}
