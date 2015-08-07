package com.wallacewu.spotifystreamer.model;


import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class that holds the necessary information for an artist.
 *
 * Created by Wallace on 7/14/2015.
 */
public class ArtistInformation implements Parcelable {
    public String   name;
    public String   id;
    public String   imageUrl;

    public ArtistInformation(String name, String id, String imageUrl) {
        this.name = name;
        this.id = id;
        this.imageUrl = imageUrl;
    }

    private ArtistInformation(Parcel parcel) {
        this.name = parcel.readString();
        this.id = parcel.readString();
        this.imageUrl = parcel.readString();
    }

    public String toString() {
        return name + " " + id + " " + imageUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(id);
        dest.writeString(imageUrl);
    }

    public static final Parcelable.Creator<ArtistInformation> CREATOR = new Parcelable.Creator<ArtistInformation>() {
        @Override
        public ArtistInformation createFromParcel(Parcel source) {
            return new ArtistInformation(source);
        }

        @Override
        public ArtistInformation[] newArray(int size) {
            return new ArtistInformation[size];
        }
    };
}