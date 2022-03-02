package it.Ettore.egalfilemanager;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.fragment.FragmentMain;


/**
 * Classe che riceve gli eventi di cambio stato sugli storages (Quando viene inserita o rimossa una sd card)
 */
public class StorageStatusReceiver extends BroadcastReceiver {


    /**
     * Al cambio di stato viene aggiornato il fragment main e nla navigation bar
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        final FragmentMain fragmentMain = FragmentMain.getExistingInstance();
        final ActivityMain activityMain = ActivityMain.getExistingInstance();
        try{
            fragmentMain.aggiornaLayoutArchivioLocale();
            activityMain.aggiornaMenuArchivioLocale();
        } catch (Exception ignored){} //eccezione nel tentativo di aggiornamento ma le view non sono visualizzate sullo schermo
    }


    /**
     * Crea l'intent filter da utilizzare quando si registra il receiver
     * @return IntentFilter con le azioni relative al montaggio/smontaggio media
     */
    public static IntentFilter getIntentFilter(){
        final IntentFilter broadcastIntentFilter = new IntentFilter();
        broadcastIntentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        broadcastIntentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        broadcastIntentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        broadcastIntentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        return broadcastIntentFilter;
    }
}
