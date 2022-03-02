package it.Ettore.egalfilemanager.view;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import it.Ettore.androidutilsx.utils.MyMath;
import it.Ettore.egalfilemanager.Costanti;
import it.Ettore.egalfilemanager.R;


/**
 * Classe di utilità per le views
 */
public class ViewUtils {


    /**
     * Corregge i margini dei FAB per versioni inferiori a Lollipop (in queste versioni veniva aggiunto un margine in più)
     * @param fabs FlatingActionButtons con i margini da correggere
     */
    public static void correctFabMargin(FloatingActionButton... fabs){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            for(FloatingActionButton fab : fabs) {
                final ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
                final int px = (int) MyMath.dpToPx(fab.getContext(), 8);
                params.setMargins(-px, -px, -px, -px);
                fab.setLayoutParams(params);
            }
        }
    }


    public static @StyleRes int getResIdTheme(@NonNull Context context){
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String tema = prefs.getString(Costanti.KEY_PREF_TEMA, Costanti.PREF_VALUE_TEMA_LIGHT);
        switch (tema){
            case Costanti.PREF_VALUE_TEMA_LIGHT:
                return R.style.AppTheme;
            case Costanti.PREF_VALUE_TEMA_DARK:
                return R.style.AppThemeDark;
            default:
                throw new IllegalArgumentException("Tema non gestito: " + tema);
        }
    }
}
