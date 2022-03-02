package it.Ettore.egalfilemanager;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;


/**
 * Classe utility per la creazione dei notification channel
 */
public class NotificationChannelManager {
    public static final String NOTIF_CHANNEL_ID_OPERAZIONI = "operazioni_channel_id";
    public static final String NOTIF_CHANNEL_ID_BACKGROUND = "background_channel_id";

    private final Context context;
    private final NotificationManager notificationManager;


    /**
     *
     * @param context Context
     */
    public NotificationChannelManager(@NonNull Context context){
        this.context = context;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }


    /**
     * Crea un channel poco invasivo per le operazioni in background, non suona e non mostra popup
     */
    public void creaChannelBackground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager.getNotificationChannel(NOTIF_CHANNEL_ID_BACKGROUND) == null) {
            final NotificationChannel channelBackground = new NotificationChannel(NOTIF_CHANNEL_ID_BACKGROUND, context.getString(R.string.notif_id_operazioni_backgroung), NotificationManager.IMPORTANCE_LOW);
            channelBackground.setDescription("Required for background operations");
            channelBackground.setBypassDnd(true);
            notificationManager.createNotificationChannel(channelBackground);
        }
    }


    /**
     * Crea un channel per le notifiche comuni (ad esempio operazione terminata), suona e mostra il popop
     */
    public void creaChannelOperazioni() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager.getNotificationChannel(NOTIF_CHANNEL_ID_OPERAZIONI) == null ) {
            final NotificationChannel channelOperazioni = new NotificationChannel(NOTIF_CHANNEL_ID_OPERAZIONI, context.getString(R.string.notif_id_operazioni_varie), NotificationManager.IMPORTANCE_HIGH);
            channelOperazioni.setDescription("Call notifications at the end of some operations");
            notificationManager.createNotificationChannel(channelOperazioni);
        }
    }
}
