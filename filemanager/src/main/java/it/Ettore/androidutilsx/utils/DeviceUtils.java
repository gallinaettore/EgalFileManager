package it.Ettore.androidutilsx.utils;
/*
Copyright (c)2019 - Egal Net di Ettore Gallina
*/


import static android.content.Context.UI_MODE_SERVICE;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import androidx.annotation.NonNull;


/**
 * Classe di utilità per analizzare il dispositivo
 */
public class DeviceUtils {


    /**
     * Verifica se l'app è eseguita su Android TV
     * @param context Context
     * @return True se l'app è eseguita su Android TV
     */
    public static boolean isAndroidTV(@NonNull Context context){
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(UI_MODE_SERVICE);
        return uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }


    /**
     * Verifica se l'app è eseguita su un dispositivo con schermo grande
     * @param context Context
     * @return True se lo schermo  è LARGE o XLARGE
     */
    public static boolean isLargeScreen(@NonNull Context context){
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE ||
                (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }


    /**
     * Verifica se il dispositivo è in modalità landscape
     * @param context Context
     * @return True se il dispositivo è in modalità landscape
     */
    public static boolean isLandscape(@NonNull Context context){
        int orientation = context.getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_LANDSCAPE;
    }


    /**
     * Verifica se l'app è eseguita su un dispositivo grande messo in modalità landscape
     * @param context Context
     * @return True se l'app è eseguita su un dispositivo grande messo in modalità landscape
     */
    public static boolean isLargeLand(@NonNull Context context){
        return isLargeScreen(context) && isLandscape(context);
    }


    /**
     * Restituisce il l'Api Level del dispositivo
     * @return Api Level del dispositivo
     */
    public static int getApiLevel(){
        return Build.VERSION.SDK_INT;
    }
}
