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

import android.support.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;

/**
 * Represents a parsed app URL (e.g. for positioning the map).
 * @see AppUrlParser
 */
public class ParsedAppUrl {

    private LatLng position = null;
    private Integer zoom = null;

    /**
     * Build the ParsedAppUrl. Cannot be called directly.
     */
    ParsedAppUrl(@Nullable LatLng position, @Nullable Integer zoom) {
        this.position = position;
        this.zoom = zoom;
    }

    /**
     * Return the positions for the center of the map, if set.
     * @return Center position, or null if not specified.
     */
    @Nullable
    public LatLng getPosition() {
        return position;
    }

    /**
     * Return the zoom level for the map, if set.
     * @return Zoom level, or null if not specified.
     */
    @Nullable
    public Integer getZoom() {
        return zoom;
    }

    /**
     * Return the data source to use, if set.
     * @return Data source short name, or null if not specified.
     */
    @Nullable
    public String getSourceShortName() {
        return null;
    }

}
