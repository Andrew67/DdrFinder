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

package com.andrew67.ddrfinder.arcades.util;

import android.support.annotation.NonNull;
import android.util.Log;

import com.andrew67.ddrfinder.arcades.model.ApiResult;
import com.andrew67.ddrfinder.arcades.model.ArcadeLocation;
import com.andrew67.ddrfinder.arcades.model.DataSource;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads arcades for given bounds box and data source using the DDR Finder API on the server.
 * As locations come in, results are cached and re-used for locations already loaded before
 */
public class CachedMapLoader {
    private static final String TAG = CachedMapLoader.class.getSimpleName();
    private final List<ApiResult> resultsCache = Collections
            .synchronizedList(new ArrayList<ApiResult>());

    // Singleton pattern
    private CachedMapLoader() { }
    private static CachedMapLoader instance = null;
    public static CachedMapLoader getInstance() {
        if (instance != null) return instance;
        return instance = new CachedMapLoader();
    }

    public void requestLocations(@NonNull LatLngBounds bounds,
                                 @NonNull String dataSrc,
                                 boolean force,
                                 final @NonNull MapLoaderCallback callback) {
        callback.onPreLoad();

        if (!force) {
            final ApiResult cachedApiResult = alreadyLoaded(bounds, dataSrc);
            if (cachedApiResult != null) {
                Log.d(TAG, "Cache HIT!");
                callback.onLocationsLoaded(cachedApiResult);
                callback.onFinish();
                return;
            }
        }

        // force || cachedApiResult == null

        // Preload slightly larger bounds when zoomed in, for a smoother pan/zoom experience
        // as network requests are reduced.
        if (Math.abs(bounds.northeast.latitude - bounds.southwest.latitude) < 0.5
                && Math.abs(bounds.northeast.longitude - bounds.southwest.longitude) < 0.5) {
            bounds = LatLngBounds.builder()
                    .include(new LatLng(bounds.northeast.latitude + 0.125, bounds.northeast.longitude + 0.125))
                    .include(new LatLng(bounds.southwest.latitude - 0.125, bounds.southwest.longitude - 0.125))
                    .build();
        }

        new NetworkMapLoader(dataSrc, new MapLoaderCallback() {
            @Override
            public void onPreLoad() {
                // Do nothing
            }

            @Override
            public void onLocationsLoaded(@NonNull ApiResult result) {
                resultsCache.add(result);
                callback.onLocationsLoaded(result);
            }

            @Override
            public void onError(int errorCode, int errorMessageResourceId) {
                callback.onError(errorCode, errorMessageResourceId);
            }

            @Override
            public void onFinish() {
                callback.onFinish();
            }
        }).execute(bounds);
    }

    /**
     * Test whether the given boundaries have already been loaded for the given data source
     * @return A merged "API result" (if all corners are cached), or null
     */
    private ApiResult alreadyLoaded(LatLngBounds box, String dataSrc) {
        // Test all four corners (best we can do)
        final LatLng northeast = box.northeast;
        final LatLng southwest = box.southwest;
        final LatLng northwest = new LatLng(northeast.latitude, southwest.longitude);
        final LatLng southeast = new LatLng(southwest.latitude, northeast.longitude);

        boolean loaded = false;
        boolean loadedNE = false;
        boolean loadedSW = false;
        boolean loadedNW = false;
        boolean loadedSE = false;

        final Set<DataSource> cachedSources = new HashSet<>();
        final Set<ArcadeLocation> cachedArcadeLocations = new HashSet<>();

        synchronized (resultsCache) {
            for (ApiResult result : resultsCache) {
                // Check if the result record contains results including the given data source
                boolean containsDataSource = false;
                for (DataSource source : result.getSources()) {
                    if (source.getShortName().equals(dataSrc)) {
                        containsDataSource = true;
                        cachedSources.add(source);
                    }
                }

                // Check if the result bounds include the requested box,
                // and add locations as we go if they do.
                if (containsDataSource) {
                    final LatLngBounds bounds = result.getBounds();

                    if (bounds.contains(northeast) || bounds.contains(southwest) ||
                            bounds.contains(northwest) || bounds.contains(southeast)) {
                        for (ArcadeLocation location : result.getLocations()) {
                            if (box.contains(location.getPosition()))
                                cachedArcadeLocations.add(location);
                        }

                        if (bounds.contains(northeast)) loadedNE = true;
                        if (bounds.contains(southwest)) loadedSW = true;
                        if (bounds.contains(northwest)) loadedNW = true;
                        if (bounds.contains(southeast)) loadedSE = true;
                    }

                    if (loadedNE && loadedSW && loadedNW && loadedSE) break;
                }
            }
        }
        if (loadedNE && loadedSW && loadedNW && loadedSE) loaded = true;

        if (loaded) return new ApiResult(new ArrayList<>(cachedSources),
                new ArrayList<>(cachedArcadeLocations), box);
        else return null;
    }
}
