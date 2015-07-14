package com.wallacewu.spotifystreamer;

import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class TopTracksActivity extends ActionBarActivity {

    private TopTracksFragment mTracksFragment;
    static final private String TRACKS_FRAGMENT_TAG = "TRACKS_FRAGMENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_tracks);

        // Find the retained fragment on activity restarts
        FragmentManager fragmentManager = getSupportFragmentManager();
        mTracksFragment = (TopTracksFragment) fragmentManager.findFragmentByTag(TRACKS_FRAGMENT_TAG);

        if (mTracksFragment == null) {
            mTracksFragment = new TopTracksFragment();
            fragmentManager.beginTransaction().add(mTracksFragment, TRACKS_FRAGMENT_TAG).commit();
            //mTracksFragment.setData(loadMyData());
        }

        // the data is available getData();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //mTracksFragement.setData(collectMyLoadedData());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // Add menu later, as needed
        //getMenuInflater().inflate(R.menu.menu_top_tracks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.finish();
            return true;
        }
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
