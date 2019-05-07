/*
 * Copyright (c) 2015-2019 Andrés Cordero
 * Web: https://github.com/Andrew67/DdrFinder
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.andrew67.ddrfinder.arcades.model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

/**
 * Represents the API v3 arcade location.
 * See: https://github.com/Andrew67/ddr-finder/blob/master/docs/API.md
 */
public final class ArcadeLocation implements Parcelable, ClusterItem {
    private int id;
    private String src;
    private String sid;
    private String name;
    private String city;
    private double lat;
    private double lng;
    private Integer hasDDR; // optional; interface should be Boolean, but this gets fed to us as "0" or "1" (or null!)

    @SuppressWarnings("unused")
    private ArcadeLocation() { }

    public int getId() {
        return id;
    }

    public String getSrc() {
        return src;
    }

    public String getSid() {
        return sid;
    }

    public String getName() {
        return name;
    }

    // getTitle/getSnippet control the marker's info window.
    // Now that we're going with the bottom sheet approach, we want the window to be empty.

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public String getSnippet() {
        return null;
    }

    public String getCity() {
        return city;
    }

    private LatLng position = null;
    @Override
    @NonNull
    public LatLng getPosition() {
        return (position == null) ? position = new LatLng(lat, lng) : position;
    }

    public boolean hasDDR() {
        return Integer.valueOf(1).equals(hasDDR);
    }

    @Override
    public boolean equals(Object that) {
        return that instanceof ArcadeLocation && this.hashCode() == that.hashCode();
    }

    @Override
    public int hashCode() {
        return this.id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(src);
        dest.writeString(sid);
        dest.writeString(name);
        dest.writeString(city);
        dest.writeDouble(lat);
        dest.writeDouble(lng);
        // Accounts for hasDDR = null
        if (hasDDR()) dest.writeInt(1);
        else dest.writeInt(0);
    }

    private ArcadeLocation(Parcel in) {
        this.id = in.readInt();
        this.src = in.readString();
        this.sid = in.readString();
        this.name = in.readString();
        this.city = in.readString();
        this.lat = in.readDouble();
        this.lng = in.readDouble();
        this.hasDDR = in.readInt();
    }

    public static final Parcelable.Creator<ArcadeLocation> CREATOR
            = new Parcelable.Creator<ArcadeLocation>() {
        public ArcadeLocation createFromParcel(Parcel in) {
            return new ArcadeLocation(in);
        }

        public ArcadeLocation[] newArray(int size) {
            return new ArcadeLocation[size];
        }
    };
}
