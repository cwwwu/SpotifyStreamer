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
    private String mArtistName;
    private int mCurrentTrackIdx;
    private int mTrackTotalDurationMs;

    private final IBinder mAudioBind = new AudioBinder();

    static private final int STATE_STOPPED = 0;
    static private final int STATE_PREPARED = 1;
    static private final int STATE_PLAYING = 2;
    static private final int STATE_PAUSED = 3;
    private int mAudioState;

    static private final String ACTION_PLAY = "com.wallacewu.spotifystreamer.audio.AudioService.ACTION_PLAY";
    static private final String ACTION_PAUSE = "com.wallacewu.spotifystreamer.audio.AudioService.ACTION_PAUSE";
    static private final String ACTION_PREV = "com.wallacewu.spotifystreamer.audio.AudioService.ACTION_PREV";
    static private final String ACTION_NEXT = "com.wallacewu.spotifystreamer.audio.AudioService.ACTION_NEXT";
    static private final int REQUEST_CODE = 100;

    @Override
    public void onCreate() {
        mCurrentTrackIdx = 0;
        mAudioState = STATE_STOPPED;
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        super.onCreate();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mTrackTotalDurationMs = mp.getDuration();
        updateAudioState(STATE_PREPARED);
        Log.d(LOG_TAG, "Total duration of track: " + mp.getDuration() + "ms");

        playOrResumeTrack();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAudioBind;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntentAction(intent);

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

    private boolean handleIntentAction(Intent intent) {
        if (intent == null || intent.getAction() == null)
            return false;

        String action = intent.getAction();
        if (action.equals(ACTION_PAUSE)) {
            pausePlayback();
            return true;
        } else if (action.equals(ACTION_PLAY)) {
            playOrResumeTrack();
            return true;
        } else if (action.equals(ACTION_PREV)) {
            playPreviousTrack();
            return true;
        } else if (action.equals(ACTION_NEXT)) {
            playNextTrack();
            return true;
        }

        return false;
    }

    private NotificationCompat.Action generateNotificationAction(int drawableResId, String title, String intentAction) {
        Intent intent = new Intent(getApplicationContext(), AudioService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), REQUEST_CODE, intent, 0);
        return new NotificationCompat.Action.Builder(drawableResId, title, pendingIntent).build();
    }

    private void createNotification(NotificationCompat.Action playbackAction) {
        NotificationCompat.MediaStyle mediaStyle = new NotificationCompat.MediaStyle();

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                0, new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentTitle(mTracks.get(mCurrentTrackIdx).trackName)
                .setContentText(mArtistName)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .addAction(generateNotificationAction(android.R.drawable.ic_media_previous, "Previous", ACTION_PREV))
                .addAction(playbackAction)
                .addAction(generateNotificationAction(android.R.drawable.ic_media_next, "Next", ACTION_NEXT))
                .setStyle(mediaStyle);

        mediaStyle.setShowActionsInCompactView(1, 2);

        startForeground(101, builder.build());
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

    public void setArtistName(String artistName) {
        mArtistName = artistName;
    }

    public int getCurrentTrackIdx() {
        return mCurrentTrackIdx;
    }

    public String getCurrentTrackUrl() {
        if (isAudioStreaming())
            return mTracks.get(mCurrentTrackIdx).trackPreviewUrl;

        return null;
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
        createNotification(generateNotificationAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY));
        updateAudioState(STATE_PAUSED);
    }

    public void playOrResumeTrack() {
        mMediaPlayer.start();
        createNotification(generateNotificationAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
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
