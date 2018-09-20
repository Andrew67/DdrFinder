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

import android.arch.lifecycle.LifecycleOwner;
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
 * Custom ClusterRenderer class for arcade locations to handle setting up markers
 */
public class LocationClusterRenderer extends DefaultClusterRenderer<ArcadeLocation> {

    private final float defaultPinColor;
    private final float hasDDRPinColor;
    private final float selectedPinColor;

    private final LifecycleOwner lifecycleOwner;
    private final SelectedLocationModel selectedLocationModel;
    private final ClusterManager<ArcadeLocation> clusterManager;

    private int selectedLocationId = -1;

    public LocationClusterRenderer(FragmentActivity context, GoogleMap map,
                                   ClusterManager<ArcadeLocation> clusterManager) {
        super(context, map, clusterManager);
        defaultPinColor = ThemeUtil.getThemeColorHue(context.getTheme(), R.attr.pinColor);
        hasDDRPinColor = ThemeUtil.getThemeColorHue(context.getTheme(), R.attr.pinColorHasDDR);
        selectedPinColor = ThemeUtil.getThemeColorHue(context.getTheme(), R.attr.pinColorSelected);

        this.lifecycleOwner = context;
        this.selectedLocationModel = ViewModelProviders.of(context).get(SelectedLocationModel.class);
        this.clusterManager = clusterManager;
    }

    @Override
    public void onAdd() {
        super.onAdd();
        // Waiting until we're attached to the cluster manager is essential to avoid
        // an NPE during e.g. rotation if an arcade was already selected,
        // which would cause an early triggering of the event if based solely on activity lifecycle
        selectedLocationModel.getSelectedLocation().observe(lifecycleOwner, this::updateMarkers);
    }

    @Override
    public void onRemove() {
        super.onRemove();
        selectedLocationModel.getSelectedLocation().removeObserver(this::updateMarkers);
    }

    @Override
    protected void onBeforeClusterItemRendered(ArcadeLocation loc, MarkerOptions markerOptions) {
        markerOptions.icon(getIconForLocation(loc));
        markerOptions.alpha(getAlphaForLocation(loc));
        markerOptions.zIndex(getZIndexForLocation(loc));
    }

    /**
     * Updates the marker for the given location to the "selected" color,
     * then updates the previous location's marker back to the non-selected color, as appropriate
     */
    private void updateMarkers(@Nullable SelectedLocationModel.CompositeArcade selectedArcadeData) {
        selectedLocationId = (selectedArcadeData == null) ?
                -1 : selectedArcadeData.arcadeLocation.getId();

        for (Marker marker : clusterManager.getMarkerCollection().getMarkers()) {
            final ArcadeLocation location = getClusterItem(marker);
            if (location != null) {
                marker.setIcon(getIconForLocation(location));
                marker.setAlpha(getAlphaForLocation(location));
                marker.setZIndex(getZIndexForLocation(location));
            }
        }
    }

    /**
     * Set marker color to accent color or DDR location color,
     * or selected color if currently selected
     */
    private BitmapDescriptor getIconForLocation(ArcadeLocation loc) {
        float hue = defaultPinColor;
        if (loc.hasDDR()) hue = hasDDRPinColor;
        if (loc.getId() == selectedLocationId) hue = selectedPinColor;

        return BitmapDescriptorFactory.defaultMarker(hue);
    }

    /**
     * Set alpha to default if no arcades selected or is currently selected,
     * otherwise dim the marker
     */
    private float getAlphaForLocation(ArcadeLocation loc) {
        float alpha = 1.0f;
        if (selectedLocationId >= 0 && loc.getId() != selectedLocationId) alpha = 0.8f;

        return alpha;
    }

    /**
     * Set currently selected arcade to show its marker above all others
     */
    private float getZIndexForLocation(ArcadeLocation loc) {
        return loc.getId() == selectedLocationId ? 1 : 0;
    }

}
