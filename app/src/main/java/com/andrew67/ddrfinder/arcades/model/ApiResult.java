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

import android.support.annotation.NonNull;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.Collections;
import java.util.List;

/**
 * Represents the API v3 result.
 * See: https://github.com/Andrew67/ddr-finder/blob/master/docs/API.md
 */
public final class ApiResult {

    private String error;
    private Integer errorCode;
    private List<DataSource> sources;
    private List<ArcadeLocation> locations;
    private transient LatLngBounds bounds; // non-API

    private ApiResult() { }

    public ApiResult(List<DataSource> sources,
                      List<ArcadeLocation> locations,
                      LatLngBounds bounds) {
        this.sources = sources;
        this.locations = locations;
        this.bounds = bounds;
    }

    /**
     * Returns the error code from the API.
     * @return Error code.
     */
    public int getErrorCode() {
        return (errorCode == null) ? ERROR_OK : errorCode;
    }

    // Error codes, from V2
    public static final int ERROR_CLIENT_API_VERSION = 1;
    public static final int ERROR_REQUIRED_FIELD = 20;
    public static final int ERROR_DATA_SOURCE = 21;
    public static final int ERROR_OVERSIZED_BOX = 23;
    public static final int ERROR_REQUESTS = 42;
    public static final int ERROR_OK = -1; // non-standard; success case
    public static final int ERROR_UNEXPECTED = -2; // non-standard
    public static final int ERROR_NO_RESULTS = -3; // non-standard

    /**
     * Returns the list of ArcadeLocation items from the API.
     */
    @NonNull
    public List<ArcadeLocation> getLocations() {
        if (locations != null) return locations;
        else return Collections.emptyList();
    }

    /**
     * Returns the list of DataSource items from the API.
     */
    @NonNull
    public List<DataSource> getSources() {
        if (sources != null) return sources;
        else return Collections.emptyList();
    }

    /**
     * (non-API) Set the latitude/longitude bounds this result belongs to.
     * This field should only be set once.
     */
    public void setBounds(LatLngBounds bounds) {
        this.bounds = bounds;
    }

    /**
     * (non-API) Get the latitude/longitude bounds this result belongs to.
     */
    public LatLngBounds getBounds() {
        return bounds;
    }
}
