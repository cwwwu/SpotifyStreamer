package com.wallacewu.spotifystreamer.audio;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.wallacewu.spotifystreamer.MainActivity;
import com.wallacewu.spotifystreamer.R;
import com.wallacewu.spotifystreamer.data.TrackInformation;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Wallace on 7/15/2015.
 */
public class AudioService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private final String LOG_TAG = AudioService.class.getSimpleName();

    private MediaPlayer mMediaPlayer;
    private ArrayList<TrackInformation> mTracks;
    private int mCurrentTrackIdx;
    private int mTrackTotalDurationMs;

    private final IBinder mAudioBind = new AudioBinder();

    static private final int STATE_STOPPED = 0;
    static private final int STATE_PREPARED = 1;
    static private final int STATE_PLAYING = 2;
    static private final int STATE_PAUSED = 3;
    private int mAudioState;

    @Override
    public void onCreate() {
        super.onCreate();
        mCurrentTrackIdx = 0;
        mAudioState = STATE_STOPPED;
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mTrackTotalDurationMs = mp.getDuration();
        updateAudioState(STATE_PREPARED);
        Log.d(LOG_TAG, "Total duration of track: " + mp.getDuration() + "ms");

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                0, new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pendingPreviousIntent = null;
        PendingIntent pendingPlayIntent = null;
        PendingIntent pendingNextIntent = null;
        Notification notification = new NotificationCompat.Builder(this)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentTitle("Title")
                .setTicker("Ticker")
                .setContentText("ContentText")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_media_previous, "Previous", pendingPreviousIntent)
                .addAction(android.R.drawable.ic_media_play, "Play", pendingPlayIntent)
                .addAction(android.R.drawable.ic_media_next, "Next", pendingNextIntent)
                .setStyle(new NotificationCompat.MediaStyle().setShowActionsInCompactView(1))
                .build();
        //notification.tickerText = "Ticker text";
        //notification.icon = android.R.drawable.ic_media_play;
        //notification.flags |= Notification.FLAG_ONGOING_EVENT;
        //notification.setLatestEventInfo(getApplicationContext(), "Sample", "Playing song", pendingIntent);
        startForeground(101, notification);

        playOrResumeTrack();
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
        stopForeground(true);
        playNextTrack();
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

    public class AudioBinder extends Binder {
        public AudioService getService() {
            return AudioService.this;
        }
    }

    private void broadcastAudioState(String state) {
        Intent intent = new Intent();
        intent.setAction(state);
        sendBroadcast(intent);
    }

    private void updateAudioState(int state) {
        switch (state) {
            case STATE_STOPPED:
                broadcastAudioState("ACTION_STOP_PLAYBACK");
                break;
            case STATE_PREPARED:
                broadcastAudioState("ACTION_ONPREPARED_PLAYBACK");
                break;
            case STATE_PLAYING:
                if (mAudioState == STATE_PAUSED) {
                    broadcastAudioState("ACTION_RESUME_PLAYBACK");
                } else {
                    broadcastAudioState("ACTION_START_PLAYBACK");
                }
                break;
            case STATE_PAUSED:
                broadcastAudioState("ACTION_PAUSE_PLAYBACK");
                break;
        }

        mAudioState = state;
    }

    public void prepareTrack(int trackIdx) {
        mCurrentTrackIdx = trackIdx;

        try {
            mMediaPlayer.setDataSource(mTracks.get(trackIdx).trackPreviewUrl);
        }
        catch (IOException e) {
            Log.e("REPLACE ME", "Error setting data source", e);
        }

        mMediaPlayer.prepareAsync();
    }

    public void setTrackInfoList(ArrayList<TrackInformation> tracks) {
        mTracks = tracks;
    }

    public int getCurrentTrackIdx() {
        return mCurrentTrackIdx;
    }

    public int getTrackTotalDuration() {
        return mTrackTotalDurationMs;
    }

    public int getCurrentPlaybackPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    public void seekTrack(int msec) {
        mMediaPlayer.seekTo(msec);
    }

    public void pausePlayback() {
        mMediaPlayer.pause();
        updateAudioState(STATE_PAUSED);
    }

    public void playOrResumeTrack() {
        mMediaPlayer.start();
        updateAudioState(STATE_PLAYING);
    }

    public boolean isAudioStreaming() {
        return mMediaPlayer.isPlaying();
    }

    public boolean isStopped() {
        return mAudioState == STATE_STOPPED;
    }

    public void playPreviousTrack() {
        mMediaPlayer.stop();
        mMediaPlayer.reset();

        if (--mCurrentTrackIdx < 0) {
            mCurrentTrackIdx = mTracks.size() - 1;
        }

        prepareTrack(mCurrentTrackIdx);
    }

    public void playNextTrack() {
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        mCurrentTrackIdx = (mCurrentTrackIdx + 1) % mTracks.size();

        prepareTrack(mCurrentTrackIdx);
    }
}
