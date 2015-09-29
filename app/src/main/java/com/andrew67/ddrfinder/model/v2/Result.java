/*
 * Copyright (c) 2015 Andrés Cordero
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

package com.andrew67.ddrfinder.model.v2;

import com.andrew67.ddrfinder.interfaces.ApiResult;
import com.andrew67.ddrfinder.interfaces.ArcadeLocation;
import com.andrew67.ddrfinder.interfaces.DataSource;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.Arrays;
import java.util.List;

/**
 * Represents the API v2 result.
 * See: https://github.com/Andrew67/ddr-finder/wiki/API-Description
 */
public class Result implements ApiResult {
    private String error;
    private Integer errorCode;
    private Source[] sources;
    private Location[] locations;
    private transient LatLngBounds bounds; // non-standard

    @Override
    public int getErrorCode() {
        return (errorCode == null) ? ERROR_OK : errorCode;
    }

    @Override
    public List<ArcadeLocation> getLocations() {
        return Arrays.<ArcadeLocation>asList(locations);
    }

    @Override
    public List<DataSource> getSources() {
        return Arrays.<DataSource>asList(sources);
    }

    @Override
    public void setBounds(LatLngBounds bounds) {
        this.bounds = bounds;
    }

    @Override
    public LatLngBounds getBounds() {
        return bounds;
    }
}
