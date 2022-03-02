package it.Ettore.egalfilemanager.tools.backupapp;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.os.Build;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import it.Ettore.egalfilemanager.R;


/**
 * Classe che contiene le informazioni di un'app installata sul dispositivo
 */
public class AppInfo implements Comparable<AppInfo> {
    public String name, packageName, versionName, installer;
    public File apk;
    public boolean isSystemApp;
    public int versionCode, minSdk, targetSdk;


    AppInfo(){}


    /**
     * Restituisce le informazioni sottoforma di mappa (da utilizzare nella dialog info)
     * @param ctx Context
     * @return Mappa con le informazioni
     */
    public Map<String, String> toMap(@NonNull Context ctx){
        final Map<String, String> map = new LinkedHashMap<>();
        map.put(ctx.getString(R.string.apk_nome_app), name);
        map.put(ctx.getString(R.string.apk_package_name), packageName);
        map.put(ctx.getString(R.string.apk_nome_versione), versionName);
        map.put(ctx.getString(R.string.apk_codice_versione), String.valueOf(versionCode));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //per le versioni precedenti non posso ottenere il min sdk
            map.put(ctx.getString(R.string.apk_min_sdk), String.valueOf(minSdk));
        }
        map.put(ctx.getString(R.string.apk_target_sdk), String.valueOf(targetSdk));
        map.put(ctx.getString(R.string.apk_type), isSystemApp ? ctx.getString(R.string.apk_system) : ctx.getString(R.string.apk_user));
        if(installer != null){
            map.put(ctx.getString(R.string.apk_installer), installer);
        }
        return map;
    }


    @Override
    public int compareTo(@NonNull AppInfo appInfo) {
        return name.compareToIgnoreCase(appInfo.name);
    }
}
