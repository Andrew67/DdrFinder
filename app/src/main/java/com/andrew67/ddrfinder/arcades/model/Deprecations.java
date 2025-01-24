/*
 * Copyright (c) 2025 Andrés Cordero
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

/**
 * Represents the API v3 app deprecation information.
 * See: <a href="https://github.com/Andrew67/ddr-finder/blob/master/docs/API.md">API docs</a>
 */
public final class Deprecations implements Parcelable {
    private int googlePlay;

    @SuppressWarnings("unused")
    private Deprecations() { }

    public int getGooglePlay() {
        return googlePlay;
    }

    @Override
    public boolean equals(Object that) {
        return that instanceof Deprecations && this.hashCode() == that.hashCode();
    }

    @Override
    public int hashCode() {
        return this.googlePlay;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(googlePlay);
    }

    private Deprecations(Parcel in) {
        this.googlePlay = in.readInt();
    }

    public static final Creator<Deprecations> CREATOR
            = new Creator<Deprecations>() {
        public Deprecations createFromParcel(Parcel in) {
            return new Deprecations(in);
        }

        public Deprecations[] newArray(int size) {
            return new Deprecations[size];
        }
    };
}
