/*
 * Copyright (c) 2018 Andrés Cordero
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
import android.support.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;

/**
 * Helps parse app URLs (e.g. for positioning the map).
 * See docs/URLs.md in the ddr-finder main project.
 */
public class AppUrlParser {

    /**
     * Builds an instance of ParsedAppUrl by parsing the given Uri.
     * @param url Uri to parse. Can come straight from getIntent().getData().
     * @return Instance of ParsedAppUrl.
     */
    public static ParsedAppUrl parse(@Nullable Uri url) {
        LatLng position = null;
        Integer zoom = null;
        if (url != null) {
            try {
                // For now we only handle /ng links, so no need to verify path segments.
                String ll = url.getQueryParameter("ll");
                if (ll != null) {
                    String[] llComponents = ll.split(",");
                    Double latitude = Double.valueOf(llComponents[0]);
                    Double longitude = Double.valueOf(llComponents[1]);
                    position = new LatLng(latitude, longitude);
                }
                String z = url.getQueryParameter("z");
                if (z != null) {
                    zoom = Integer.valueOf(z);
                }
            } catch (NumberFormatException e) {
                // This exception is to be expected given user input.
            }
        }
        return new ParsedAppUrl(position, zoom);
    }

}
