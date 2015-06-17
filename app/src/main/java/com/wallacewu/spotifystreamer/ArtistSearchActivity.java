package com.wallacewu.spotifystreamer;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;


public class ArtistSearchActivity extends ActionBarActivity {

    private ArtistSearchActivityFragment mSearchActivityFragment;
    static final private String SEARCH_ACTIVITY_FRAGMENT_TAG = "SEARCH_ACTIVITY_FRAGMENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_search);

        FragmentManager fragmentManager = getSupportFragmentManager();
        mSearchActivityFragment = (ArtistSearchActivityFragment) fragmentManager.findFragmentByTag(
                ArtistSearchActivity.SEARCH_ACTIVITY_FRAGMENT_TAG);

        if (mSearchActivityFragment == null) {
            mSearchActivityFragment = new ArtistSearchActivityFragment();
            fragmentManager.beginTransaction().add(mSearchActivityFragment, ArtistSearchActivity.SEARCH_ACTIVITY_FRAGMENT_TAG)
                    .commit();
            if (savedInstanceState != null) {
                mSearchActivityFragment.setSearchArtist(
                        savedInstanceState.getString(ArtistSearchActivityFragment.INSTANCE_STATE_SEARCH_STRING,""));
            }
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mSearchActivityFragment != null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            mSearchActivityFragment.setSearchArtist(preferences.getString(ArtistSearchActivityFragment.INSTANCE_STATE_SEARCH_STRING, ""));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_artist_search, menu);
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
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString(ArtistSearchActivityFragment.INSTANCE_STATE_SEARCH_STRING, mSearchActivityFragment.getSearchArtist());
    }
}
