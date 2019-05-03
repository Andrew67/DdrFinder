/*
 * Copyright (c) 2013-2019 Andrés Cordero
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

package com.andrew67.ddrfinder.util;

import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Utility class for building geo: URIs.
 */
public class GeoUriBuilder {

    /**
     * Builds a geo: URI with the given parameters, intended to have the map app show a marker
     * at that location with the given label.
     */
    public static Uri showMarkerAt(double latitude, double longitude, @NonNull String label) {
        String encodedLabel = "";
        try {
            encodedLabel = URLEncoder.encode(label, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // UTF-8 should never be unsupported
        }
        return Uri.parse("geo:" + latitude + "," + longitude +
                "?q=" + latitude + "," + longitude +
                "(" + encodedLabel + ")");
    }

    /**
     * Same as {@link #showMarkerAt(double, double, String)}, but creates a Google Maps URL.
     * Intended as a fallback mechanism for the case where no apps can handle geo: URLs.
     */
    public static Uri googleMapsMarkerAt(double latitude, double longitude, @NonNull String label) {
        String encodedLabel = "";
        try {
            encodedLabel = URLEncoder.encode(label, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // UTF-8 should never be unsupported
        }
        return Uri.parse("https://maps.google.com/?q=loc:" +
                latitude + "," + longitude + "(" + encodedLabel + ")");
    }

}
