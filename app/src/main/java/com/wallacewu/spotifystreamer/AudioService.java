package com.wallacewu.spotifystreamer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.wallacewu.spotifystreamer.data.TrackInformation;

import java.io.IOException;
import java.util.ArrayList;

/**
 * This service's responsibility is to stream audio in the background.
 */
public class AudioService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private final String LOG_TAG = AudioService.class.getSimpleName();

    private MediaPlayer mMediaPlayer;

    private ArrayList<TrackInformation> mTracks;
    private String mArtistName;
    private int mCurrentTrackIdx;
    private int mTrackTotalDurationMs;

    static private final int NOTIFICATION_ID = 101;

    private final IBinder mAudioBind = new AudioBinder();

    static private final int STATE_UNINITIALIZED = 0;
    static private final int STATE_PREPARING = 1;
    static private final int STATE_PREPARED = 2;
    static private final int STATE_PLAYING = 3;
    static private final int STATE_PAUSED = 4;
    static private final int STATE_STOPPED = 5;

    private int mAudioState;

    static private final String ACTION_PLAY = "com.wallacewu.spotifystreamer.AudioService.ACTION_PLAY";
    static private final String ACTION_PAUSE = "com.wallacewu.spotifystreamer.AudioService.ACTION_PAUSE";
    static private final String ACTION_PREV = "com.wallacewu.spotifystreamer.AudioService.ACTION_PREV";
    static private final String ACTION_NEXT = "com.wallacewu.spotifystreamer.AudioService.ACTION_NEXT";
    static private final int REQUEST_CODE = 100;

    @Override
    public void onCreate() {
        mCurrentTrackIdx = 0;
        mAudioState = STATE_UNINITIALIZED;
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
        if (!Utils.shouldShowNotification(this))
            return;

        NotificationCompat.MediaStyle mediaStyle = new NotificationCompat.MediaStyle();

        TrackInformation trackInformation = mTracks.get(mCurrentTrackIdx);
        String imageUrl = trackInformation.albumImageUrl;
        Bitmap image = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentTitle(trackInformation.trackName)
                .setContentText(mArtistName)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(image)
                .setOngoing(true)
                .setShowWhen(false)
                .addAction(generateNotificationAction(android.R.drawable.ic_media_previous, getString(R.string.action_prev), ACTION_PREV))
                .addAction(playbackAction)
                .addAction(generateNotificationAction(android.R.drawable.ic_media_next, getString(R.string.action_next), ACTION_NEXT))
                .setStyle(mediaStyle);

        mediaStyle.setShowActionsInCompactView(0, 1, 2);

        loadImage(imageUrl, builder);

        startForeground(NOTIFICATION_ID, builder.build());
    }

    private void loadImage(String imageUrl, final NotificationCompat.Builder builder) {
        if (imageUrl == null)
            return;

        Picasso.with(this).load(imageUrl).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                builder.setLargeIcon(bitmap);
                startForeground(NOTIFICATION_ID, builder.build());
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
            }
        });
    }

    private void broadcastAudioState(String state) {
        Intent intent = new Intent();
        intent.setAction(state);
        sendBroadcast(intent);
    }

    private void updateAudioState(int state) {
        switch (state) {
            case STATE_UNINITIALIZED:
                broadcastAudioState(AudioStateChangeReceiver.ACTION_START_PLAYBACK);
                break;
            case STATE_STOPPED:
                broadcastAudioState(AudioStateChangeReceiver.ACTION_STOP_PLAYBACK);
                break;
            case STATE_PREPARING:
                broadcastAudioState(AudioStateChangeReceiver.ACTION_PREPARING_PLAYBACK);
                break;
            case STATE_PREPARED:
                broadcastAudioState(AudioStateChangeReceiver.ACTION_ONPREPARED_PLAYBACK);
                break;
            case STATE_PLAYING:
                if (mAudioState == STATE_PAUSED) {
                    broadcastAudioState(AudioStateChangeReceiver.ACTION_RESUME_PLAYBACK);
                } else {
                    broadcastAudioState(AudioStateChangeReceiver.ACTION_START_PLAYBACK);
                }
                break;
            case STATE_PAUSED:
                broadcastAudioState(AudioStateChangeReceiver.ACTION_PAUSE_PLAYBACK);
                break;
        }

        mAudioState = state;
    }

    public void prepareTrack(int trackIdx) {
        mMediaPlayer.reset();
        mCurrentTrackIdx = trackIdx;

        try {
            mMediaPlayer.setDataSource(mTracks.get(trackIdx).trackPreviewUrl);
        }
        catch (IOException e) {
            Log.e(LOG_TAG, "Error setting data source", e);
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
        if (isAudioTrackUrlValid())
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
        createNotification(generateNotificationAction(android.R.drawable.ic_media_play, getString(R.string.action_play), ACTION_PLAY));
        updateAudioState(STATE_PAUSED);
    }

    public void playOrResumeTrack() {
        mMediaPlayer.start();
        createNotification(generateNotificationAction(android.R.drawable.ic_media_pause, getString(R.string.action_pause), ACTION_PAUSE));
        updateAudioState(STATE_PLAYING);
    }

    public boolean isAudioStreaming() {
        return mAudioState == STATE_PLAYING;
    }

    public boolean isAudioTrackUrlValid() {
        return !(mAudioState == STATE_STOPPED || mAudioState == STATE_UNINITIALIZED);
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
