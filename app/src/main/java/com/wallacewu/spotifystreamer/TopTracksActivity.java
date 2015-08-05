package com.wallacewu.spotifystreamer;

import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
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

        if (savedInstanceState == null) {
            ActionBar actionBar = this.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setSubtitle(getIntent().getStringExtra(ArtistSearchFragment.INTENT_EXTRA_ARTIST_NAME)); // TODO: handle config change
            }

            Bundle args = new Bundle();
            args.putString(ArtistSearchFragment.INTENT_EXTRA_ARTIST_NAME, getIntent().getStringExtra(ArtistSearchFragment.INTENT_EXTRA_ARTIST_NAME));
            args.putString(ArtistSearchFragment.INTENT_EXTRA_ARTIST_ID, getIntent().getStringExtra(ArtistSearchFragment.INTENT_EXTRA_ARTIST_ID));

            TopTracksFragment topTracksFragment = new TopTracksFragment();
            topTracksFragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.top_tracks_container, topTracksFragment)
                    .commit();
        }

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
