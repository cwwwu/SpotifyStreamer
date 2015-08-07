package com.wallacewu.spotifystreamer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.wallacewu.spotifystreamer.model.TrackInformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.RetrofitError;


/**
 * This fragment displays the top ten tracks for a given artist in a list.
 */
public class TopTracksFragment extends Fragment {

    private ProgressBar     mProgressBar;
    private TextView        mErrorTextView;
    private ListView        mTrackListView;
    private TrackAdapter    mTracksAdapter;
    private ArrayList<TrackInformation> mTracks;
    private String          mArtistName;

    static final public String INTENT_EXTRA_ARTIST_NAME = "ARTIST_NAME";
    static final public String INTENT_EXTRA_TRACK_LIST = "TRACK_LIST";
    static final public String INTENT_EXTRA_TRACK_IDX = "TRACK_START_IDX";

    static final private String BUNDLE_TRACKS_PARCEL_LIST = "TRACKS";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_top_tracks, container, false);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.top_tracks_progress_bar);
        mErrorTextView = (TextView) rootView.findViewById(R.id.top_tracks_no_tracks);

        Bundle args = getArguments();
        String artistId = null;
        mArtistName = "Unknown artist";

        if (args != null) {
            mArtistName = args.getString(ArtistSearchFragment.INTENT_EXTRA_ARTIST_NAME, "Unknown artist");
            artistId = args.getString(ArtistSearchFragment.INTENT_EXTRA_ARTIST_ID, null);
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(BUNDLE_TRACKS_PARCEL_LIST)) {
            mTracks = savedInstanceState.getParcelableArrayList(BUNDLE_TRACKS_PARCEL_LIST);
        } else {
            mTracks = new ArrayList<>();
        }

        mTracksAdapter = new TrackAdapter(getActivity(), R.layout.list_item_artist, mTracks);
        mTrackListView = (ListView) rootView.findViewById(R.id.top_ten_tracks_list);
        mTrackListView.setAdapter(mTracksAdapter);

        mTrackListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent mediaPlayerIntent = new Intent(getActivity(), MediaPlayerActivity.class)
                        .putExtra(INTENT_EXTRA_TRACK_LIST, mTracks)
                        .putExtra(INTENT_EXTRA_TRACK_IDX, position)
                        .putExtra(INTENT_EXTRA_ARTIST_NAME, mArtistName);
                startActivity(mediaPlayerIntent);
            }
        });

        if (args != null && artistId != null && mTracks.isEmpty()) {
            FetchTrackTask fetchTrackTask = new FetchTrackTask();
            fetchTrackTask.execute(artistId);
        }

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(BUNDLE_TRACKS_PARCEL_LIST, mTracks);
        super.onSaveInstanceState(outState);
    }

    /**
     * Custom array adapter that is used to display a list of an artists' top ten tracks
     * from the Spotify query made.
     */
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
            if (track.albumImageUrl != null) {
                Picasso.with(getContext()).load(track.albumImageUrl).into(albumImageView);
            } else {
                albumImageView.setImageResource(R.color.missing_thumbnail);
            }

            return view;
        }
    }

    /**
     * The asynchronous task used to retrieve track information from Spotify when the parent
     * activity is created.
     */
    class FetchTrackTask extends AsyncTask<String, Void, ArrayList<TrackInformation>> {

        private final String LOG_TAG = FetchTrackTask.class.getSimpleName();
        private RetrofitError.Kind mStatus = null;

        @Override
        protected void onPreExecute() {
            mProgressBar.setVisibility(View.VISIBLE);
        }

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
            try {
                for (Track track : results.tracks) {
                    tracks.add(
                            new TrackInformation(
                                    track.album.name,
                                    track.name,
                                    (track.album.images.size() > 0) ? track.album.images.get(0).url : null,
                                    track.preview_url)
                    );
                }
            } catch (RetrofitError error) {
                if (error.getKind() == RetrofitError.Kind.NETWORK) {
                    mStatus = RetrofitError.Kind.NETWORK;
                    return null;
                } else {
                    throw error;
                }
            }

            return tracks;
        }

        @Override
        protected void onPostExecute(ArrayList<TrackInformation> tracks) {
            mProgressBar.setVisibility(View.GONE);

            if (tracks == null && mStatus == RetrofitError.Kind.NETWORK) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.network_connectivity_message)
                        .setPositiveButton(R.string.open_network_settings, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .create().show();
                return;
            }

            if (tracks.size() > 0) {
                mTracksAdapter.clear();
                for (TrackInformation track : tracks) {
                    mTracksAdapter.add(track);
                }
            } else {
                mErrorTextView.setVisibility(View.VISIBLE);
            }
        }
    }
}
