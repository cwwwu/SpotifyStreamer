package com.wallacewu.spotifystreamer;

import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.wallacewu.spotifystreamer.data.TrackInformation;

import java.util.ArrayList;


public class TopTracksActivity extends ActionBarActivity implements TopTracksFragment.Callback {

    private String  mSelectedArtist;
    static final private String BUNDLE_SELECTED_ARTIST = "SELECTED_ARTIST";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_tracks);

        if (savedInstanceState == null) {
            mSelectedArtist = getIntent().getStringExtra(ArtistSearchFragment.INTENT_EXTRA_ARTIST_NAME);


            Bundle args = new Bundle();
            args.putString(ArtistSearchFragment.INTENT_EXTRA_ARTIST_NAME, mSelectedArtist);
            args.putString(ArtistSearchFragment.INTENT_EXTRA_ARTIST_ID, getIntent().getStringExtra(ArtistSearchFragment.INTENT_EXTRA_ARTIST_ID));

            TopTracksFragment topTracksFragment = new TopTracksFragment();
            topTracksFragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.top_tracks_container, topTracksFragment)
                    .commit();
        } else {
            mSelectedArtist = savedInstanceState.getString(BUNDLE_SELECTED_ARTIST, "");
        }

        getSupportActionBar().setSubtitle(mSelectedArtist);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //mTracksFragement.setData(collectMyLoadedData());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(BUNDLE_SELECTED_ARTIST, mSelectedArtist);
        super.onSaveInstanceState(outState);
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

    @Override
    public void onTrackSelected(String artistName, ArrayList<TrackInformation> tracks, int startTrackIdx) {
        Intent mediaPlayerIntent = new Intent(this, MediaPlayerActivity.class)
            .putParcelableArrayListExtra(TopTracksFragment.INTENT_EXTRA_TRACK_LIST, tracks)
            .putExtra(TopTracksFragment.INTENT_EXTRA_TRACK_IDX, startTrackIdx)
            .putExtra(TopTracksFragment.INTENT_EXTRA_ARTIST_NAME, artistName);
        startActivity(mediaPlayerIntent);
    }
}
