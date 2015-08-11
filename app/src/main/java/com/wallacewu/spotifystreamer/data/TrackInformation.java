package com.wallacewu.spotifystreamer.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class that holds the necessary information for a track.
 *
 * Created by Wallace on 7/14/2015.
 */
public class TrackInformation implements Parcelable {
    public String   albumName;
    public String   trackName;
    public String   albumImageUrl;
    public String   trackPreviewUrl;

    public TrackInformation(String albumName, String trackName, String albumImageUrl, String trackPreviewUrl) {
        this.albumName = albumName;
        this.trackName = trackName;
        this.albumImageUrl = albumImageUrl;
        this.trackPreviewUrl = trackPreviewUrl;
    }

    private TrackInformation(Parcel parcel) {
        this.albumName = parcel.readString();
        this.trackName = parcel.readString();
        this.albumImageUrl = parcel.readString();
        this.trackPreviewUrl = parcel.readString();
    }

    public String toString() {
        return albumName + " " + trackName + " " + albumImageUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(albumName);
        dest.writeString(trackName);
        dest.writeString(albumImageUrl);
        dest.writeString(trackPreviewUrl);
    }

    public static final Parcelable.Creator<TrackInformation> CREATOR = new Parcelable.Creator<TrackInformation>() {
        @Override
        public TrackInformation createFromParcel(Parcel source) {
            return new TrackInformation(source);
        }

        @Override
        public TrackInformation[] newArray(int size) {
            return new TrackInformation[size];
        }
    };
}