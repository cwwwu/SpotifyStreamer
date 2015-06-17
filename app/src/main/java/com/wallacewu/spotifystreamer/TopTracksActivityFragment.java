package com.wallacewu.spotifystreamer;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.http.QueryMap;


/**
 * A placeholder fragment containing a simple view.
 */
public class TopTracksActivityFragment extends Fragment {

    private String artistId;
    private TrackAdapter mTracksAdapter;

    public TopTracksActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_top_tracks, container, false);

        Intent intent = getActivity().getIntent();
        if (intent != null) {
            if (intent.hasExtra(ArtistSearchActivityFragment.INTENT_EXTRA_ARTIST_NAME)) {
                ((ActionBarActivity)getActivity())
                        .getSupportActionBar()
                        .setSubtitle(intent.getStringExtra(ArtistSearchActivityFragment.INTENT_EXTRA_ARTIST_NAME));
            }

            if (intent.hasExtra(ArtistSearchActivityFragment.INTENT_EXTRA_ARTIST_ID)) {
                artistId = intent.getStringExtra(ArtistSearchActivityFragment.INTENT_EXTRA_ARTIST_ID);
            }
        }

        mTracksAdapter = new TrackAdapter(getActivity(),
                R.layout.list_item_artist,
                new ArrayList<TrackInformation>());
        ListView trackListView = (ListView) rootView.findViewById(R.id.top_ten_tracks_list);
        trackListView.setAdapter(mTracksAdapter);

        if (intent != null && intent.hasExtra(ArtistSearchActivityFragment.INTENT_EXTRA_ARTIST_ID)) {
            FetchTrackTask fetchTrackTask = new FetchTrackTask();
            fetchTrackTask.execute(intent.getStringExtra(ArtistSearchActivityFragment.INTENT_EXTRA_ARTIST_ID));
        }

        return rootView;
    }

    class TrackInformation {
        public String   albumName;
        public String   trackName;
        public Image    albumImage;

        TrackInformation(String albumName, String trackName, Image albumImage) {
            this.albumName = albumName;
            this.trackName = trackName;
            this.albumImage = albumImage;
        }
    }

    class TrackAdapter extends ArrayAdapter<TrackInformation> {

        public TrackAdapter(Context context, int resource, ArrayList<TrackInformation> tracks) {
            super(context, resource, tracks);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            TrackInformation track = getItem(position);

            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.list_item_track, parent, false);
            }

            TextView albumTextView = (TextView) view.findViewById(R.id.album_name);
            TextView trackTextView = (TextView) view.findViewById(R.id.track_name);
            ImageView albumImageView = (ImageView) view.findViewById(R.id.album_image_thumbnail);

            albumTextView.setText(track.albumName);
            trackTextView.setText(track.trackName);
            if (track.albumImage != null) {
                Picasso.with(getContext()).load(track.albumImage.url).into(albumImageView);
            } else {
                albumImageView.setImageResource(android.R.color.transparent);
            }

            return view;
        }
    }

    class FetchTrackTask extends AsyncTask<String, Void, ArrayList<TrackInformation>> {

        private final String LOG_TAG = FetchTrackTask.class.getSimpleName();

        @Override
        protected ArrayList<TrackInformation> doInBackground(String... params) {
            if (params.length == 0)
                return null;

            String artistId = params[0];

            SpotifyApi spotifyApi = new SpotifyApi();
            SpotifyService spotifyService = spotifyApi.getService();
            Map<String, Object> options = new HashMap<String, Object>();
            options.put(SpotifyService.OFFSET, 0);
            options.put(SpotifyService.LIMIT, 10);
            options.put(SpotifyService.COUNTRY, Locale.getDefault().getCountry());
            Tracks results = spotifyService.getArtistTopTrack(artistId, options);

            ArrayList<TrackInformation> tracks = new ArrayList<TrackInformation>();
            for (Track track : results.tracks) {
                tracks.add(
                        new TrackInformation(
                                track.album.name,
                                track.name,
                                (track.album.images.size() > 0) ? track.album.images.get(0) : null)
                );
            }

            return tracks;
        }

        @Override
        protected void onPostExecute(ArrayList<TrackInformation> tracks) {
            if (tracks.size() > 0) {
                mTracksAdapter.clear();
                for (TrackInformation track : tracks) {
                    mTracksAdapter.add(track);
                }
            }
        }
    }
}