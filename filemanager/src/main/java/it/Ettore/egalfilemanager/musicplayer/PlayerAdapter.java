package it.Ettore.egalfilemanager.musicplayer;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;


/**
 * Player implementation that handles playing music with proper handling of headphones and audio focus.
 */
class PlayerAdapter {

    private static final String TAG = PlayerAdapter.class.getSimpleName();

    private static final float MEDIA_VOLUME_DEFAULT = 1.0f;
    private static final float MEDIA_VOLUME_DUCK = 0.2f;

    private static final IntentFilter AUDIO_NOISY_INTENT_FILTER = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private boolean mAudioNoisyReceiverRegistered = false;
    private final BroadcastReceiver mAudioNoisyReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                        if (isPlaying()) {
                            pause();
                        }
                    }
                }
            };

    private final Context mApplicationContext;
    private final AudioManager mAudioManager;
    private final AudioFocusHelper mAudioFocusHelper;
    private boolean mPlayOnAudioFocus = false;
    private MediaPlayer mMediaPlayer;
    private String mFilePath;
    private PlaybackInfoListener mPlaybackInfoListener;
    private MediaMetadataCompat mCurrentMedia;
    private int mState;
    private boolean mCurrentMediaPlayedToCompletion;

    // Work-around for a MediaPlayer bug related to the behavior of MediaPlayer.seekTo() while not playing.
    private int mSeekWhileNotPlaying = -1;



    PlayerAdapter(@NonNull Context context, PlaybackInfoListener listener) {
        mApplicationContext = context.getApplicationContext();
        mAudioManager = (AudioManager) mApplicationContext.getSystemService(Context.AUDIO_SERVICE);
        mAudioFocusHelper = new AudioFocusHelper();
        mPlaybackInfoListener = listener;
    }


    /**
     * Once the {@link MediaPlayer} is released, it can't be used again, and another one has to be created.
     * In the onStop() method of the Activity the {@link MediaPlayer} is released.
     * Then in the onStart() of the Activity a new {@link MediaPlayer} object has to be created.
     * That's why this method is private, and called by load(int) and not the constructor.
     */
    private void initializeMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    // Set the state to "paused" because it most closely matches the state in MediaPlayer with regards to available state transitions compared to "stop".
                    // Paused allows: seekTo(), start(), pause(), stop()
                    // Stop allows: stop()
                    setNewState(PlaybackStateCompat.STATE_PLAYING); //non lo metto in stato di pausa perchè deve passare alla traccia successiva di default
                    //eseguo il listener
                    mPlaybackInfoListener.onPlaybackCompleted();
                }
            });
        }
    }


    /**
     * Restituisce i dati del file corrente in riproduzione
     * @return Metadata
     */
    public MediaMetadataCompat getCurrentMedia() {
        return mCurrentMedia;
    }


    /**
     * Avvia la riproduzione del file
     * @param metadata Metadati con i dati del file da riprodurre
     */
    public void playFromMedia(MediaMetadataCompat metadata) {
        mCurrentMedia = metadata;
        final String mediaId = metadata.getDescription().getMediaId();
        playFile(mediaId);
    }


    /**
     * Inizializza il player e riproduce il file. Se viene passato come parametro lo stesso file non esegue nessuna operazione
     * @param filePath Path del file da riprodurre
     */
    private void playFile(String filePath) {
        boolean mediaChanged = (mFilePath == null || !filePath.equals(mFilePath));
        if (mCurrentMediaPlayedToCompletion) {
            // Last audio file was played to completion, the resourceId hasn't changed, but the
            // player was released, so force a reload of the media file for playback.
            mediaChanged = true;
            mCurrentMediaPlayedToCompletion = false;
        }
        if (!mediaChanged) {
            if (!isPlaying()) {
                play();
            }
            return;
        } else {
            release();
        }

        mFilePath = filePath;

        initializeMediaPlayer();

        try {
            mMediaPlayer.setDataSource(mFilePath);
        } catch (Exception e) {
            Log.w(TAG, "Failed to open file: " + mFilePath);
            setNewState(PlaybackStateCompat.STATE_ERROR);
            return;
        }

        try {
            mMediaPlayer.prepare();
        } catch (Exception e) {
            Log.w(TAG, "Failed to open file: " + mFilePath);
            setNewState(PlaybackStateCompat.STATE_ERROR);
            return;
        }

        play();
    }


    /**
     * Avvia la riproduzione
     */
    private void play() {
        if (mAudioFocusHelper.requestAudioFocus()) {
            registerAudioNoisyReceiver();
            // Called when media is ready to be played and indicates the app has audio focus.
            if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
                mMediaPlayer.start();
                setNewState(PlaybackStateCompat.STATE_PLAYING);
            }
        }
    }


    /**
     * Interrompe la riporduzione
     */
    public final void stop() {
        mAudioFocusHelper.abandonAudioFocus();
        unregisterAudioNoisyReceiver();
        // Regardless of whether or not the MediaPlayer has been created / started, the state must
        // be updated, so that MediaNotificationManager can take down the notification.
        setNewState(PlaybackStateCompat.STATE_STOPPED);
        // The player should clean up resources at this point.
        release();
    }


    /**
     * Mette in pausa la riproduzione
     */
    public final void pause() {
        if (!mPlayOnAudioFocus) {
            mAudioFocusHelper.abandonAudioFocus();
        }

        unregisterAudioNoisyReceiver();

        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
        setNewState(PlaybackStateCompat.STATE_PAUSED);
    }


    /**
     * Sposta la riproduzione alla posizione passata
     * @param position Posizione della riproduzione
     */
    public void seekTo(long position) {
        if (mMediaPlayer != null) {
            if (!mMediaPlayer.isPlaying()) {
                mSeekWhileNotPlaying = (int) position;
            }
            mMediaPlayer.seekTo((int) position);

            // Set the state (to the current state) because the position changed and should be reported to clients.
            setNewState(mState);
        }
    }


    /**
     * Rilascia le risorse
     */
    private void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }


    /**
     * Restituisce lo stato della riproduzione del player
     * @return True se il player è in riproduzione.
     */
    private boolean isPlaying() {
        return mMediaPlayer != null && mMediaPlayer.isPlaying();
    }



    /**
     * Imposta il nuovo stato del player e lo notifica al listener
     * @param newPlayerState Nuovo stato
     */
    private void setNewState(@PlaybackStateCompat.State int newPlayerState) {
        mState = newPlayerState;

        // Whether playback goes to completion, or whether it is stopped, the mCurrentMediaPlayedToCompletion is set to true.
        if (mState == PlaybackStateCompat.STATE_STOPPED) {
            mCurrentMediaPlayedToCompletion = true;
        }

        // Work around for MediaPlayer.getCurrentPosition() when it changes while not playing.
        final long reportPosition;
        if (mSeekWhileNotPlaying >= 0) {
            reportPosition = mSeekWhileNotPlaying;

            if (mState == PlaybackStateCompat.STATE_PLAYING) {
                mSeekWhileNotPlaying = -1;
            }
        } else {
            reportPosition = mMediaPlayer == null ? 0 : mMediaPlayer.getCurrentPosition();
        }

        final PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setActions(getAvailableActions());
        stateBuilder.setState(mState, reportPosition, 1.0f, SystemClock.elapsedRealtime());
        mPlaybackInfoListener.onPlaybackStateChange(stateBuilder.build());
    }


    /**
     * Set the current capabilities available on this session. Note: If a capability is not
     * listed in the bitmask of capabilities then the MediaSession will not handle it. For
     * example, if you don't want ACTION_STOP to be handled by the MediaSession, then don't
     * included it in the bitmask that's returned.
     */
    @PlaybackStateCompat.Actions
    private long getAvailableActions() {
        long actions = PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        switch (mState) {
            case PlaybackStateCompat.STATE_STOPPED:
                actions |= PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE;
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                actions |= PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SEEK_TO;
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                actions |= PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_STOP;
                break;
            default:
                actions |= PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP
                        | PlaybackStateCompat.ACTION_PAUSE;
        }
        return actions;
    }


    /**
     * Imposta il volume del player
     * @param volume Volume
     */
    private void setVolume(float volume) {
        if (mMediaPlayer != null) {
            mMediaPlayer.setVolume(volume, volume);
        }
    }


    public int getState() {
        return mState;
    }


    private void registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            mApplicationContext.registerReceiver(mAudioNoisyReceiver, AUDIO_NOISY_INTENT_FILTER);
            mAudioNoisyReceiverRegistered = true;
        }
    }


    private void unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            mApplicationContext.unregisterReceiver(mAudioNoisyReceiver);
            mAudioNoisyReceiverRegistered = false;
        }
    }





    /**
     * Helper class for managing audio focus related tasks.
     */
    private final class AudioFocusHelper implements AudioManager.OnAudioFocusChangeListener {

        private boolean requestAudioFocus() {
            final int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }

        private void abandonAudioFocus() {
            mAudioManager.abandonAudioFocus(this);
        }


        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (mPlayOnAudioFocus && !isPlaying()) {
                        play();
                    } else if (isPlaying()) {
                        setVolume(MEDIA_VOLUME_DEFAULT);
                    }
                    mPlayOnAudioFocus = false;
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    setVolume(MEDIA_VOLUME_DUCK);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if (isPlaying()) {
                        mPlayOnAudioFocus = true;
                        pause();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    mAudioManager.abandonAudioFocus(this);
                    mPlayOnAudioFocus = false;
                    stop();
                    break;
            }
        }
    }

}
