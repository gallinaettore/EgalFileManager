package it.Ettore.egalfilemanager.iconmanager.glide;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.egalfilemanager.R;


public class ClearGlideCacheTask extends AsyncTask<Void, Void, Void> {
    private final WeakReference<Context> context;


    public ClearGlideCacheTask(@NonNull Context context){
        this.context = new WeakReference<>(context);
    }


    @Override
    protected Void doInBackground(Void... voids) {
        Glide.get(context.get()).clearDiskCache();
        return null;
    }


    @Override
    protected void onPostExecute(Void aVoid) {
        ColoredToast.makeText(context.get(), R.string.cache_cancellata, Toast.LENGTH_LONG).show();
    }
}
