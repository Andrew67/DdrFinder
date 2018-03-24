/*
 * Copyright (c) 2016 Andrés Cordero
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

import android.content.Context;

import com.andrew67.ddrfinder.R;
import com.andrew67.ddrfinder.arcades.model.ArcadeLocation;
import com.andrew67.ddrfinder.util.ThemeUtil;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

/**
 * Custom ClusterRenderer class for arcade locations to handle setting up marker color, info window, etc.
 */
public class LocationClusterRenderer extends DefaultClusterRenderer<ArcadeLocation> {

    private final float defaultPinColor;
    private final float hasDDRPinColor;

    public LocationClusterRenderer(Context context, GoogleMap map, ClusterManager<ArcadeLocation> clusterManager) {
        super(context, map, clusterManager);
        defaultPinColor = ThemeUtil.getThemeColorHue(context.getTheme(), R.attr.pinColor);
        hasDDRPinColor = ThemeUtil.getThemeColorHue(context.getTheme(), R.attr.pinColorHasDDR);
    }

    @Override
    protected void onBeforeClusterItemRendered(ArcadeLocation loc, MarkerOptions markerOptions) {
        // Set marker color to accent color or DDR location color.
        float hue = loc.hasDDR() ? hasDDRPinColor : defaultPinColor;

        markerOptions.title(loc.getName())
                .icon(BitmapDescriptorFactory.defaultMarker(hue));
        if (!"".equals(loc.getCity())) markerOptions.snippet(loc.getCity());
    }

}
