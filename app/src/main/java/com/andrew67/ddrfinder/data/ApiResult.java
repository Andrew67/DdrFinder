/*
 * Copyright (c) 2013 Andrés Cordero 
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

package com.andrew67.ddrfinder.data;

import java.util.List;

import com.google.android.gms.maps.model.LatLngBounds;

/**
 * An immutable class that represents the result of an API query.
 * Provides more information than a mere list (such as errors).
 */
public class ApiResult {
	/** Arcade locations returned by the query. */
	private final List<ArcadeLocation> locations;
	/** Geographical boundary covered by the query. */
	private final LatLngBounds bounds;
	
	private final int errorCode;
	/** Everything is OK; can use the locations list. */
	public static final int ERROR_NONE = 0;
	/** An unexpected API error has occurred. */
	public static final int ERROR_API = 1;
	/** The user has zoomed out past the valid API boundary box values. */
	public static final int ERROR_ZOOM = 2;
	
	public ApiResult(List<ArcadeLocation> locations, LatLngBounds bounds) {
		this.locations = locations;
		this.bounds = bounds;
		this.errorCode = ERROR_NONE;
	}
	
	public ApiResult(int errorCode) {
		this.locations = null;
		this.bounds = null;
		this.errorCode = errorCode;
	}

	public List<ArcadeLocation> getLocations() {
		return locations;
	}

	public int getErrorCode() {
		return errorCode;
	}
	
	public LatLngBounds getBounds() {
		return bounds;
	}
}
