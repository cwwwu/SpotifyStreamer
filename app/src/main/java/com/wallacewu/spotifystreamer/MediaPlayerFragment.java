package com.wallacewu.spotifystreamer;

import android.support.v4.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.squareup.picasso.Picasso;
import com.wallacewu.spotifystreamer.audio.AudioService;
import com.wallacewu.spotifystreamer.data.TrackInformation;
import com.wallacewu.spotifystreamer.util.Utils;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * A placeholder fragment containing a simple view.
 */
public class MediaPlayerFragment extends DialogFragment implements AudioStateChangeReceiver.Callback {

    private static final long PROGRESS_UPDATE_INTERNAL = 1000;
    private static final long PROGRESS_UPDATE_INITIAL_INTERVAL = 100;

    private AudioService mAudioService;
    private Intent mPlayIntent;
    private boolean mAudioBound = false;
    private ServiceConnection mAudioConnection;

    private TextView mArtistTextView;
    private TextView mAlbumTextView;
    private ImageView mAlbumImageView;
    private TextView mTrackTextView;
    private SeekBar mSeekBar;
    private TextView mElapsedTimeView;
    private TextView mTotalTimeView;
    private ImageButton mPlayButton;

    private ArrayList<TrackInformation> mTrackList;
    private String mArtistName;
    private int mStartTrackIdx;

    private AudioStateChangeReceiver mAudioStateReceiver;

    private final ScheduledExecutorService mExecutorService = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> mScheduleFuture;
    private final Handler mHandler = new Handler();
    private final Runnable mUpdateProgressTask = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    public MediaPlayerFragment() {

    }

    private void updateProgress() {
        int currentPosition = mAudioService.getCurrentPlaybackPosition();
        mSeekBar.setProgress(currentPosition);
        mElapsedTimeView.setText(Utils.formatMillis(mAudioService.getCurrentPlaybackPosition()));
    }

    private void updatePlayerTrackInfo(int trackIdx) {
        if (trackIdx < 0 || trackIdx >= mTrackList.size())
            return;

        TrackInformation trackInformation = mTrackList.get(trackIdx);

        String albumName = getActivity().getString(R.string.unknown_album_name);
        String trackName = getActivity().getString(R.string.unknown_track_name);
        String albumImageUrl = null;

        albumName = trackInformation.albumName != null ? trackInformation.albumName : albumName;
        trackName = trackInformation.trackName != null ? trackInformation.trackName : trackName;
        albumImageUrl = trackInformation.albumImageUrl != null ? trackInformation.albumImageUrl : albumImageUrl;

        mAlbumTextView.setText(albumName);
        if (albumImageUrl != null) {
            Picasso.with(getActivity()).load(albumImageUrl).into(mAlbumImageView);
        } else {
            mAlbumImageView.setImageResource(R.color.missing_thumbnail);
        }
        mTrackTextView.setText(trackName);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_media_player, container, false);

        Bundle args = getArguments();

        mArtistName = getActivity().getString(R.string.unknown_artist_name);
        mTrackList = null;

        if (args != null) {
            mTrackList = args.getParcelableArrayList(TopTracksFragment.INTENT_EXTRA_TRACK_LIST);
            mArtistName = args.getString(TopTracksFragment.INTENT_EXTRA_ARTIST_NAME, mArtistName);
            mStartTrackIdx = args.getInt(TopTracksFragment.INTENT_EXTRA_TRACK_IDX, -1);
        }

        mArtistTextView = (TextView) rootView.findViewById(R.id.player_artist_name);
        mArtistTextView.setText(mArtistName);

        mAlbumTextView = (TextView) rootView.findViewById(R.id.player_album_name);
        mAlbumImageView = (ImageView) rootView.findViewById(R.id.player_album_image);
        mTrackTextView = (TextView) rootView.findViewById(R.id.player_track_name);

        mElapsedTimeView = (TextView) rootView.findViewById(R.id.player_current_time);
        mTotalTimeView = (TextView) rootView.findViewById(R.id.player_time_remaining);

        mElapsedTimeView.setText(getActivity().getString(R.string.time_placeholder));
        mTotalTimeView.setText(getActivity().getString(R.string.time_placeholder));


        ImageButton previousButton = (ImageButton) rootView.findViewById(R.id.player_button_prev);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAudioService.playPreviousTrack();
            }
        });
        ImageButton nextButton = (ImageButton) rootView.findViewById(R.id.player_button_next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAudioService.playNextTrack();
            }
        });
        mPlayButton = (ImageButton) rootView.findViewById(R.id.player_button_play);
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAudioService.isAudioStreaming()) {
                    mAudioService.pausePlayback();
                } else {
                    mAudioService.playOrResumeTrack();
                }
            }
        });

        mSeekBar = (SeekBar) rootView.findViewById(R.id.player_seek_bar);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopSeekbarUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mAudioService.seekTrack(seekBar.getProgress() * mAudioService.getTrackTotalDuration() / seekBar.getMax());
                scheduleSeekbarUpdate();
            }
        });

        mAudioConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                AudioService.AudioBinder binder = (AudioService.AudioBinder)service;
                mAudioService = binder.getService();

                mAudioService.setTrackInfoList(mTrackList);
                mAudioService.prepareTrack(mStartTrackIdx);

                mAudioBound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mAudioBound = false;
            }
        };

        mAudioStateReceiver = new AudioStateChangeReceiver(this);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("ACTION_START_PLAYBACK");
        intentFilter.addAction("ACTION_PAUSE_PLAYBACK");
        intentFilter.addAction("ACTION_STOP_PLAYBACK");
        intentFilter.addAction("ACTION_ONPREPARED_PLAYBACK");
        intentFilter.addAction("ACTION_RESUME_PLAYBACK");

        getActivity().registerReceiver(mAudioStateReceiver, intentFilter);

        if (mPlayIntent == null) {
            mPlayIntent = new Intent(getActivity(), AudioService.class);
            getActivity().bindService(mPlayIntent, mAudioConnection, Context.BIND_AUTO_CREATE);
            getActivity().startService(mPlayIntent);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        getActivity().unregisterReceiver(mAudioStateReceiver);

        getActivity().unbindService(mAudioConnection);
    }


    private void scheduleSeekbarUpdate() {
        stopSeekbarUpdate();
        if (!mExecutorService.isShutdown()) {
            mScheduleFuture = mExecutorService.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            mHandler.post(mUpdateProgressTask);
                        }
                    }, PROGRESS_UPDATE_INITIAL_INTERVAL,
                    PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS);
        }
    }

    private void stopSeekbarUpdate() {
        if (mScheduleFuture != null) {
            mScheduleFuture.cancel(false);
        }
    }

    @Override
    public void onPreparedPlayback() {
        updatePlayerTrackInfo(mAudioService.getCurrentTrackIdx());
        mSeekBar.setProgress(0);
    }

    @Override
    public void onStartPlayback() {
        mPlayButton.setImageDrawable(getActivity().getResources().getDrawable(android.R.drawable.ic_media_pause));
        mTotalTimeView.setText(Utils.formatMillis(mAudioService.getTrackTotalDuration()));
        mSeekBar.setMax(mAudioService.getTrackTotalDuration());
        scheduleSeekbarUpdate();
    }

    @Override
    public void onPausePlayback() {
        mPlayButton.setImageDrawable(getActivity().getResources().getDrawable(android.R.drawable.ic_media_play));
        stopSeekbarUpdate();
    }

    @Override
    public void onResumePlayback() {
        mPlayButton.setImageDrawable(getActivity().getResources().getDrawable(android.R.drawable.ic_media_pause));
        scheduleSeekbarUpdate();
    }

    @Override
    public void onStopPlayback() {

    }
}
