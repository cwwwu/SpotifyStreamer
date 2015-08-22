package com.wallacewu.spotifystreamer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Local broadcast receiver. This broadcast receiver is responsible for listening to changes to
 * the media player state (e.g., playing, paused, etc.) and invoking the appropriate callbacks
 * based on the change in state that occurred.
 */
public class AudioStateChangeReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = AudioStateChangeReceiver.class.getSimpleName();

    static public String ACTION_START_PLAYBACK = "ACTION_START_PLAYBACK";
    static public String ACTION_RESUME_PLAYBACK = "ACTION_RESUME_PLAYBACK";
    static public String ACTION_PAUSE_PLAYBACK = "ACTION_PAUSE_PLAYBACK";
    static public String ACTION_STOP_PLAYBACK = "ACTION_STOP_PLAYBACK";
    static public String ACTION_ONPREPARED_PLAYBACK = "ACTION_ONPREPARED_PLAYBACK";
    static public String ACTION_PREPARING_PLAYBACK = "ACTION_PREPARING_PLAYBACK";

    private Callback mCallback;

    public interface Callback {
        void onPreparedPlayback();
        void onStartPlayback();
        void onResumePlayback();
        void onPausePlayback();
        void onStopPlayback();
    }

    public AudioStateChangeReceiver(Callback callback) {
        super();
        mCallback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LOG_TAG, "Received action: " + intent.getAction());

        if (intent.getAction().equals(ACTION_START_PLAYBACK)) {
            mCallback.onStartPlayback();
        }

        if (intent.getAction().equals(ACTION_PAUSE_PLAYBACK)) {
            mCallback.onPausePlayback();
        }

        if (intent.getAction().equals(ACTION_STOP_PLAYBACK)) {
            mCallback.onStopPlayback();
        }

        if (intent.getAction().equals(ACTION_ONPREPARED_PLAYBACK)) {
            mCallback.onPreparedPlayback();
        }

        if (intent.getAction().equals(ACTION_RESUME_PLAYBACK)) {
            mCallback.onResumePlayback();
        }
    }
}
