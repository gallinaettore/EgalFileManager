/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.Ettore.egalfilemanager.musicplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import androidx.media.session.MediaButtonReceiver;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityMusicPlayer;



/**
 * Classe che gestisce le notifiche del servizio
 * Keeps track of a notification and updates it automatically for a given MediaSession. This is required so that the music service don't get killed during playback.
 */
public class MediaNotificationManager {

    public static final String MUSICPLAYER_CHANNEL = "musicplayer.channel";
    public static final int NOTIFICATION_ID = 412;
    public static final String KEY_BUNDLE_STARTED_FROM_NOTIFICATION = "started_from_notification";

    private static final String TAG = MediaNotificationManager.class.getSimpleName();
    private static final int REQUEST_CODE = 501;

    private final MusicService mService;

    private final NotificationCompat.Action mPlayAction;
    private final NotificationCompat.Action mPauseAction;
    private final NotificationCompat.Action mNextAction;
    private final NotificationCompat.Action mPrevAction;
    private final NotificationManager mNotificationManager;


    /**
     *
     * @param service Service
     */
    MediaNotificationManager(MusicService service) {
        mService = service;

        mNotificationManager = (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);

        //creo le azioni da associare alla notifica
        mPlayAction = new NotificationCompat.Action(R.drawable.ic_play_arrow_white_24dp, "Play", MediaButtonReceiver.buildMediaButtonPendingIntent(
                        mService, PlaybackStateCompat.ACTION_PLAY));
        mPauseAction = new NotificationCompat.Action(R.drawable.ic_pause_white_24dp, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(
                        mService, PlaybackStateCompat.ACTION_PAUSE));
        mNextAction = new NotificationCompat.Action(R.drawable.ic_skip_next_white_24dp, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(
                        mService, PlaybackStateCompat.ACTION_SKIP_TO_NEXT));
        mPrevAction = new NotificationCompat.Action(R.drawable.ic_skip_previous_white_24dp, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(
                        mService, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));

        // Cancel all notifications to handle the case where the Service was killed and restarted by the system.
        mNotificationManager.cancelAll();
    }


    /**
     * Restituisce il notification manager
     * @return NotificationManager
     */
    public NotificationManager getNotificationManager() {
        return mNotificationManager;
    }


    /**
     * Restituisce la notifica con i dati da visualizzare
     * @param metadata Dati del file corrente
     * @param state Stato della riproduzione
     * @param token Token della sessione
     * @return Notifica da visualizzare
     */
    public Notification getNotification(MediaMetadataCompat metadata, @NonNull PlaybackStateCompat state, MediaSessionCompat.Token token) {
        boolean isPlaying = state.getState() == PlaybackStateCompat.STATE_PLAYING;
        MediaDescriptionCompat description = metadata.getDescription();
        NotificationCompat.Builder builder = buildNotification(state, token, isPlaying, description);
        return builder.build();
    }


    /**
     * Crea la notifica con i dati da visualizzare
     * @param state Stato della riproduzione
     * @param token Token della sessione
     * @param isPlaying True se è in fase di riproduzione. False se è in pausa
     * @param description Dati del file corrente
     * @return Notifica da visualizzare
     */
    private NotificationCompat.Builder buildNotification(@NonNull PlaybackStateCompat state, MediaSessionCompat.Token token,
                                                         boolean isPlaying, MediaDescriptionCompat description) {

        // Create the (mandatory) notification channel when running on Android Oreo.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mService, MUSICPLAYER_CHANNEL);
        builder.setStyle(
                new MediaStyle()
                        .setMediaSession(token)
                        .setShowActionsInCompactView(0, 1, 2)
                        // For backwards compatibility with Android L and earlier.
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(mService, PlaybackStateCompat.ACTION_STOP)))
                .setColor(ContextCompat.getColor(mService, R.color.colorAccent))
                .setSmallIcon(R.drawable.ic_status_bar_player)
                // Pending intent that is fired when user clicks on notification.
                .setContentIntent(createContentIntent())
                // Title - Usually Song name.
                .setContentTitle(description.getTitle())
                // Subtitle - Usually Artist name.
                .setContentText(description.getSubtitle())
                //.setLargeIcon(MusicLibrary.getAlbumBitmap(mService, description.getMediaId()))
                // When notification is deleted (when playback is paused and notification can be
                // deleted) fire MediaButtonPendingIntent with ACTION_STOP.
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(mService, PlaybackStateCompat.ACTION_STOP))
                // Show controls on lock screen even when user hides sensitive content.
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        // If skip to next action is enabled.
        if ((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
            builder.addAction(mPrevAction);
        }

        builder.addAction(isPlaying ? mPauseAction : mPlayAction);

        // If skip to prev action is enabled.
        if ((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
            builder.addAction(mNextAction);
        }

        return builder;
    }


    /**
     * Crea il channel della notifica
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
        if (mNotificationManager.getNotificationChannel(MUSICPLAYER_CHANNEL) == null) {
            NotificationChannel mChannel = new NotificationChannel(MUSICPLAYER_CHANNEL, "MediaSession", NotificationManager.IMPORTANCE_LOW);
            // Configure the notification channel.
            mChannel.setDescription("MediaSession and MediaPlayer");
            mNotificationManager.createNotificationChannel(mChannel);
        }
    }


    /**
     * Crea l'intent per visualizzare l'activity del player cliccando sulla notifica
     * @return Intent per visualizzare l'activity del player
     */
    private PendingIntent createContentIntent() {
        Intent openUI = new Intent(mService, ActivityMusicPlayer.class);
        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openUI.putExtra(KEY_BUNDLE_STARTED_FROM_NOTIFICATION, true);
        return PendingIntent.getActivity(mService, REQUEST_CODE, openUI, PendingIntent.FLAG_CANCEL_CURRENT);
    }

}