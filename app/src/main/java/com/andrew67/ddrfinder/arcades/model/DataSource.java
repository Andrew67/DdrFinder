/*
 * Copyright (c) 2015-2018 Andrés Cordero
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

import com.andrew67.ddrfinder.BuildConfig;

/**
 * Represents the API v3 data source.
 * See: https://github.com/Andrew67/ddr-finder/blob/master/docs/API.md
 * The infoURL field is ignored; mInfoURL is used in its place since this code is always on mobile
 */
public final class DataSource implements Parcelable {

    private String shortName;
    private String name;
    private String mInfoURL;
    private boolean hasDDR;

    private DataSource() { }

    /**
     * Get the key/shortname of the data source, e.g. "ziv"
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * Get the human-readable name of the data source, e.g. "Zenius -I- vanisher.com"
     */
    public String getName() {
        return name;
    }

    /**
     * Get the info URL for locations that belong to this source.
     */
    public String getInfoURL() {
        return mInfoURL;
    }

    /**
     * Retuns whether the location has meaningful data for the "hasDDR" field.
     */
    public boolean hasDDR() {
        return hasDDR;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(shortName);
        dest.writeString(name);
        dest.writeString(mInfoURL);
        dest.writeBooleanArray(new boolean[] { hasDDR });
    }

    private DataSource(Parcel in) {
        this.shortName = in.readString();
        this.name = in.readString();
        this.mInfoURL = in.readString();
        boolean[] hasDDRArray = new boolean[1];
        in.readBooleanArray(hasDDRArray);
        this.hasDDR = hasDDRArray[0];
    }

    public static final Parcelable.Creator<DataSource> CREATOR
            = new Parcelable.Creator<DataSource>() {
        public DataSource createFromParcel(Parcel in) {
            return new DataSource(in);
        }

        public DataSource[] newArray(int size) {
            return new DataSource[size];
        }
    };

    /**
     * Provides a "fallback" data source.
     * Since this is hard-coded, it should only be used if the server API fails to provide it.
     * @return Fallback data source for locations.
     */
    public static DataSource getFallback() {
        DataSource src = new DataSource();
        src.shortName = "fallback";
        src.name = "Source Website";
        src.mInfoURL = BuildConfig.FALLBACK_INFO_URL;
        src.hasDDR = false;
        return src;
    }
}
