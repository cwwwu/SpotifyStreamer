package com.wallacewu.spotifystreamer;

import android.app.PendingIntent;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.wallacewu.spotifystreamer.data.TrackInformation;

import java.util.ArrayList;


public class MediaPlayerActivity extends ActionBarActivity {

    static final private String MEDIA_PLAYER_FRAGMENT_TAG = "MEDIA_PLAYER_FRAGMENT_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);

        Intent intent = getIntent();

        ArrayList<TrackInformation> tracks = new ArrayList<>();
        int startTrackIdx = -1;
        String artistName = "";

        if (intent != null) {
            startTrackIdx = intent.getIntExtra(TopTracksFragment.INTENT_EXTRA_TRACK_IDX, -1);
            if (intent.hasExtra(TopTracksFragment.INTENT_EXTRA_ARTIST_NAME))
                artistName = intent.getStringExtra(TopTracksFragment.INTENT_EXTRA_ARTIST_NAME);
            if (intent.hasExtra(TopTracksFragment.INTENT_EXTRA_TRACK_LIST))
                tracks = intent.getParcelableArrayListExtra(TopTracksFragment.INTENT_EXTRA_TRACK_LIST);
        }

        if (savedInstanceState == null) {
            Bundle args = new Bundle();
            args.putString(TopTracksFragment.INTENT_EXTRA_ARTIST_NAME, artistName);
            args.putInt(TopTracksFragment.INTENT_EXTRA_TRACK_IDX, startTrackIdx);
            args.putParcelableArrayList(TopTracksFragment.INTENT_EXTRA_TRACK_LIST, tracks);

            MediaPlayerFragment mediaPlayerFragment = new MediaPlayerFragment();
            mediaPlayerFragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.player_container, mediaPlayerFragment, MEDIA_PLAYER_FRAGMENT_TAG)
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_media_player, menu);
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
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
