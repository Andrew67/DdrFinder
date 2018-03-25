/*
 * Copyright (c) 2013-2018 Andrés Cordero
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

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.SingleLiveEvent;
import android.arch.lifecycle.SnackbarMessage;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.NonNull;

import com.andrew67.ddrfinder.arcades.model.ApiResult;
import com.andrew67.ddrfinder.arcades.model.ArcadeLocation;
import com.andrew67.ddrfinder.arcades.model.DataSource;
import com.andrew67.ddrfinder.arcades.util.AttributionGenerator;
import com.andrew67.ddrfinder.arcades.util.CachedMapLoader;
import com.andrew67.ddrfinder.arcades.util.MapLoaderCallback;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.List;

/**
 * Provides all the data fields needed to show a loaded map of arcades,
 * based on a given bounds and data source.
 */
public class ArcadesModel extends ViewModel {
    // Data corresponding to last area requested
    private final MutableLiveData<List<ArcadeLocation>> arcadeLocations = new MutableLiveData<>();
    private final MutableLiveData<List<DataSource>> dataSources = new MutableLiveData<>();
    private final MutableLiveData<String> attribution = new MutableLiveData<>();

    // Fire once
    private final SingleLiveEvent<Boolean> inProgress = new SingleLiveEvent<>();
    private final SnackbarMessage errorMessage = new SnackbarMessage();

    private final CachedMapLoader cachedMapLoader = new CachedMapLoader();

    /** Get the arcade locations to show on the current map */
    public LiveData<List<ArcadeLocation>> getArcadeLocations() {
        return arcadeLocations;
    }

    /**
     * Get the data source information from the latest request.
     * This function should be refactored so that the UI code is not the one pulling out data
     * sources corresponding to loaded arcades
     */
    @Deprecated
    public LiveData<List<DataSource>> getDataSources() {
        return dataSources;
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
                                 boolean force) {
        cachedMapLoader.requestLocations(bounds, dataSrc, force, mapLoaderCallback);
    }

    private final MapLoaderCallback mapLoaderCallback = new MapLoaderCallback() {
        @Override
        public void onPreLoad() {
            inProgress.setValue(true);
        }

        @Override
        public void onLocationsLoaded(@NonNull ApiResult result) {
            arcadeLocations.setValue(result.getLocations());
            dataSources.setValue(result.getSources());
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
    };
}
