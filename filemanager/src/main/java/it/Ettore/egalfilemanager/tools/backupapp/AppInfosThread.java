package it.Ettore.egalfilemanager.tools.backupapp;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;


/**
 * Thread per la ricerca delle app installate nel dispositivo
 */
public class AppInfosThread implements Runnable {
    public static final int TYPE_SYSTEM = 1;
    public static final int TYPE_USER = 2;
    private final WeakReference<Activity> activity;
    private final AppInfosListener listener;
    private final int type;
    private boolean interrompi;


    /**
     *
     * @param activity Activity
     * @param type Tipo di app da cercare. Usare le costanti TYPE di questa classe.
     * @param listener Listener chiamato al termine della ricerca.
     */
    public AppInfosThread(@NonNull Activity activity, int type, AppInfosListener listener) {
        this.activity = new WeakReference<>(activity);
        this.type = type;
        this.listener = listener;
    }


    /**
     * Avvia la ricerca
     */
    public void start(){
        new Thread(this).start();
    }


    /**
     * Interrompe la ricerca
     */
    public void interrompi(){
        interrompi = true;
    }


    /**
     * Effettua la ricerca in background
     */
    @Override
    public void run() {
        try {
            final PackageManager pm = activity.get().getPackageManager();
            final List<PackageInfo> packagesInfos = pm.getInstalledPackages(0);
            final List<AppInfo> listaInfo = new ArrayList<>(packagesInfos.size());
            AppInfo appInfo;
            ApplicationInfo ai;
            for (PackageInfo packageInfo : packagesInfos) {
                if(interrompi) return;
                ai = packageInfo.applicationInfo;
                boolean userApp = isUserApp(ai);
                if((type == TYPE_USER && !userApp) || (type == TYPE_SYSTEM && userApp)){
                    continue;
                }
                appInfo = new AppInfo();
                appInfo.name = (String) ai.loadLabel(pm);
                appInfo.apk = new File(ai.publicSourceDir);
                appInfo.packageName = packageInfo.packageName;
                appInfo.isSystemApp = !userApp;
                appInfo.versionName = packageInfo.versionName;
                appInfo.versionCode = packageInfo.versionCode;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    appInfo.minSdk = ai.minSdkVersion;
                }
                appInfo.targetSdk = ai.targetSdkVersion;
                try {
                    appInfo.installer = pm.getInstallerPackageName(packageInfo.packageName);
                } catch (Exception ignored) {}
                listaInfo.add(appInfo);
            }
            Collections.sort(listaInfo);
            callListener(listaInfo);
        } catch (Exception e){
            callListener(new ArrayList<>());
        }
    }


    /**
     * Verifica il tipo di app
     * @param ai ApplicationInfo
     * @return True se è un'app utente. False se è un'app di sistema.
     */
    private boolean isUserApp(ApplicationInfo ai) {
        int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
        return (ai.flags & mask) == 0;
    }


    /**
     * Esegui il listener nel thread principale
     * @param listaAppInfos Lista di informazioni sulle app
     */
    private void callListener(final List<AppInfo> listaAppInfos){
        if(activity.get() != null && !activity.get().isFinishing() && listener != null) {
            activity.get().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listener.onAppInfosObtained(listaAppInfos);
                }
            });
        }
    }


    /**
     * Listener dell'AppInfoThread
     */
    public interface AppInfosListener {

        /**
         * Chiamato al termine della ricerca
         * @param listaInfo Lista di informazioni sulle app installate
         */
        void onAppInfosObtained(List<AppInfo> listaInfo);
    }
}
