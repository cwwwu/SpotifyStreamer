package com.wallacewu.spotifystreamer;

import android.support.v4.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.wallacewu.spotifystreamer.data.TrackInformation;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * The fragment containing the audio player.
 */
public class MediaPlayerFragment extends DialogFragment implements AudioStateChangeReceiver.Callback {

    private static final long PROGRESS_UPDATE_INTERNAL = 100;
    private static final long PROGRESS_UPDATE_INITIAL_INTERVAL = 100;
    private static final String BUNDLE_CURRENT_TRACK_IDX = "BUNDLE_CURRENT_TRACK_IDX";
    private static final String BUNDLE_TRACK_LIST = "BUNDLE_TRACK_LIST";
    private static final String BUNDLE_ARTIST_NAME = "BUNDLE_ARTIST_NAME";

    private AudioService mAudioService;
    private Intent mPlayIntent;
    IntentFilter mPlaybackIntentFilter;
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

    private ShareActionProvider mShareActionProvider;

    private ArrayList<TrackInformation> mTrackList;
    private String mArtistName;
    private int mTrackIdx;

    private AudioStateChangeReceiver mAudioStateReceiver;

    // The code here for updating the seek bar was heavily inspired by the player UI code in this project:
    // https://github.com/googlesamples/android-UniversalMusicPlayer
    private final ScheduledExecutorService mExecutorService = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> mScheduleFuture;
    private final Handler mHandler = new Handler();
    private final Runnable mUpdateProgressTask = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

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

    public MediaPlayerFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_media_player, container, false);

        Bundle args = getArguments();

        mArtistName = getActivity().getString(R.string.unknown_artist_name);

        // We either populate the view based on the savedInstanceState bundle (on orientation)
        // or when the user has chosen a track to start playing
        if (savedInstanceState != null) {
            mTrackIdx = savedInstanceState.getInt(BUNDLE_CURRENT_TRACK_IDX, -1);
            mTrackList = savedInstanceState.getParcelableArrayList(BUNDLE_TRACK_LIST);
            mArtistName = savedInstanceState.getString(BUNDLE_ARTIST_NAME);
        } else if (args != null) {
            mTrackList = args.getParcelableArrayList(TopTracksFragment.INTENT_EXTRA_TRACK_LIST);
            mArtistName = args.getString(TopTracksFragment.INTENT_EXTRA_ARTIST_NAME, mArtistName);
            mTrackIdx = args.getInt(TopTracksFragment.INTENT_EXTRA_TRACK_IDX, -1);
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
                AudioService.AudioBinder binder = (AudioService.AudioBinder) service;
                mAudioService = binder.getService();

                // When we're connected to the audio streaming service, we may already be
                // preparing/streaming audio. In this case, we will see if we need to update
                // the track. This is to handle the case where the user selects a new track from
                // the top tracks list (view) and the player is rebinding to the existing service.
                if (mAudioService.isAudioTrackUrlValid()) {
                    if (!mAudioService.getCurrentTrackUrl().equals(mTrackList.get(mTrackIdx).trackPreviewUrl)) {
                        mAudioService.setTrackInfoList(mTrackList);
                        mAudioService.setArtistName(mArtistName);
                        mAudioService.prepareTrack(mTrackIdx);
                    }

                    mSeekBar.setMax(mAudioService.getTrackTotalDuration());
                    updatePlayerTrackInfo(mAudioService.getCurrentTrackIdx());
                    mTotalTimeView.setText(Utils.formatTimestamp(mAudioService.getTrackTotalDuration()));
                    scheduleSeekbarUpdate();
                } else {
                    mAudioService.setTrackInfoList(mTrackList);
                    mAudioService.setArtistName(mArtistName);
                    mAudioService.prepareTrack(mTrackIdx);
                }

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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mPlaybackIntentFilter = new IntentFilter();
        mPlaybackIntentFilter.addAction(AudioStateChangeReceiver.ACTION_START_PLAYBACK);
        mPlaybackIntentFilter.addAction(AudioStateChangeReceiver.ACTION_PAUSE_PLAYBACK);
        mPlaybackIntentFilter.addAction(AudioStateChangeReceiver.ACTION_STOP_PLAYBACK);
        mPlaybackIntentFilter.addAction(AudioStateChangeReceiver.ACTION_ONPREPARED_PLAYBACK);
        mPlaybackIntentFilter.addAction(AudioStateChangeReceiver.ACTION_RESUME_PLAYBACK);

        if (mPlayIntent == null) {
            mPlayIntent = new Intent(getActivity(), AudioService.class);
            getActivity().startService(mPlayIntent);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        getActivity().bindService(mPlayIntent, mAudioConnection, Context.BIND_AUTO_CREATE);
        getActivity().registerReceiver(mAudioStateReceiver, mPlaybackIntentFilter);

        if (mAudioBound && mAudioService.isAudioStreaming()) {
            updatePlayerTrackInfo(mAudioService.getCurrentTrackIdx());
            scheduleSeekbarUpdate();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        stopSeekbarUpdate();
        getActivity().unbindService(mAudioConnection);
        getActivity().unregisterReceiver(mAudioStateReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        int idx = (mAudioBound) ? mAudioService.getCurrentTrackIdx() : -1;
        outState.putInt(BUNDLE_CURRENT_TRACK_IDX, idx);
        outState.putParcelableArrayList(BUNDLE_TRACK_LIST, mTrackList);
        outState.putString(BUNDLE_ARTIST_NAME, mArtistName);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (getActivity().isFinishing()) {
            getActivity().stopService(mPlayIntent);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.mediaplayerfragment, menu);

        MenuItem menuItem = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        updateShareTrackIntent();
    }

    private void updateShareTrackIntent() {
        if (!mAudioBound)
            return;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mAudioService.getCurrentTrackUrl());
        mShareActionProvider.setShareIntent(shareIntent);
    }

    private void updateProgress() {
        int currentPosition = mAudioService.getCurrentPlaybackPosition();
        mSeekBar.setProgress(currentPosition);
        mElapsedTimeView.setText(Utils.formatTimestamp(mAudioService.getCurrentPlaybackPosition()));
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

    private void stopSeekbarUpdate() {
        if (mScheduleFuture != null) {
            mScheduleFuture.cancel(false);
        }
    }

    @Override
    public void onPreparedPlayback() {
        updatePlayerTrackInfo(mAudioService.getCurrentTrackIdx());
        mSeekBar.setProgress(0);
        updateShareTrackIntent();
    }

    @Override
    public void onStartPlayback() {
        mPlayButton.setImageDrawable(getActivity().getResources().getDrawable(android.R.drawable.ic_media_pause));
        mTotalTimeView.setText(Utils.formatTimestamp(mAudioService.getTrackTotalDuration()));
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
        mPlayButton.setImageDrawable(getActivity().getResources().getDrawable(android.R.drawable.ic_media_play));
        stopSeekbarUpdate();
        mSeekBar.setProgress(0);
    }
}
