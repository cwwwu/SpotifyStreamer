package com.wallacewu.spotifystreamer;

import android.app.FragmentManager;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity implements ArtistSearchFragment.Callback {

    private boolean mTwoPane;
    private ArtistSearchFragment mSearchFragment;
    static final private String TOP_TRACKS_FRAGMENT_TAG = "TOP_TRACKS_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (findViewById(R.id.top_tracks_container) != null) {
            mTwoPane = true;

            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.top_tracks_container, new TopTracksFragment(), TOP_TRACKS_FRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;
        }

//        FragmentManager fragmentManager = getFragmentManager();
//        mSearchFragment = (ArtistSearchFragment) fragmentManager.findFragmentById(R.id.fragment_artist_search);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // store the data in the search artist fragment
        //mSearchFragment.setData(collectMyLoadedData())
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // Add menu later, as needed
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(String artistName, String artistId) {
        if (mTwoPane) {
            Bundle args = new Bundle();
            args.putString(ArtistSearchFragment.INTENT_EXTRA_ARTIST_NAME, artistName);
            args.putString(ArtistSearchFragment.INTENT_EXTRA_ARTIST_ID, artistId);

            TopTracksFragment fragment = new TopTracksFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.top_tracks_container, fragment, TOP_TRACKS_FRAGMENT_TAG)
                    .commit();
        } else {
            Intent topTracksIntent = new Intent(this, TopTracksActivity.class)
                .putExtra(ArtistSearchFragment.INTENT_EXTRA_ARTIST_NAME, artistName)
                .putExtra(ArtistSearchFragment.INTENT_EXTRA_ARTIST_ID, artistId);
            startActivity(topTracksIntent);
        }
    }
}
