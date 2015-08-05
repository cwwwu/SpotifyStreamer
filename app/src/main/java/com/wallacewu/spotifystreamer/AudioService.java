package com.wallacewu.spotifystreamer;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Wallace on 7/15/2015.
 */
public class AudioService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    private MediaPlayer mMediaPlayer;
//    private ArrayList<TrackInformation> mTracks;
    private String mTracks;
    private int mCurrentPlayPosition;

    private final IBinder mAudioBind = new AudioBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        mCurrentPlayPosition = 0;
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAudioBind;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mMediaPlayer.stop();
        mMediaPlayer.release();
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onDestroy() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }
    }

    public void setTrackInfoList(String tracks) {
        mTracks = tracks;
        playTrack();
    }

    public void setTrack(int trackIndex) {
        mCurrentPlayPosition = trackIndex;
    }

    public class AudioBinder extends Binder {
        AudioService getService() {
            return AudioService.this;
        }
    }

    public void playTrack() {
        try {
            mMediaPlayer.setDataSource(mTracks);
        }
        catch (IOException e) {
            Log.e("REPLACE ME", "Error setting data source", e);
        }
        mMediaPlayer.prepareAsync();
    }
}
