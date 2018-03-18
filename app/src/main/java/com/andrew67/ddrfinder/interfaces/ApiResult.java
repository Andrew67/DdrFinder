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

package com.andrew67.ddrfinder.interfaces;

import com.google.android.gms.maps.model.LatLngBounds;

import java.util.List;

/**
 * Standard interface for API results.
 * When new fields are added in API versions, old models should provide new functions to compensate.
 * For example, the model.v1 ApiResultV1 class was extended to return static "ziv" source information.
 * Normalizes error codes to latest API level as well.
 * See: https://github.com/Andrew67/ddr-finder/blob/master/docs/API.md
 */
public interface ApiResult {

    /**
     * Returns the error code from the API.
     * @return Error code.
     */
    int getErrorCode();

    // Error codes, from V2
    int ERROR_CLIENT_API_VERSION = 1;
    int ERROR_REQUIRED_FIELD = 20;
    int ERROR_DATA_SOURCE = 21;
    int ERROR_OVERSIZED_BOX = 23;
    int ERROR_REQUESTS = 42;
    int ERROR_OK = -1; // non-standard; success case
    int ERROR_UNEXPECTED = -2; // non-standard
    int ERROR_NO_RESULTS = -3; // non-standard

    /**
     * Returns the list of ArcadeLocation items from the API.
     * @return List of arcade locations. In case of error, empty list is returned. Never null.
     */
    List<ArcadeLocation> getLocations();

    /**
     * Returns the list of DataSource items from the API.
     * @return List of data sources. In case of error, empty list is returned, never null.
     */
    List<DataSource> getSources();

    /**
     * (non-API) Set the latitude/longitude bounds this result belongs to.
     * This field should only be set once.
     * @param bounds Latitude/longitude bounds.
     */
    void setBounds(LatLngBounds bounds);

    /**
     * (non-API) Get the latitude/longitude bounds this result belongs to.
     * @return Latitude/longitude bounds.
     */
    LatLngBounds getBounds();
}
