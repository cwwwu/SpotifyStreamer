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
import android.support.v4.view.MenuItemCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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
import com.wallacewu.spotifystreamer.data.ArtistInformation;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import retrofit.RetrofitError;


/**
 * This fragment enables the user to search for an artist and have
 * the search results show up as a list.
 */
public class ArtistSearchFragment extends Fragment {

    private ArtistAdapter   mArtistsAdapter;
    private MenuItem        mSearchViewMenuItem;
    private SearchView      mSearchView;
    private ListView        mArtistList;
    private ProgressBar     mProgressBar;

    private ArrayList<ArtistInformation> mArtists;

    private int             mPosition = ListView.INVALID_POSITION;

    static final public String INTENT_EXTRA_ARTIST_NAME = "ARTIST_NAME";
    static final public String INTENT_EXTRA_ARTIST_ID = "ARTIST_ID";

    static final private String BUNDLE_ARTIST_PARCEL_LIST = "ARTISTS";
    static final private String BUNDLE_SELECTED_POSITION = "POSITION";

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        void onItemSelected(String artistName, String artistId);
    }

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
                ((Callback) getActivity()).onItemSelected(artist.name, artist.id);
                mPosition = position;
            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey(BUNDLE_SELECTED_POSITION)) {
            mPosition = savedInstanceState.getInt(BUNDLE_SELECTED_POSITION);
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPosition != ListView.INVALID_POSITION) {
            mArtistList.smoothScrollToPosition(mPosition);
        }
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_artist_search, menu);

        mSearchViewMenuItem = menu.findItem(R.id.search_artist);
        mSearchView = (SearchView) mSearchViewMenuItem.getActionView();

        // Automatically show and hide the keyboard when the search view is expanded and collapsed, respectively. In
        // the case of the search view being expanded, set the focus on the search view.
        MenuItemCompat.setOnActionExpandListener(mSearchViewMenuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mSearchView.requestFocus();
                mSearchView.requestFocusFromTouch();
                InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
                return true;
            }
        });

        mSearchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        mSearchView.setQueryHint(getActivity().getString(R.string.artist_search_hint));
        mSearchView.setFocusable(true);
        mSearchView.setIconifiedByDefault(false);

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                //Hide keyboard when user presses 'search'
                mSearchView.clearFocus();
                InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);

                mSearchViewMenuItem.collapseActionView();

                FetchArtistTask fetchArtistTask = new FetchArtistTask();
                fetchArtistTask.execute(query);
                return true;
            }
        });

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(BUNDLE_ARTIST_PARCEL_LIST, mArtists);
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(BUNDLE_SELECTED_POSITION, mPosition);
        }
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
