/*
 * Copyright (c) 2018-2019 Andrés Cordero
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

package com.andrew67.ddrfinder.arcades.vm;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SingleLiveEvent;
import androidx.lifecycle.SnackbarMessage;
import androidx.lifecycle.ViewModel;
import androidx.annotation.NonNull;
import android.util.Log;

import com.andrew67.ddrfinder.arcades.model.ApiResult;
import com.andrew67.ddrfinder.arcades.model.ArcadeLocation;
import com.andrew67.ddrfinder.arcades.model.DataSource;
import com.andrew67.ddrfinder.arcades.util.AttributionGenerator;
import com.andrew67.ddrfinder.arcades.util.CachedMapLoader;
import com.andrew67.ddrfinder.arcades.util.MapLoaderCallback;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides all the data fields needed to show a loaded map of arcades,
 * based on a given bounds and data source.
 */
public class ArcadesModel extends ViewModel {
    // Data corresponding to last area requested
    private final MutableLiveData<List<ArcadeLocation>> arcadeLocations = new MutableLiveData<>();
    private final Map<String, DataSource> dataSources = new HashMap<>();
    private final MutableLiveData<String> attribution = new MutableLiveData<>();

    // Fire once
    private final SingleLiveEvent<Boolean> inProgress = new SingleLiveEvent<>();
    private final SnackbarMessage errorMessage = new SnackbarMessage();

    private final CachedMapLoader cachedMapLoader = CachedMapLoader.getInstance();

    /** Get the arcade locations to show on the current map */
    public LiveData<List<ArcadeLocation>> getArcadeLocations() {
        return arcadeLocations;
    }

    /** Get the attribution to show on the current map */
    public LiveData<String> getAttribution() {
        return attribution;
    }

    /** Get the progress state of the map loader request. This is an indeterminate bar */
    public LiveData<Boolean> getProgress() {
        return inProgress;
    }

    /** Get the most recent error message. Fires once when it actually occurs */
    public SnackbarMessage getErrorMessage() {
        return errorMessage;
    }

    /**
     * Request arcade locations for the given bounds box and data source.
     * Updates the various data fields during the process
     * @param force Whether to force a network request (e.g. user clicked Reload button)
     */
    public void requestLocations(@NonNull LatLngBounds bounds,
                                 @NonNull String dataSrc,
                                 boolean hasDDROnly,
                                 boolean force) {
        cachedMapLoader.requestLocations(bounds, dataSrc, force, new MapLoaderCallback() {
            @Override
            public void onPreLoad() {
                inProgress.setValue(true);
            }

            @Override
            public void onLocationsLoaded(@NonNull ApiResult result) {
                List<ArcadeLocation> locations = result.getLocations();
                if (hasDDROnly) {
                    locations = new ArrayList<>();
                    for (ArcadeLocation location : result.getLocations()) {
                        if (location.hasDDR()) locations.add(location);
                    }
                    Log.d("ArcadesModel", "DDR filter enabled; filtered " +
                            (result.getLocations().size() - locations.size()) + " locations");
                }
                arcadeLocations.setValue(locations);

                dataSources.clear();
                for (DataSource src : result.getSources()) dataSources.put(src.getShortName(), src);

                attribution.setValue(AttributionGenerator.fromSources(result.getSources()));
            }

            @Override
            public void onError(int errorCode, int errorMessageResourceId) {
                errorMessage.setValue(errorMessageResourceId);
            }

            @Override
            public void onFinish() {
                inProgress.setValue(false);
            }
        });
    }

    /**
     * Fetches the DataSource for the given arcade location.
     * If specific source not found, "fallback" source is returned
     */
    @NonNull
    public DataSource getSource(@NonNull ArcadeLocation location) {
        if (dataSources.containsKey(location.getSrc()))
            return dataSources.get(location.getSrc());
        else {
            Log.w("ArcadesModel", "Failed to get source: " + location.getSrc());
            if (dataSources.containsKey("fallback")) return dataSources.get("fallback");
            else {
                Log.w("ArcadesModel", "Failed to get fallback source");
                return DataSource.getFallback();
            }
        }
    }
}
