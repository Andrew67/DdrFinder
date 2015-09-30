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

import android.os.Parcelable;

/**
 * Standard interface for API data source.
 * When new fields are added in API versions, old models should provide new functions to compensate.
 * For example, the model.v1 ApiResultV1 class was extended to return static "ziv" source information.
 * See: https://github.com/Andrew67/ddr-finder/wiki/API-Description
 */
public interface DataSource extends Parcelable {

    /**
     * Get the key/shortname of the data source, e.g. "ziv"
     * @return Shortname of data source.
     */
    String getShortName();

    /**
     * Get the human-readable name of the data source, e.g. "Zenius -I- vanisher.com"
     * @return Human-readable name of the data soruce.
     */
    String getName();

    /**
     * Get the info URL for locations that belong to this source.
     * @return Info URL for locations.
     */
    String getInfoURL();

    /**
     * Retuns whether the location has meaningful data for the "hasDDR" field.
     * @return Whether the "hasDDR" field is meaningful.
     */
    boolean hasDDR();
}
