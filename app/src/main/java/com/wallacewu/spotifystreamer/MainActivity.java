package com.wallacewu.spotifystreamer;

import android.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity {

    private ArtistSearchFragment mSearchFragment;
    static final private String SEARCH_FRAGMENT_TAG = "SEARCH_FRAGMENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_search);

        // Find the retained fragment on activity restarts
        FragmentManager fragmentManager = getFragmentManager();
        mSearchFragment = (ArtistSearchFragment) fragmentManager.findFragmentByTag(SEARCH_FRAGMENT_TAG);

        if (mSearchFragment == null) {
            mSearchFragment = new ArtistSearchFragment();
            fragmentManager.beginTransaction().add(mSearchFragment, SEARCH_FRAGMENT_TAG).commit();
            //mSearchFragment.setData(loadMyData())
        }

        //data is available in getData()
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
}
