package it.Ettore.androidutilsx.ui;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Field;

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


public abstract class MyActivity extends AppCompatActivity {
    public static final String KEY_BUNDLE_RES_ID_THEME = "res_id_theme";
    private static SharedPreferences prefs;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        //Verifico se viene passato la risorsa di un tema nell'intent ed eventualmente la applico
        int resIdTema = getIntent().getIntExtra(KEY_BUNDLE_RES_ID_THEME, 0);
        if(resIdTema != 0){
            setTheme(resIdTema);
        }

        super.onCreate(savedInstanceState);
    }


    public void getOverflowMenu() {
        //Visualizza il menu nell'ActionBar
        try {
            final ViewConfiguration config = ViewConfiguration.get(this);
            final Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ignored) {}
    }


    public void nascondiTastiera(){
        nascondiTastiera(getCurrentFocus());
    }


    public void nascondiTastiera(View view){
        try{
            final InputMethodManager inputManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            if(view != null){
                inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } catch (Exception ignored){}
    }


    public void mostraTastiera(){
        mostraTastiera(getCurrentFocus());
    }


    public void mostraTastiera(View view){
        try{
            final InputMethodManager inputManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            if(view != null){
                inputManager.showSoftInput(view, InputMethodManager.SHOW_FORCED);
            }
        } catch (Exception ignored){}
    }


    public SharedPreferences getPrefs(){
        if(prefs == null){
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
        }
        return prefs;
    }


    public void settaTitolo(@StringRes Integer resId){
        final ActionBar actionBar = getSupportActionBar();
        if(actionBar != null && resId != null){
            actionBar.setTitle(resId);
        }
    }


    public void settaTitolo(@Nullable String titolo){
        final ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setTitle(titolo);
        }
    }
}
