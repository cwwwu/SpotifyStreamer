package com.wallacewu.spotifystreamer;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.IOException;


/**
 * A placeholder fragment containing a simple view.
 */
public class MediaPlayerFragment extends Fragment {

    private MediaPlayer mMediaPlayer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_media_player, container, false);

        Intent intent   = getActivity().getIntent();

        String artistName = "Unknown artist";
        String albumName = "Unknown album";
        String trackName = "No track selected";
        String albumImageUrl = null;
        String trackPreviewUrl = null;

        if (intent != null) {
            TrackInformation trackInformation = intent.getParcelableExtra(TopTracksFragment.INTENT_EXTRA_TRACK_INFO);
            if (trackInformation != null) {
                albumName = trackInformation.albumName != null ? trackInformation.albumName : albumName;
                trackName = trackInformation.trackName != null ? trackInformation.trackName : trackName;
                albumImageUrl = trackInformation.albumImageUrl != null ? trackInformation.albumImageUrl : albumImageUrl;
                trackPreviewUrl = trackInformation.trackPreviewUrl != null ? trackInformation.trackPreviewUrl : trackPreviewUrl;
            }
            if (intent.hasExtra(TopTracksFragment.INTENT_EXTRA_ARTIST_NAME)) {
                artistName = intent.getStringExtra(TopTracksFragment.INTENT_EXTRA_ARTIST_NAME);
            }
        }

        TextView artistTextView = (TextView) rootView.findViewById(R.id.player_artist_name);
        artistTextView.setText(artistName);

        TextView albumTextView = (TextView) rootView.findViewById(R.id.player_album_name);
        albumTextView.setText(albumName);

        ImageView albumImageView = (ImageView) rootView.findViewById(R.id.player_album_image);
        if (albumImageUrl != null) {
            Picasso.with(getActivity()).load(albumImageUrl).into(albumImageView);
        } else {
            albumImageView.setImageResource(R.color.missing_thumbnail);
        }

        TextView trackTextView = (TextView) rootView.findViewById(R.id.player_track_name);
        trackTextView.setText(trackName);

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mMediaPlayer.setDataSource(trackPreviewUrl);
            mMediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mMediaPlayer.start();

        return rootView;
    }
}
