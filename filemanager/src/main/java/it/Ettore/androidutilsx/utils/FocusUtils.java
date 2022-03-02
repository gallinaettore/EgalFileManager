package it.Ettore.androidutilsx.utils;/*
Copyright (c)2019 - Egal Net di Ettore Gallina
*/


import android.app.Activity;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;


/**
 * Classe di utilità per la gestione del focus
 */
public class FocusUtils {
    private static final String TAG = "FocusUtils";


    /**
     * Metodo da chiamare nell'oncReate dell'activity o del fragment.
     * Avvia il monitoraggio e logga quale view ha il focus
     * @param activity Activity
     */
    public static void startMonitoring(Activity activity){
        new Thread(() -> {
            int oldId = -1;
            while (true) {
                if(activity == null){
                    break; //se eseguito all'interno di un fragment e viene chiuso
                }
                View view = activity.getCurrentFocus();
                if (view != null && view.getId() != oldId) {
                    oldId = view.getId();
                    String idName = "";
                    try {
                        idName = activity.getResources().getResourceEntryName(view.getId());
                    } catch (Resources.NotFoundException e) {
                        idName = String.valueOf(view.getId());
                    }
                    Log.i(TAG, "Focused Id: " + idName + ", Class: " + view.getClass().toString().replace("class ", ""));
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }
}
