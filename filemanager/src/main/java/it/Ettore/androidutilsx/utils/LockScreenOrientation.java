package it.Ettore.androidutilsx.utils;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;
import android.view.Surface;


/**
 * Classe di utilità per le dialogs
 */
public class LockScreenOrientation {

    /**
     * Blocca il cambio di orientamento dell'activity.
     * Da utilizzare quando si mostra una dialog durante un task, in modo da non permettere il cambio di orientamento e non far chiudere la dialog
     //(perchè il task continua ma la dialog non sarà aggiornata)
     * @param activity Activity da bloccare
     */
    @SuppressLint("WrongConstant")
    public static void lock(Activity activity){
        if(activity == null || activity.isFinishing()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        } else {
            //dovrebbe funzionare con la maggior parte dei dispositivi

            final Display display = activity.getWindowManager().getDefaultDisplay();
            int rotation = display.getRotation();

            final Point size = new Point();
            display.getSize(size);

            int lock;

            if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
                // if rotation is 0 or 180 and width is greater than height, we have a tablet
                if (size.x > size.y) {
                    if (rotation == Surface.ROTATION_0) {
                        lock = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    } else {
                        lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    }
                } else {
                    // we have a phone
                    if (rotation == Surface.ROTATION_0) {
                        lock = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    } else {
                        lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    }
                }
            } else {
                // if rotation is 90 or 270 and width is greater than height, we have a phone
                if (size.x > size.y) {
                    if (rotation == Surface.ROTATION_90) {
                        lock = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    } else {
                        lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    }
                } else {
                    // we have a tablet
                    if (rotation == Surface.ROTATION_90) {
                        lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    } else {
                        lock = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    }
                }
            }
            activity.setRequestedOrientation(lock);
        }
    }




    /**
     * Sblocca il cambio di orientamento dell'activity.
     * Da utilizzare al termine del task in modo da ripristinare la possibilità di rotazione dell'activity
     * @param activity Activity da sbloccare
     */
    public static void unlock(Activity activity){
        if(activity == null || activity.isFinishing()) return;
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }


}
