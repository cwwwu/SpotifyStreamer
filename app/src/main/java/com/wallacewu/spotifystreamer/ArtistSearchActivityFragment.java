package com.wallacewu.spotifystreamer;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Pager;


/**
 * A placeholder fragment containing a simple view.
 */
public class ArtistSearchActivityFragment extends Fragment {

    private ArtistAdapter mArtistsAdapter;
    private String mSearchString;
    static final public String INTENT_EXTRA_ARTIST_NAME = "ARTIST_NAME";
    static final public String INTENT_EXTRA_ARTIST_ID = "ARTIST_ID";

    public ArtistSearchActivityFragment() {
        mSearchString = "";
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_artist_search, container, false);

        EditText searchEditText = (EditText) rootView.findViewById(R.id.search_artist_text);

        searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    mSearchString = v.getText().toString();
                    FetchArtistTask fetchArtistTask = new FetchArtistTask();
                    fetchArtistTask.execute(mSearchString);
                    handled = true;
                }
                return handled;
            }
        });

        mArtistsAdapter = new ArtistAdapter(getActivity(),
                R.layout.list_item_artist,
                new ArrayList<ArtistInformation>());
        ListView artistListView = (ListView) rootView.findViewById(R.id.search_artist_results_list);
        artistListView.setAdapter(mArtistsAdapter);
        artistListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ArtistInformation artist = mArtistsAdapter.getItem(position);
                Intent topTracksIntent = new Intent(getActivity(), TopTracksActivity.class)
                        .putExtra(INTENT_EXTRA_ARTIST_NAME, artist.name)
                        .putExtra(INTENT_EXTRA_ARTIST_ID, artist.id);
                startActivity(topTracksIntent);
            }
        });

        if (mSearchString.length() > 0) {
            FetchArtistTask fetchArtistTask = new FetchArtistTask();
            fetchArtistTask.execute(mSearchString);
        }

        return rootView;
    }

    class ArtistInformation {
        public String   name;
        public String   id;
        public Image    image;

        ArtistInformation(String name, String id, Image image) {
            this.name = name;
            this.id = id;
            this.image = image;
        }
    }

    class ArtistAdapter extends ArrayAdapter<ArtistInformation> {

        public ArtistAdapter(Context context, int resource, ArrayList<ArtistInformation> artists) {
            super(context, resource, artists);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            ArtistInformation artist = getItem(position);

            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.list_item_artist, parent, false);
            }

            TextView artistTextView = (TextView) view.findViewById(R.id.artist_result_name);
            ImageView artistImageView = (ImageView) view.findViewById(R.id.artist_image_thumbnail);

            artistTextView.setText(artist.name);
            if (artist.image != null) {
                Picasso.with(getContext()).load(artist.image.url).into(artistImageView);
            } else {
                artistImageView.setImageResource(android.R.color.transparent);
            }

            return view;
        }
    }

    class FetchArtistTask extends AsyncTask<String, Void, ArrayList<ArtistInformation>> {

        private final String LOG_TAG = FetchArtistTask.class.getSimpleName();

        @Override
        protected ArrayList<ArtistInformation> doInBackground(String... params) {
            if (params.length == 0)
                return null;

            String searchArtistName = params[0];

            SpotifyApi spotifyApi = new SpotifyApi();
            SpotifyService spotifyService = spotifyApi.getService();
            ArtistsPager results = spotifyService.searchArtists(searchArtistName);

            ArrayList<ArtistInformation> artists = new ArrayList<ArtistInformation>();
            for (Artist artist : results.artists.items) {
                Log.d(LOG_TAG, "Artist name: " + artist.name);
                Log.d(LOG_TAG, "Artist's spotify id: " + artist.id);
                artists.add(
                        new ArtistInformation(
                                artist.name,
                                artist.id,
                                (artist.images.size() > 0) ? artist.images.get(0) : null)
                );
            }

            return artists;
        }

        @Override
        protected void onPostExecute(ArrayList<ArtistInformation> artists) {
            if (artists.size() > 0) {
                mArtistsAdapter.clear();
                for (ArtistInformation artist : artists) {
                    mArtistsAdapter.add(artist);
                }
            } else {
                Toast.makeText(
                        getActivity().getApplicationContext(),
                        getString(R.string.artist_not_found),
                        Toast.LENGTH_LONG)
                        .show();
            }
        }
    }
}
