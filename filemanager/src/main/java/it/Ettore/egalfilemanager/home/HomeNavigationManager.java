package it.Ettore.egalfilemanager.home;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.fileutils.StoragesUtils;

import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_ROOT_EXPLORER;


/**
 * Classa per la gestione degli items da visualizzare nella schermata principale
 */
public class HomeNavigationManager {
    private static final int ARCHIVIO_LOCALE_START_MENU_ID = 1000;
    private final Activity activity;
    private int archivioLocaleNextMenuId = ARCHIVIO_LOCALE_START_MENU_ID;


    /**
     *
     * @param activity Activity chiamante
     */
    public HomeNavigationManager(@NonNull Activity activity){
        this.activity = activity;
    }


    /**
     * Analizza il dispositivo e crea una lista di items comprendenti le sd cards e il percorso root (se disponibile)
     * @return Lista di items
     */
    public List<HomeItem> listaItemsArchivioLocale(){
        final List<HomeItem> listaItems = listaItemsSdCards();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        final boolean rootExplorer = prefs.getBoolean(KEY_PREF_ROOT_EXPLORER, false);
        if(rootExplorer){
            listaItems.add(new HomeItem(R.drawable.main_root, activity.getString(R.string.root_directory), new File("/"), archivioLocaleNextMenuId, R.drawable.navmenu_root));
            archivioLocaleNextMenuId++;
        }
        return listaItems;
    }


    /**
     * Analizza il dispositivo e crea una lista di items comprendenti le sd cards
     * @return Lista di items
     */
    public List<HomeItem> listaItemsSdCards(){
        final StoragesUtils storagesUtils = new StoragesUtils(activity);
        final List<HomeItem> listaItems = new ArrayList<>();
        listaItems.add(new HomeItem(R.drawable.main_dispositivo, activity.getString(R.string.memoria_interna), storagesUtils.getInternalStorage(), archivioLocaleNextMenuId, R.drawable.navmenu_mem_interna));
        archivioLocaleNextMenuId++;
        final List<File> externals = storagesUtils.getExternalStorages();
        for(File storage : externals){
            String storageLabel = storagesUtils.getVolumeLabel(storage);
            if(storageLabel == null){
                storageLabel = activity.getString(R.string.memoria_esterna);
            }
            boolean usb = storagesUtils.isInUsbStorage(storage);
            @DrawableRes int resIdIcona = usb ? R.drawable.main_usb : R.drawable.main_sdcard;
            @DrawableRes int resIdIcNavMenu = usb ? R.drawable.navmenu_usb : R.drawable.navmenu_mem_esterna;
            listaItems.add(new HomeItem(resIdIcona, storageLabel, storage, archivioLocaleNextMenuId, resIdIcNavMenu));
            archivioLocaleNextMenuId++;
        }
        return listaItems;
    }

}
