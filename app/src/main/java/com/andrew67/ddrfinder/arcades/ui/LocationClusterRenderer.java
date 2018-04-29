/*
 * Copyright (c) 2016-2018 Andrés Cordero
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

package com.andrew67.ddrfinder.arcades.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import com.andrew67.ddrfinder.R;
import com.andrew67.ddrfinder.arcades.model.ArcadeLocation;
import com.andrew67.ddrfinder.arcades.vm.SelectedLocationModel;
import com.andrew67.ddrfinder.util.ThemeUtil;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

/**
 * Custom ClusterRenderer class for arcade locations to handle setting up marker color
 */
public class LocationClusterRenderer extends DefaultClusterRenderer<ArcadeLocation> {

    private final float defaultPinColor;
    private final float hasDDRPinColor;
    private final float selectedPinColor;

    private final SelectedLocationModel selectedLocationModel;
    private ArcadeLocation previousSelectedLocation;

    public LocationClusterRenderer(FragmentActivity context, GoogleMap map,
                                   ClusterManager<ArcadeLocation> clusterManager) {
        super(context, map, clusterManager);
        defaultPinColor = ThemeUtil.getThemeColorHue(context.getTheme(), R.attr.pinColor);
        hasDDRPinColor = ThemeUtil.getThemeColorHue(context.getTheme(), R.attr.pinColorHasDDR);
        selectedPinColor = ThemeUtil.getThemeColorHue(context.getTheme(), R.attr.pinColorSelected);

        selectedLocationModel = ViewModelProviders.of(context).get(SelectedLocationModel.class);
        selectedLocationModel.getSelectedLocation().observeForever(this::updateSelectedMarker);
    }

    @Override
    protected void onBeforeClusterItemRendered(ArcadeLocation loc, MarkerOptions markerOptions) {
        markerOptions.icon(getIconForLocation(loc));
    }

    /**
     * Updates the marker for the given location to the "selected" color,
     * then updates the previous location's marker back to the non-selected color, as appropriate
     */
    private void updateSelectedMarker(@Nullable SelectedLocationModel.CompositeArcade selectedArcadeData) {
        if (selectedArcadeData == null) return;
        final ArcadeLocation selectedLocation = selectedArcadeData.arcadeLocation;
        if (selectedLocation.equals(previousSelectedLocation)) return;

        final Marker marker = getMarker(selectedLocation);
        if (marker != null) marker.setIcon(getIconForLocation(selectedLocation));

        if (previousSelectedLocation != null) {
            final Marker previousMarker = getMarker(previousSelectedLocation);
            if (previousMarker != null)
                previousMarker.setIcon(getIconForLocation(previousSelectedLocation));
        }

        previousSelectedLocation = selectedLocation;
    }

    private BitmapDescriptor getIconForLocation(ArcadeLocation loc) {
        // Set marker color to accent color or DDR location color,
        // or selected color if currently selected
        float hue = defaultPinColor;
        if (loc.hasDDR()) hue = hasDDRPinColor;

        final SelectedLocationModel.CompositeArcade selectedArcade =
                selectedLocationModel.getSelectedLocation().getValue();
        if (selectedArcade != null && selectedArcade.arcadeLocation.getId() == loc.getId())
            hue = selectedPinColor;

        return BitmapDescriptorFactory.defaultMarker(hue);
    }

}
