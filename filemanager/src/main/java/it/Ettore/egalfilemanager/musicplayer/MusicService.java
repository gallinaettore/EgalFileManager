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
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.media.MediaBrowserServiceCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.iconmanager.IconManager;


/**
 * Service per la riproduzione della musica in background
 */
public class MusicService extends MediaBrowserServiceCompat {
    public static final String CALLBACK_ACTION_MOVE_ITEM = "callback_action_move_item";
    public static final String CALLBACK_ACTION_REMOVE_ALL = "remove_all";
    public static final String CALLBACK_ACTION_ADD_ITEMS_FINISHED = "add_items_finished";
    public static final String KEY_BUNDLE_MOVE_ITEM_FROM_POSITION = "move_item_from_position";
    public static final String KEY_BUNDLE_MOVE_ITEM_TO_POSITION = "move_item_to_position";
    public static final String KEY_BUNDLE_NUM_ITEMS_ADDED = "num_items_added";

    private static final String TAG = MusicService.class.getSimpleName();

    private MediaSessionCompat mSession;
    private PlayerAdapter mPlayerAdapter;
    private MediaNotificationManager mMediaNotificationManager;
    private MediaSessionCallback mCallback;
    private boolean mServiceInStartedState;


    @Override
    public void onCreate() {
        super.onCreate();

        // Create a new MediaSession.
        mSession = new MediaSessionCompat(this, "MusicService");
        mCallback = new MediaSessionCallback();
        mSession.setCallback(mCallback);
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        setSessionToken(mSession.getSessionToken());

        //gestore delle notifiche
        mMediaNotificationManager = new MediaNotificationManager(this);
        //gestore del player
        mPlayerAdapter = new PlayerAdapter(this, new MediaPlayerListener());
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }


    @Override
    public void onDestroy() {
        mPlayerAdapter.stop();
        mSession.release();
    }


    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, Bundle rootHints) {
        if(TextUtils.equals(clientPackageName, getPackageName())) {
            return new BrowserRoot(getString(R.string.app_name), null); //Android Auto
        }
        return null;
    }


    @Override
    public void onLoadChildren(@NonNull final String parentMediaId, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(null);
    }






    /**
     * Classe che gestisce le azioni da eseguire inviate dall'activity o dalla notifica
     * (MediaSession Callback: Transport Controls -> MediaPlayerAdapter)
     */
    public class MediaSessionCallback extends MediaSessionCompat.Callback {
        private final List<MediaSessionCompat.QueueItem> mPlaylist = new ArrayList<>();
        private int mQueueIndex = -1;
        private MediaMetadataCompat mPreparedMedia;
        private boolean isShuffleMode;
        private int repeatMode = PlaybackStateCompat.REPEAT_MODE_NONE;
        private RandomTrack randomTrack = new RandomTrack();


        /**
         * Chiamato quando si aggiunge un item alla playlist
         * @param description MediaDescription con i dati del file (MediaId contiene il path del file)
         */
        @Override
        public void onAddQueueItem(MediaDescriptionCompat description) {
            mPlaylist.add(new MediaSessionCompat.QueueItem(description, description.hashCode()));
            randomTrack.addTrack();
            mQueueIndex = (mQueueIndex == -1) ? 0 : mQueueIndex;
            mSession.setQueue(mPlaylist);
        }


        /**
         * Chiamato quando si elimina un item dall playlist
         * @param description MediaDescription con i dati del file (MediaId contiene l'indice della posizione da cancellare)
         */
        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            int positionToDelete = Integer.parseInt(description.getMediaId());
            mPlaylist.remove(positionToDelete);
            randomTrack.removeTrack(positionToDelete);
            if(mPlaylist.isEmpty()){
                mQueueIndex = -1;
            } else if (positionToDelete <= mQueueIndex){
                mQueueIndex--;
            }
            mSession.setQueue(mPlaylist);
        }


        /**
         * Chiamato per preparare il file da riprodurre
         */
        @Override
        public void onPrepare() {
            if (mQueueIndex < 0 || mQueueIndex >= mPlaylist.size()) {
                // Nothing to play.
                return;
            }

            //ottengo i dati del file da riprodurre
            final String mediaId = mPlaylist.get(mQueueIndex).getDescription().getMediaId();
            mPreparedMedia = getMetadataForFile(mediaId, mQueueIndex);
            mSession.setMetadata(mPreparedMedia);

            if (!mSession.isActive()) {
                mSession.setActive(true);
            }
        }


        /**
         * Chiamato per avviare la riproduzione
         */
        @Override
        public void onPlay() {
            if (mPlaylist.isEmpty()) {
                // Nothing to play.
                return;
            }

            if (mPreparedMedia == null) {
                onPrepare();
            }

            mPlayerAdapter.playFromMedia(mPreparedMedia);
        }


        /**
         * Chiamato per mettere in pausa la riproduzione
         */
        @Override
        public void onPause() {
            mPlayerAdapter.pause();
        }


        /**
         * Chiamato per stoppare la riproduzione
         */
        @Override
        public void onStop() {
            mPlayerAdapter.stop();
            mSession.setActive(false);
        }


        /**
         * Chiamato per passare alla traccia successiva (alla pressione del tasto avanti sull'activity o sulla notifica)
         */
        @Override
        public void onSkipToNext() {
            if(mPlaylist.size() == 0){
                return;
            } else if (isShuffleMode){
                //mQueueIndex = new Random().nextInt(mPlaylist.size());
                if(repeatMode != PlaybackStateCompat.REPEAT_MODE_ONE){
                    int nextIndex = randomTrack.nextTrackIndex(repeatMode == PlaybackStateCompat.REPEAT_MODE_ALL);
                    if(nextIndex >= 0){
                        mQueueIndex = nextIndex;
                    } else {
                        return;
                    }
                }
            } else if(repeatMode == PlaybackStateCompat.REPEAT_MODE_NONE){
                //riproduce fino all'ultima traccia
                if(mQueueIndex < mPlaylist.size() - 1){
                    mQueueIndex++;
                } else {
                    return;
                }
            } else if (repeatMode == PlaybackStateCompat.REPEAT_MODE_ALL){
                //quando finisce comincia dall'inizio
                if(mQueueIndex < mPlaylist.size() - 1){
                    mQueueIndex++;
                } else {
                    mQueueIndex = 0;
                }
            }

            mPreparedMedia = null;
            onPlay();
        }


        /**
         * Chiamato per passare alla traccia precedente (alla pressione del tasto indietro sull'activity o sulla notifica)
         */
        @Override
        public void onSkipToPrevious() {
            if(mPlaylist.size() == 0){
                return;
            } else if (isShuffleMode){
                return; //in modalità shuffle non torna indietro
            } else if(repeatMode == PlaybackStateCompat.REPEAT_MODE_NONE){
                //riproduce fino all'inizio
                if(mQueueIndex > 0){
                    mQueueIndex--;
                } else {
                    return;
                }
            } else if (repeatMode == PlaybackStateCompat.REPEAT_MODE_ALL){
                //se all'inizio ritorna indietro all'ultima traccia
                if(mQueueIndex > 0){
                    mQueueIndex--;
                } else {
                    mQueueIndex = mPlaylist.size() - 1;
                }
            }

            mPreparedMedia = null;
            onPlay();
        }


        /**
         * Riproduce la traccia successiva quando il player ha terminato la traccia corrente
         */
        private void autoPlayNextTrack(){
            mPreparedMedia = null;

            if(mPlaylist.size() == 0){
                mQueueIndex = 0;
                onPause();
                return;
            } else if (isShuffleMode){
                if(repeatMode != PlaybackStateCompat.REPEAT_MODE_ONE){
                    int nextIndex = randomTrack.nextTrackIndex(repeatMode == PlaybackStateCompat.REPEAT_MODE_ALL);
                    if(nextIndex >= 0){
                        mQueueIndex = nextIndex;
                    } else {
                        mQueueIndex = 0; //se poi si preme di nuovo play parte dall'inizio
                        onPause();
                        return;
                    }
                }
            } else if(repeatMode == PlaybackStateCompat.REPEAT_MODE_NONE){
                //riproduce fino all'ultima traccia
                if(mQueueIndex < mPlaylist.size() - 1){
                    mQueueIndex++;
                } else {
                    mQueueIndex = 0; //se poi si preme di nuovo play parte dall'inizio
                    onPause();
                    return;
                }
            } else if (repeatMode == PlaybackStateCompat.REPEAT_MODE_ALL){
                //quando finisce comincia dall'inizio
                if(mQueueIndex < mPlaylist.size() - 1){
                    mQueueIndex++;
                } else {
                    mQueueIndex = 0;
                }
            }

            onPlay();
        }


        /**
         * Riproduce un file specifico. Usato quando nell'activity si clicca su un item della playlist
         * @param mediaId Path del file
         * @param extras Dati extra non utilizzati
         */
        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            int qIndex = -1;
            for(int i=0; i < mPlaylist.size(); i++){
                if(mPlaylist.get(i).getDescription().getMediaId().equals(mediaId)){
                    qIndex = i;
                }
            }
            if(qIndex == -1) return;
            mQueueIndex = qIndex;
            mPreparedMedia = null;
            onPlay();
        }


        /**
         * Chiamato quando si sposta la seekbar nell'activity
         * @param pos Nuova posizione
         */
        @Override
        public void onSeekTo(long pos) {
            mPlayerAdapter.seekTo(pos);
        }


        /**
         * Gestisce le azioni personalizzate
         * @param action Azione
         * @param extras Dati extra
         */
        @Override
        public void onCustomAction(String action, Bundle extras) {
            switch (action){
                case CALLBACK_ACTION_MOVE_ITEM:
                    //quando vengono spostati gli elementi all'interno della recyclerview
                    int fromPosition = extras.getInt(KEY_BUNDLE_MOVE_ITEM_FROM_POSITION);
                    int toPosition = extras.getInt(KEY_BUNDLE_MOVE_ITEM_TO_POSITION);
                    if(fromPosition < toPosition){
                        for(int i = fromPosition; i < toPosition; i++){
                            Collections.swap(mPlaylist, i, i+1);
                        }
                    } else {
                        for (int i = fromPosition; i > toPosition; i--){
                            Collections.swap(mPlaylist, i, i-1);
                        }
                    }
                    if(fromPosition < mQueueIndex && toPosition >= mQueueIndex){
                        mQueueIndex--;
                    } else if (fromPosition == mQueueIndex){
                        mQueueIndex = toPosition;
                    }
                    mSession.setQueue(mPlaylist);
                    break;
                case CALLBACK_ACTION_REMOVE_ALL:
                    //comando di eliminazione di tutti gli elementi
                    mPlaylist.clear();
                    mQueueIndex = -1;
                    mSession.setQueue(mPlaylist);
                    break;
                case CALLBACK_ACTION_ADD_ITEMS_FINISHED:
                    //chiamato quando si termina di aggiungere tutte le tracce
                    int numItemsAdded = extras.getInt(KEY_BUNDLE_NUM_ITEMS_ADDED, 0);
                    int previousPlayListSize = mPlaylist.size() - numItemsAdded;
                    if (mPlayerAdapter.getState() != PlaybackStateCompat.STATE_PLAYING && previousPlayListSize > 0){
                        mQueueIndex = previousPlayListSize >= 0 ? previousPlayListSize : 0;
                    }
                    break;
            }
        }


        /**
         * Chiamato quando l'activity passa un nuovo shuffle mode
         * @param shuffleMode ShuffleMode
         */
        @Override
        public void onSetShuffleMode(int shuffleMode) {
            this.isShuffleMode = shuffleMode != PlaybackStateCompat.SHUFFLE_MODE_NONE;
            mSession.setShuffleMode(shuffleMode);
            if(this.isShuffleMode){
                randomTrack.setCurrentTrackPlaying(mQueueIndex); //notifica che la traccia corrente è stata riprodotta e non deve essere estratta
            }
        }


        /**
         * Chimaato quando l'activity passa una nuova modalità di ripetizione
         * @param repeatMode repeatMode
         */
        @Override
        public void onSetRepeatMode(int repeatMode) {
            this.repeatMode = repeatMode;
            mSession.setRepeatMode(repeatMode);
        }
    }


    /**
     * Crea i MediaMetadati con tutti i dati estratti dal file
     * @param path Path del file
     * @param queueIndex Indice del file nella playlist
     * @return Metadati
     */
    private MediaMetadataCompat getMetadataForFile(String path, int queueIndex){
        //ottengo i dati del file: artista, titolo e immagine di copertina
        String artist = "", title = null;
        long duration = 0;
        final MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            final String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if(durationStr != null){
                duration = Long.parseLong(durationStr);
            }
        } catch (Exception e){
            Log.e(TAG, "Impossibile leggere i dati del file " + path);
            try {
                retriever.release();
            } catch (RuntimeException ignored) {}
        }
        final File file = new File(path);
        final Bitmap art = IconManager.getAudioPreview(file, this, 70);
        if(title == null){
            title = FileUtils.getFileNameWithoutExt(file);
        }

        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, path);
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration);
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, queueIndex);

        //Notification icon in card
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist);
        if(art != null) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, art);
            //metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        }

        //lock screen icon for pre lollipop
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, art);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, artist);

        return metadataBuilder.build();
    }







    /**
     * Classe che gestisce il listener del MediaPlayer
     * (MediaPlayerAdapter Callback: MediaPlayerAdapter state -> MusicService.)
     */
    public class MediaPlayerListener extends PlaybackInfoListener {

        private final ServiceManager mServiceManager;

        MediaPlayerListener() {
            mServiceManager = new ServiceManager();
        }


        /**
         * Chiamato quando il player cambia il suo stato
         * @param state Stato del player
         */
        @Override
        public void onPlaybackStateChange(PlaybackStateCompat state) {
            // Report the state to the MediaSession.
            mSession.setPlaybackState(state);

            // Manage the started state of this service.
            switch (state.getState()) {
                case PlaybackStateCompat.STATE_PLAYING:
                    mServiceManager.moveServiceToStartedState(state);
                    break;
                case PlaybackStateCompat.STATE_PAUSED:
                    mServiceManager.updateNotificationForPause(state);
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    mServiceManager.moveServiceOutOfStartedState(state);
                    break;
            }
        }


        /**
         * Chiamato quando il player finisce l'esecuzione della traccia corrente
         */
        @Override
        public void onPlaybackCompleted() {
            mCallback.autoPlayNextTrack(); //riproduce la traccia successiva
        }



        /**
         * Classe di utilità che gestisce lo stato del service (avviato, in pausa..) e le notifiche
         */
        class ServiceManager {

            /**
             * Chiamato quando lo stato del player è in riproduzione
             * @param state Stato
             */
            private void moveServiceToStartedState(PlaybackStateCompat state) {
                Notification notification = mMediaNotificationManager.getNotification(mPlayerAdapter.getCurrentMedia(), state, getSessionToken());
                if (!mServiceInStartedState) {
                    ContextCompat.startForegroundService(MusicService.this, new Intent(MusicService.this, MusicService.class));
                    mServiceInStartedState = true;
                }
                startForeground(MediaNotificationManager.NOTIFICATION_ID, notification);
            }


            /**
             * Chiamato quando lo stato del player è in pausa
             * @param state Stato
             */
            private void updateNotificationForPause(PlaybackStateCompat state) {
                stopForeground(false);
                if(mPlayerAdapter.getCurrentMedia() != null) {
                    Notification notification = mMediaNotificationManager.getNotification(mPlayerAdapter.getCurrentMedia(), state, getSessionToken());
                    mMediaNotificationManager.getNotificationManager().notify(MediaNotificationManager.NOTIFICATION_ID, notification);
                }
            }


            /**
             * Chiamato quando lo stato del player è in stop
             * @param state State
             */
            private void moveServiceOutOfStartedState(PlaybackStateCompat state) {
                stopForeground(true);
                stopSelf();
                mServiceInStartedState = false;
            }
        }

    }

}