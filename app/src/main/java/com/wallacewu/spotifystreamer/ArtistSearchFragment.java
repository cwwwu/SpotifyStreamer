package com.wallacewu.spotifystreamer;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.provider.Settings;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;
import retrofit.RetrofitError;


/**
 * This fragment enables the user to search for an artist and have
 * the search results show up as a list.
 */
public class ArtistSearchFragment extends Fragment {

    private ArtistAdapter   mArtistsAdapter;
    private SearchView      mSearchView;
    private ListView        mArtistList;
    private ProgressBar     mProgressBar;

    private ArrayList<ArtistInformation> mArtists;

    static final public String INTENT_EXTRA_ARTIST_NAME = "ARTIST_NAME";
    static final public String INTENT_EXTRA_ARTIST_ID = "ARTIST_ID";

    static final private String BUNDLE_ARTIST_PARCEL_LIST = "ARTISTS";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_artist_search, container, false);

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.search_artist_progress_bar);
        mSearchView = (SearchView) rootView.findViewById(R.id.search_artist_text);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                //Hide keyboard when user presses 'search'
                mSearchView.clearFocus();
                InputMethodManager inputMethodManager = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);

                FetchArtistTask fetchArtistTask = new FetchArtistTask();
                fetchArtistTask.execute(query);
                return true;
            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey(BUNDLE_ARTIST_PARCEL_LIST)) {
            mArtists = savedInstanceState.getParcelableArrayList(BUNDLE_ARTIST_PARCEL_LIST);
        } else {
            mArtists = new ArrayList<>();
        }

        mArtistsAdapter = new ArtistAdapter(getActivity(), R.layout.list_item_artist, mArtists);
        mArtistList = (ListView) rootView.findViewById(R.id.search_artist_results_list);
        mArtistList.setAdapter(mArtistsAdapter);
        mArtistList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ArtistInformation artist = mArtistsAdapter.getItem(position);
                Intent topTracksIntent = new Intent(getActivity(), TopTracksActivity.class)
                        .putExtra(INTENT_EXTRA_ARTIST_NAME, artist.name)
                        .putExtra(INTENT_EXTRA_ARTIST_ID, artist.id);
                startActivity(topTracksIntent);
            }
        });

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(BUNDLE_ARTIST_PARCEL_LIST, mArtists);
        super.onSaveInstanceState(outState);
    }

    /**
     * Custom array adapter that is used to display the list of artists returned
     * from the Spotify query made.
     */
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
            if (artist.imageUrl != null) {
                Picasso.with(getContext()).load(artist.imageUrl).into(artistImageView);
            } else {
                artistImageView.setImageResource(R.color.missing_thumbnail);
            }

            return view;
        }
    }

    /**
     * The asynchronous task used to retrieve artist information from Spotify when the user
     * enters an artist name.
     */
    class FetchArtistTask extends AsyncTask<String, Void, ArrayList<ArtistInformation>> {

        private final String LOG_TAG = FetchArtistTask.class.getSimpleName();
        private RetrofitError.Kind mStatus = null;

        @Override
        protected void onPreExecute() {
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected ArrayList<ArtistInformation> doInBackground(String... params) {
            if (params.length == 0)
                return null;

            String searchArtistName = params[0];

            ArrayList<ArtistInformation> artists = new ArrayList<ArtistInformation>();
            try {
                SpotifyApi spotifyApi = new SpotifyApi();
                SpotifyService spotifyService = spotifyApi.getService();
                ArtistsPager results = spotifyService.searchArtists(searchArtistName);

                for (Artist artist : results.artists.items) {
                    artists.add(
                            new ArtistInformation(
                                    artist.name,
                                    artist.id,
                                    (artist.images.size() > 0) ? artist.images.get(0).url : null)
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

            return artists;
        }

        @Override
        protected void onPostExecute(ArrayList<ArtistInformation> artists) {
            mProgressBar.setVisibility(View.GONE);

            // Handle network error gracefully by asking the user to check if the device's wireless
            // settings are okay. The user can go to the wireless settings if he/she opts to do so
            // from the dialog.
            if (artists == null && mStatus == RetrofitError.Kind.NETWORK) {
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

            if (artists.size() > 0) {
                mArtistsAdapter.clear();
                for (ArtistInformation artist : artists) {
                    mArtistsAdapter.add(artist);
                }
                mArtistList.smoothScrollToPosition(0);
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
