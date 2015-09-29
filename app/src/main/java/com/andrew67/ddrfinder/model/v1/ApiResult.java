/*
 * Copyright (c) 2013-2015 Andrés Cordero
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

package com.andrew67.ddrfinder.model.v1;

import java.util.Collections;
import java.util.List;

import com.andrew67.ddrfinder.interfaces.DataSource;
import com.google.android.gms.maps.model.LatLngBounds;

/**
 * An (almost) immutable class that represents the result of an API query.
 * Provides more information than a mere list (such as errors).
 */
public class ApiResult implements com.andrew67.ddrfinder.interfaces.ApiResult {
	/** Arcade locations returned by the query. */
	private final List<com.andrew67.ddrfinder.interfaces.ArcadeLocation> locations;
	/** Geographical boundary covered by the query. */
	private LatLngBounds bounds;
	
	private final int errorCode;
	/** Everything is OK; can use the locations list. */
	public static final int ERROR_NONE = 0;
	/** An unexpected API error has occurred. */
	public static final int ERROR_API = 1;
	/** The user has zoomed out past the valid API boundary box values. */
	public static final int ERROR_ZOOM = 2;
	
	public ApiResult(List<com.andrew67.ddrfinder.interfaces.ArcadeLocation> locations, LatLngBounds bounds) {
		this.locations = locations;
		this.bounds = bounds;
		this.errorCode = ERROR_NONE;
	}
	
	public ApiResult(int errorCode) {
		this.locations = null;
		this.bounds = null;
		this.errorCode = errorCode;
	}

	public List<com.andrew67.ddrfinder.interfaces.ArcadeLocation> getLocations() {
		return locations;
	}

	public Integer getErrorCode() {
		return errorCode;
	}
	
	public LatLngBounds getBounds() {
		return bounds;
	}

	public void setBounds(LatLngBounds bounds) {
		this.bounds = bounds;
	}

	public List<DataSource> getSources() {
		return Collections.<DataSource>singletonList(new ZivSource());
	}

	/**
	 * Custom class for returning a hardcoded data source, since V1 used Ziv exclusively.
	 */
	private static class ZivSource implements DataSource {

		@Override
		public String getName() {
			return "Zenius -I- vanisher.com";
		}

		@Override
		public String getInfoURL() {
			return "http://m.zenius-i-vanisher.com/arcadelocations_viewarcade.php?locationid=${sid}";
		}

		@Override
		public boolean hasDDR() {
			return false;
		}
	}
}
