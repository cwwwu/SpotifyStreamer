package com.wallacewu.spotifystreamer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
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
import com.wallacewu.spotifystreamer.model.TrackInformation;
import com.wallacewu.spotifystreamer.util.Utils;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import kaaes.spotify.webapi.android.models.Track;


/**
 * A placeholder fragment containing a simple view.
 */
public class MediaPlayerFragment extends Fragment {

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

    private ArrayList<TrackInformation> mTrackList;
    private String mArtistName;
    private int mStartTrackIdx;

    private final ScheduledExecutorService mExecutorService = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> mScheduleFuture;
    private final Handler mHandler = new Handler();
    private final Runnable mUpdateProgressTask = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    private void updateProgress() {
        int currentPosition = mAudioService.getCurrentPlaybackPosition();
        int trackDuration = mAudioService.getTrackTotalDuration();
        mSeekBar.setProgress(currentPosition * mSeekBar.getMax() / trackDuration);
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

        Intent intent   = getActivity().getIntent();

        mArtistName = getActivity().getString(R.string.unknown_artist_name);
        mTrackList = null;

        if (intent != null) {
            mTrackList = intent.getParcelableArrayListExtra(TopTracksFragment.INTENT_EXTRA_TRACK_LIST);

            if (intent.hasExtra(TopTracksFragment.INTENT_EXTRA_ARTIST_NAME)) {
                mArtistName = intent.getStringExtra(TopTracksFragment.INTENT_EXTRA_ARTIST_NAME);
            }

            if (intent.hasExtra(TopTracksFragment.INTENT_EXTRA_TRACK_IDX)) {
                mStartTrackIdx = intent.getIntExtra(TopTracksFragment.INTENT_EXTRA_TRACK_IDX, -1);
            }
        }

        mArtistTextView = (TextView) rootView.findViewById(R.id.player_artist_name);
        mArtistTextView.setText(mArtistName);

        mAlbumTextView = (TextView) rootView.findViewById(R.id.player_album_name);
        mAlbumImageView = (ImageView) rootView.findViewById(R.id.player_album_image);
        mTrackTextView = (TextView) rootView.findViewById(R.id.player_track_name);

        updatePlayerTrackInfo(mStartTrackIdx);

        mElapsedTimeView = (TextView) rootView.findViewById(R.id.player_current_time);
        mTotalTimeView = (TextView) rootView.findViewById(R.id.player_time_remaining);


        ImageButton previousButton = (ImageButton) rootView.findViewById(R.id.player_button_prev);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAudioService.playPreviousTrack();
                updatePlayerTrackInfo(mAudioService.getCurrentTrackIdx());
            }
        });
        ImageButton nextButton = (ImageButton) rootView.findViewById(R.id.player_button_next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAudioService.playNextTrack();
                updatePlayerTrackInfo(mAudioService.getCurrentTrackIdx());
            }
        });
        ToggleButton playButton = (ToggleButton) rootView.findViewById(R.id.player_button_play);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToggleButton button = (ToggleButton) v;
                if (button.isChecked()) {
                    mAudioService.pausePlayback();
                    stopSeekbarUpdate();
                } else {
                    mAudioService.playOrResumeTrack();
                    scheduleSeekbarUpdate();
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

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mAudioService.seekTrack(seekBar.getProgress() * mAudioService.getTrackTotalDuration() / seekBar.getMax());
            }
        });

        mAudioConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                AudioService.AudioBinder binder = (AudioService.AudioBinder)service;
                mAudioService = binder.getService();
                mAudioService.setTrackInfoList(mTrackList);
                mAudioService.prepareTrack(mStartTrackIdx);

                mTotalTimeView.setText(Utils.formatMillis(mAudioService.getTrackTotalDuration()));
                scheduleSeekbarUpdate();
                mAudioBound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mAudioBound = false;
            }
        };

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mPlayIntent == null) {
            mPlayIntent = new Intent(getActivity(), AudioService.class);
            getActivity().bindService(mPlayIntent, mAudioConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

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
}
