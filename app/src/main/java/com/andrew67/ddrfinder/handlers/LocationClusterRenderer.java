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

package com.andrew67.ddrfinder.handlers;

import android.content.Context;
import android.os.Build;

import com.andrew67.ddrfinder.interfaces.ArcadeLocation;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

/**
 * Custom ClusterRenderer class for arcade locations to handle setting up marker color, info window, etc.
 */
public class LocationClusterRenderer extends DefaultClusterRenderer<ArcadeLocation> {

    public LocationClusterRenderer(Context context, GoogleMap map, ClusterManager<ArcadeLocation> clusterManager) {
        super(context, map, clusterManager);
    }

    @Override
    protected void onBeforeClusterItemRendered(ArcadeLocation loc, MarkerOptions markerOptions) {
        // Set default marker color to red.
        float hue = BitmapDescriptorFactory.HUE_RED;

        // Set the default to purple on Material devices to match accent color.
        if (Build.VERSION.SDK_INT >= 21) {
            hue = BitmapDescriptorFactory.HUE_VIOLET;
        }

        // Has the location been tagged as closed? Mark orange.
        if (loc.isClosed()) {
            hue = BitmapDescriptorFactory.HUE_ORANGE;
        }
        // Does the location have a DDR machine? Mark blue.
        else if (loc.hasDDR()) {
            hue = BitmapDescriptorFactory.HUE_AZURE;
        }

        markerOptions.title(loc.getName())
                .icon(BitmapDescriptorFactory.defaultMarker(hue));
        if (!"".equals(loc.getCity())) markerOptions.snippet(loc.getCity());
    }

}
