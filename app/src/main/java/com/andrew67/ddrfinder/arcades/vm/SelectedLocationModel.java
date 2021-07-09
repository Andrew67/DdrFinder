/*
 * Copyright (c) 2018-2021 Andrés Cordero
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
import androidx.lifecycle.ViewModel;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.andrew67.ddrfinder.arcades.model.ArcadeLocation;
import com.andrew67.ddrfinder.arcades.model.DataSource;
import com.google.android.gms.maps.model.LatLng;

/**
 * Provides a shared store for the currently selected arcade location (map/list)
 */
public class SelectedLocationModel extends ViewModel {

    private final MutableLiveData<CompositeArcade> selectedLocation = new MutableLiveData<>();

    // Hold onto the most recently obtained user location in order to prevent UI flicker
    private LatLng previousUserLocation = null;

    @NonNull
    public LiveData<CompositeArcade> getSelectedLocation() {
        return selectedLocation;
    }

    /**
     * Update the currently selected location and its associated data source,
     * using the most recently available user location for the distance
     */
    public void setSelectedLocation(@NonNull ArcadeLocation arcadeLocation,
                                    @NonNull DataSource dataSource) {
        selectedLocation.setValue(
                new CompositeArcade(arcadeLocation, dataSource,
                        getDistanceKmBetween(previousUserLocation, arcadeLocation.getPosition())));
    }

    /**
     * Clear the currently selected location
     */
    public void clearSelectedLocation() {
        selectedLocation.setValue(null);
    }

    /**
     * Update the user's current location.
     * This will re-calculate distance to the currently selected arcade, then emit an updated obj.
     */
    public void updateUserLocation(@NonNull LatLng userLocation) {
        // Skip if the new location is the same as the previous one
        if (userLocation.equals(previousUserLocation)) return;
        previousUserLocation = userLocation;

        // Skip if no location is currently selected
        if (selectedLocation.getValue() == null) return;

        // Calculate the distance
        final LatLng arcadeLocationPosition = selectedLocation.getValue().arcadeLocation.getPosition();
        final float distanceKm = getDistanceKmBetween(userLocation, arcadeLocationPosition);

        // Update ViewModel with new instance that includes the updated distance
        selectedLocation.setValue(selectedLocation.getValue().cloneWithDistance(distanceKm));
    }

    /**
     * Calculates the distance in kilometers between two LatLng objects
     */
    private Float getDistanceKmBetween(@Nullable LatLng loc1, @Nullable LatLng loc2) {
        if (loc1 == null || loc2 == null) return null;

        final float[] results = new float[1]; // Implementation detail; distance in meters is [0]
        Location.distanceBetween(
                loc1.latitude, loc1.longitude,
                loc2.latitude, loc2.longitude,
                results
        );
        return results[0] / 1000;
    }

    /**
     * Represents an ArcadeLocation combined with the DataSource that corresponds to its "sid" value
     */
    public static final class CompositeArcade {
        public final ArcadeLocation arcadeLocation;
        public final DataSource dataSource;
        public final Float distanceKm;

        CompositeArcade(@NonNull ArcadeLocation arcadeLocation,
                        @NonNull DataSource dataSource,
                        @Nullable Float distanceKm) {
            this.arcadeLocation = arcadeLocation;
            this.dataSource = dataSource;
            this.distanceKm = distanceKm;
        }

        /**
         * Returns a cloned {@link CompositeArcade} instance with the given distance
         */
        CompositeArcade cloneWithDistance(@NonNull Float distanceKm) {
            return new CompositeArcade(arcadeLocation, dataSource, distanceKm);
        }
    }

}
