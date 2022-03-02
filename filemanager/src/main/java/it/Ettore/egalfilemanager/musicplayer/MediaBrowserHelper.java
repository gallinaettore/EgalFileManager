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

import android.content.ComponentName;
import android.content.Context;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaControllerCompat.Callback;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;

/**
 * Helper class for a MediaBrowser that handles connecting, disconnecting, and basic browsing with simplified callbacks.
 */
public class MediaBrowserHelper {

    private static final String TAG = MediaBrowserHelper.class.getSimpleName();

    private final Context mContext;
    private final Class<? extends MediaBrowserServiceCompat> mMediaBrowserServiceClass;

    private final List<Callback> mCallbackList = new ArrayList<>();

    private final MediaBrowserConnectionCallback mMediaBrowserConnectionCallback;
    private final MediaControllerCallback mMediaControllerCallback;
    private final MediaBrowserSubscriptionCallback mMediaBrowserSubscriptionCallback;

    private MediaBrowserCompat mMediaBrowser;

    @Nullable
    private MediaControllerCompat mMediaController;


    /**
     *
     * @param context Context
     * @param serviceClass Classe del servizio che riproduce la musica
     */
    public MediaBrowserHelper(Context context, Class<? extends MediaBrowserServiceCompat> serviceClass) {
        mContext = context;
        mMediaBrowserServiceClass = serviceClass;

        mMediaBrowserConnectionCallback = new MediaBrowserConnectionCallback();
        mMediaControllerCallback = new MediaControllerCallback();
        mMediaBrowserSubscriptionCallback = new MediaBrowserSubscriptionCallback();
    }


    /**
     * Si connette al media browser. Chiamarlo nell'onStart dell'activity
     */
    public void onStart() {
        if (mMediaBrowser == null) {
            mMediaBrowser = new MediaBrowserCompat(mContext, new ComponentName(mContext, mMediaBrowserServiceClass), mMediaBrowserConnectionCallback, null);
            mMediaBrowser.connect();
        }
    }


    /**
     * Si disconnette dal media browser. Chiamarlo nell'onStop dell'activity
     */
    public void onStop() {
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mMediaControllerCallback);
            mMediaController = null;
        }
        if (mMediaBrowser != null && mMediaBrowser.isConnected()) {
            mMediaBrowser.disconnect();
            mMediaBrowser = null;
        }
        resetState();
    }



    /**
     * Called after connecting with a {@link MediaBrowserServiceCompat}.
     * <p>
     * Override to perform processing after a connection is established.
     *
     * @param mediaController {@link MediaControllerCompat} associated with the connected MediaSession.
     */
    protected void onConnected(@NonNull MediaControllerCompat mediaController) {
    }



    /**
     * Called after loading a browsable {@link MediaBrowserCompat.MediaItem}
     *
     * @param parentId The media ID of the parent item.
     * @param children List (possibly empty) of child items.
     */
    protected void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
    }


    /**
     * Called when the {@link MediaBrowserServiceCompat} connection is lost.
     */
    protected void onDisconnected() {
    }


    /**
     * Restituisce il media controller ottenuto dalla connessione
     * @return Media controller. Null a connessione non ancora avvenuta
     */
    public final MediaControllerCompat getMediaController() {
        return mMediaController;
    }


    /**
     * The internal state of the app needs to revert to what it looks like when it started before
     * any connections to the {@link MusicService} happens via the {@link MediaSessionCompat}.
     */
    private void resetState() {
        performOnAllCallbacks(new CallbackCommand() {
            @Override
            public void perform(@NonNull Callback callback) {
                callback.onPlaybackStateChanged(null);
            }
        });
    }


    /**
     * Restituisce il TransportControls per inviare comandi (start, stop...) al service
     * @return TransportControls. Null a connessione non ancora avvenuta
     */
    public MediaControllerCompat.TransportControls getTransportControls() {
        if (mMediaController == null) {
            throw new IllegalStateException("MediaController is null!");
        }
        return mMediaController.getTransportControls();
    }


    /**
     * Associa il callback creato nell'activity al media controller
     * @param callback Callback con le azioni da gestire nell'activity
     */
    public void registerCallback(Callback callback) {
        if (callback != null) {
            mCallbackList.add(callback);

            // Update with the latest metadata/playback state.
            if (mMediaController != null) {
                final MediaMetadataCompat metadata = mMediaController.getMetadata();
                if (metadata != null) {
                    callback.onMetadataChanged(metadata);
                }

                final PlaybackStateCompat playbackState = mMediaController.getPlaybackState();
                if (playbackState != null) {
                    callback.onPlaybackStateChanged(playbackState);
                }
            }
        }
    }


    /**
     * Esegue il comando in tutti i callback registrati
     * @param command Comando
     */
    private void performOnAllCallbacks(@NonNull CallbackCommand command) {
        for (Callback callback : mCallbackList) {
            if (callback != null) {
                command.perform(callback);
            }
        }
    }


    /**
     * Helper for more easily performing operations on all listening clients.
     */
    private interface CallbackCommand {
        void perform(@NonNull Callback callback);
    }


    // Receives callbacks from the MediaBrowser when it has successfully connected to the
    // MediaBrowserService (MusicService).
    private class MediaBrowserConnectionCallback extends MediaBrowserCompat.ConnectionCallback {

        // Happens as a result of onStart().
        @Override
        public void onConnected() {
            try {
                // Get a MediaController for the MediaSession.
                mMediaController = new MediaControllerCompat(mContext, mMediaBrowser.getSessionToken());
                mMediaController.registerCallback(mMediaControllerCallback);

                // Sync existing MediaSession state to the UI.
                mMediaControllerCallback.onMetadataChanged(mMediaController.getMetadata());
                mMediaControllerCallback.onPlaybackStateChanged(mMediaController.getPlaybackState());

                MediaBrowserHelper.this.onConnected(mMediaController);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }

            mMediaBrowser.subscribe(mMediaBrowser.getRoot(), mMediaBrowserSubscriptionCallback);
        }
    }


    // Receives callbacks from the MediaBrowser when the MediaBrowserService has loaded new media that is ready for playback.
    public class MediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback {

        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
            MediaBrowserHelper.this.onChildrenLoaded(parentId, children);
        }
    }


    // Receives callbacks from the MediaController and updates the UI state,
    // i.e.: Which is the current item, whether it's playing or paused, etc.
    private class MediaControllerCallback extends MediaControllerCompat.Callback {

        @Override
        public void onMetadataChanged(final MediaMetadataCompat metadata) {
            performOnAllCallbacks(new CallbackCommand() {
                @Override
                public void perform(@NonNull Callback callback) {
                    callback.onMetadataChanged(metadata);
                }
            });
        }

        @Override
        public void onPlaybackStateChanged(@Nullable final PlaybackStateCompat state) {
            performOnAllCallbacks(new CallbackCommand() {
                @Override
                public void perform(@NonNull Callback callback) {
                    callback.onPlaybackStateChanged(state);
                }
            });
        }

        // This might happen if the MusicService is killed while the Activity is in the
        // foreground and onStart() has been called (but not onStop()).
        @Override
        public void onSessionDestroyed() {
            resetState();
            onPlaybackStateChanged(null);

            MediaBrowserHelper.this.onDisconnected();
        }
    }
}