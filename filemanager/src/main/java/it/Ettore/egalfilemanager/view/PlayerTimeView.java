package it.Ettore.egalfilemanager.view;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import androidx.annotation.Nullable;
import it.Ettore.egalfilemanager.R;


/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


public class PlayerTimeView extends LinearLayout {
    private TextView currentTimeTextView, totalTimeTextView;
    private SeekBar seekBar;
    private MediaControllerCompat mMediaController;
    private ControllerCallback mControllerCallback;
    private boolean mIsTracking = false;
    private ValueAnimator mProgressAnimator;
    private int maxDuration;


    public PlayerTimeView(Context context) {
        super(context);
        init();
    }


    public PlayerTimeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    public PlayerTimeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    private void init(){
        final View view = LayoutInflater.from(getContext()).inflate(R.layout.player_time_view, null);
        final LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(param);
        addView(view);
        currentTimeTextView = view.findViewById(R.id.current_time_tv);
        totalTimeTextView = view.findViewById(R.id.total_time_tv);
        seekBar = view.findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsTracking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(mMediaController != null) {
                    mMediaController.getTransportControls().seekTo(seekBar.getProgress());
                }
                mIsTracking = false;
            }
        });
    }


    public void setMediaController(final MediaControllerCompat mediaController) {
        if (mediaController != null) {
            mControllerCallback = new ControllerCallback();
            mediaController.registerCallback(mControllerCallback);
        } else if (mMediaController != null) {
            mMediaController.unregisterCallback(mControllerCallback);
            mControllerCallback = null;
        }
        mMediaController = mediaController;
    }


    public void disconnectController() {
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mControllerCallback);
            mControllerCallback = null;
            mMediaController = null;
        }
    }


    private void setProgress(int millis){
        currentTimeTextView.setText(formatTime(millis));
        seekBar.setProgress(millis);
    }


    private void setMax(int max){
        seekBar.setMax(max);
        maxDuration = max;
        totalTimeTextView.setText(formatTime(max));
    }


    @SuppressLint("SimpleDateFormat")
    private String formatTime(long millis){
        final TimeZone tz = TimeZone.getTimeZone("UTC");
        SimpleDateFormat df;
        if(millis < 600000){
            //meno di 10 minuti
            df = new SimpleDateFormat("m:ss");
        } else if (millis >= 600000 && millis < 3600000){
            //più di 10 minuti ma meno di un'ora
            df = new SimpleDateFormat("mm:ss");
        } else {
            //più di un'ora
            df = new SimpleDateFormat("HH:mm:ss");
        }
        df.setTimeZone(tz);
        return df.format(new Date(millis));
    }


    /**
     * Aggiorna la view leggendo lo stato corrente del player
     */
    public void updateView(){
        mControllerCallback.onMetadataChanged(mMediaController.getMetadata());
        mControllerCallback.onPlaybackStateChanged(mMediaController.getPlaybackState());
    }





    private class ControllerCallback extends MediaControllerCompat.Callback implements ValueAnimator.AnimatorUpdateListener {

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);

            // If there's an ongoing animation, stop it now.
            if (mProgressAnimator != null) {
                mProgressAnimator.cancel();
                mProgressAnimator = null;
            }

            final int progress = state != null ? (int) state.getPosition() : 0;
            setProgress(progress);

            // If the media is playing then the seekbar should follow it, and the easiest
            // way to do that is to create a ValueAnimator to update it so the bar reaches
            // the end of the media the same time as playback gets there (or close enough).
            if (state != null && state.getState() == PlaybackStateCompat.STATE_PLAYING) {
                final int timeToEnd = (int) ((maxDuration - progress) / state.getPlaybackSpeed());
                if(timeToEnd > 0) { //in alcuni casi potrebbe essere 0
                    mProgressAnimator = ValueAnimator.ofInt(progress, maxDuration).setDuration(timeToEnd);
                    mProgressAnimator.setInterpolator(new LinearInterpolator());
                    mProgressAnimator.addUpdateListener(this);
                    mProgressAnimator.start();
                }
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);

            final int max = metadata != null ? (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) : 0;
            setProgress(0);
            setMax(max);
        }

        @Override
        public void onAnimationUpdate(final ValueAnimator valueAnimator) {
            // If the user is changing the slider, cancel the animation.
            if (mIsTracking) {
                valueAnimator.cancel();
                return;
            }

            final int animatedIntValue = (int) valueAnimator.getAnimatedValue();
            setProgress(animatedIntValue);
        }
    }
}
