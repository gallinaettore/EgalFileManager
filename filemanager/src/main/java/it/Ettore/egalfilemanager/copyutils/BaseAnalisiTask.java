package it.Ettore.egalfilemanager.copyutils;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.app.Activity;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.ui.ColoredProgressDialog;
import it.Ettore.androidutilsx.utils.LockScreenOrientation;
import it.Ettore.egalfilemanager.R;


/**
 * Classe generale per gestire l'analisi pre-copia di files in un task separato
 */
public abstract class BaseAnalisiTask extends AsyncTask <Void, Void, Boolean> {
    private final WeakReference<Activity> activity;
    private ColoredProgressDialog progress;
    private boolean cancellaOrigine;


    /**
     * @param activity Activity chiamante
     */
    protected BaseAnalisiTask(@NonNull Activity activity){
        this.activity = new WeakReference<>(activity);
    }


    /**
     * Imposta se utilizzare il metodo copia o taglia
     * @param cancellaOrigine True per la funzione taglia (cancellerà i file di origine). False per la funzione copia.
     */
    public void setCancellaOrigine(boolean cancellaOrigine){
        this.cancellaOrigine = cancellaOrigine;
    }


    /**
     * Restituisce il booleano per la cancellazione dei files di origine
     * @return True modalità "taglia". False modalità "copia"
     */
    protected boolean isCancellaOrigine(){
        return cancellaOrigine;
    }


    /**
     * Mostra la dialog
     */
    @Override
    protected void onPreExecute(){
        if(activity.get() != null && !activity.get().isFinishing()){
            progress = ColoredProgressDialog.show(activity.get(), null, activity.get().getString(R.string.lettura_dati_cartelle));
            progress.setCancelable(false);
            LockScreenOrientation.lock(activity.get());
        }
    }


    /**
     * Chiude la dialog
     */
    protected void dismissWaitDialog(){
        LockScreenOrientation.unlock(activity.get());
        try {
            if (progress != null && progress.isShowing()) {
                progress.dismiss();
            }
        } catch (final IllegalArgumentException ignored) {}
    }
}
