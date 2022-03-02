package it.Ettore.egalfilemanager.activity;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;

import it.Ettore.androidutilsx.utils.DurataSplash;
import it.Ettore.androidutilsx.utils.SplashFullScreenUtils;
import it.Ettore.egalfilemanager.NotificationChannelManager;
import it.Ettore.egalfilemanager.R;


/**
 * Activity che mostra una splash screen e avvia la verifica della licenza
 */
public class ActivitySplash extends BaseActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        new SplashFullScreenUtils(this).hideSystemUI();

        //Creo il notification channel per le varie notifiche delle operazioni
        final NotificationChannelManager notificationChannelManager = new NotificationChannelManager(this);
        notificationChannelManager.creaChannelBackground();
        notificationChannelManager.creaChannelOperazioni();

        DurataSplash durataSplash = new DurataSplash(500);
        new Handler().postDelayed(() -> {
            startActivity(new Intent(ActivitySplash.this, ActivityMain.class));
            finish();
        }, durataSplash.getDelay());
    }


    /**
     * Implementazione vuota per evitare che venga impostato un tema diverso
     */
    @Override
    protected void settaTema() {}
}
