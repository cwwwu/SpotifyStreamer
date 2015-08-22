package com.wallacewu.spotifystreamer;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.wallacewu.spotifystreamer.data.TrackInformation;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity implements ArtistSearchFragment.Callback, TopTracksFragment.Callback {

    private boolean mTwoPane;
    private String  mSelectedArtist;
    private ActionBar mActionBar;

    private MediaPlayerFragment mMediaPlayerFragment;
    static final private String TOP_TRACKS_FRAGMENT_TAG = "TOP_TRACKS_TAG";
    static final private String MEDIA_PLAYER_FRAGMENT_TAG = "MEDIA_PLAYER_FRAGMENT_TAG";
    static final private String BUNDLE_SELECTED_ARTIST = "SELECTED_ARTIST";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSelectedArtist = getString(R.string.action_bar_prompt);

        setContentView(R.layout.activity_main);

        mActionBar = this.getSupportActionBar();

        if (savedInstanceState != null && savedInstanceState.containsKey(BUNDLE_SELECTED_ARTIST)) {
            mSelectedArtist = savedInstanceState.getString(BUNDLE_SELECTED_ARTIST);
            mActionBar.setSubtitle(mSelectedArtist);
        }

        mActionBar.setSubtitle(mSelectedArtist);

        if (findViewById(R.id.top_tracks_container) != null) {
            mTwoPane = true;

            if (savedInstanceState == null) {
                findViewById(R.id.top_tracks_container).setVisibility(View.GONE);
                TopTracksFragment topTracksFragment = new TopTracksFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.top_tracks_container, topTracksFragment, TOP_TRACKS_FRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(BUNDLE_SELECTED_ARTIST, mSelectedArtist);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
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
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onArtistSelected(String artistName, String artistId) {
        mSelectedArtist = artistName;
        if (mTwoPane) {
            mActionBar.setSubtitle(mSelectedArtist);

            Bundle args = new Bundle();
            args.putString(ArtistSearchFragment.INTENT_EXTRA_ARTIST_NAME, artistName);
            args.putString(ArtistSearchFragment.INTENT_EXTRA_ARTIST_ID, artistId);

            TopTracksFragment fragment = new TopTracksFragment();
            fragment.setArguments(args);

            findViewById(R.id.top_tracks_container).setVisibility(View.VISIBLE);
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

    @Override
    public void onSuccessfulArtistSearch(String query) {
        mActionBar.setSubtitle(getString(R.string.artist_search_prefix) + " \"" + query + "\"");

        if (mTwoPane) {
            findViewById(R.id.top_tracks_container).setVisibility(View.GONE);
        }
    }

    @Override
    public void onTrackSelected(String artistName, ArrayList<TrackInformation> tracks, int startTrackIdx) {
        Bundle args = new Bundle();
        args.putString(TopTracksFragment.INTENT_EXTRA_ARTIST_NAME, artistName);
        args.putInt(TopTracksFragment.INTENT_EXTRA_TRACK_IDX, startTrackIdx);
        args.putParcelableArrayList(TopTracksFragment.INTENT_EXTRA_TRACK_LIST, tracks);

        if (mMediaPlayerFragment == null) {
            mMediaPlayerFragment = new MediaPlayerFragment();
        }
        mMediaPlayerFragment.setArguments(args);

        mMediaPlayerFragment.show(getSupportFragmentManager(), MEDIA_PLAYER_FRAGMENT_TAG);
    }
}
