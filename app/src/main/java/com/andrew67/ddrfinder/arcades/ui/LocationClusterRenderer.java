/*
 * Copyright (c) 2016-2020 Andrés Cordero
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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

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

    private final Bitmap defaultPin;
    private final Bitmap selectedPin;

    private final Bitmap ddrPin;
    private final Bitmap ddrSelectedPin;

    private final LifecycleOwner lifecycleOwner;
    private final SelectedLocationModel selectedLocationModel;
    private final ClusterManager<ArcadeLocation> clusterManager;

    private int selectedLocationId = -1;

    public LocationClusterRenderer(FragmentActivity context, GoogleMap map,
                                   ClusterManager<ArcadeLocation> clusterManager) {
        super(context, map, clusterManager);

        this.lifecycleOwner = context;
        this.selectedLocationModel = new ViewModelProvider(context).get(SelectedLocationModel.class);
        this.clusterManager = clusterManager;

        final Resources.Theme theme = context.getTheme();
        final int defaultPinColor = ThemeUtil.getThemeColor(theme, R.attr.colorPrimary);
        final int selectedPinColor = ThemeUtil.getThemeColor(theme, R.attr.colorAccent);
        final int iconColor = ThemeUtil.getThemeColor(theme, R.attr.colorOnPrimary);

        defaultPin = getBitmapFromVector(context,
                defaultPinColor,
                R.drawable.ic_arcade_black_16dp, iconColor);
        selectedPin = getBitmapFromVector(context,
                selectedPinColor,
                R.drawable.ic_arcade_black_16dp, iconColor);

        ddrPin = getBitmapFromVector(context,
                defaultPinColor,
                R.drawable.ic_arrow_black_20dp, iconColor);
        ddrSelectedPin = getBitmapFromVector(context,
                selectedPinColor,
                R.drawable.ic_arrow_black_20dp, iconColor);
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
            }
        }
    }

    /**
     * Set marker color to accent color or DDR location color,
     * or selected color if currently selected
     */
    private BitmapDescriptor getIconForLocation(ArcadeLocation loc) {
        Bitmap bitmap = defaultPin;
        if (loc.getId() == selectedLocationId) bitmap = loc.hasDDR() ? ddrSelectedPin : selectedPin;
        else if (loc.hasDDR()) bitmap = ddrPin;

        return BitmapDescriptorFactory.fromBitmap(bitmap);
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
     * Generates a bitmap for the given vector atop the marker, with the given colors.
     * Based on code snippet from https://stackoverflow.com/questions/42365658/custom-marker-in-google-maps-in-android-with-vector-asset-icon/48356646#48356646
     */
    private static Bitmap getBitmapFromVector(Context context, @ColorInt int markerColor,
                                              @DrawableRes int icon, @ColorInt int iconColor) {
        final Drawable background = ContextCompat.getDrawable(context,
                R.drawable.ic_map_marker_black_32dp);
        assert background != null;
        background.setBounds(0, 0,
                background.getIntrinsicWidth(), background.getIntrinsicHeight());
        DrawableCompat.setTint(background, markerColor);

        final Drawable vectorDrawable = ContextCompat.getDrawable(context, icon);
        assert vectorDrawable != null;
        final int left = (background.getIntrinsicWidth() - vectorDrawable.getIntrinsicWidth()) / 2;
        final int top = (background.getIntrinsicHeight() - vectorDrawable.getIntrinsicHeight()) / 3;
        vectorDrawable.setBounds(left, top,
                left + vectorDrawable.getIntrinsicWidth(),
                top + vectorDrawable.getIntrinsicHeight());
        DrawableCompat.setTint(vectorDrawable, iconColor);

        final Bitmap bitmap = Bitmap.createBitmap(background.getIntrinsicWidth(),
                background.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        background.draw(canvas);
        vectorDrawable.draw(canvas);
        return bitmap;
    }

}
