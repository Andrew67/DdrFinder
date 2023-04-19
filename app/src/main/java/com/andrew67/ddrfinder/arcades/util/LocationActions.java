/*
 * Copyright (c) 2013-2023 Andrés Cordero
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

import com.andrew67.ddrfinder.arcades.model.ArcadeLocation;
import com.andrew67.ddrfinder.arcades.model.DataSource;
import com.andrew67.ddrfinder.util.CustomTabsUtil;
import com.andrew67.ddrfinder.util.GeoUriBuilder;
import com.google.android.gms.maps.model.LatLng;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsSession;

import android.util.Log;

/**
 * Helper class for location actions.
 */
public class LocationActions {
	private final ArcadeLocation location;
	private final DataSource source;

    /**
     * Set up a new location action helper.
     * @param location Location to act upon.
     * @param source Source metadata for location. Pass null to use fallback.
     */
    public LocationActions(@NonNull ArcadeLocation location, @Nullable DataSource source) {
        this.location = location;
        this.source = (source != null) ? source : DataSource.getFallback();
    }

    /**
     * Copy the location's GPS coordinates to the context clipboard.
     * @param context The context which provides the clipboard service.
     * @return Success status of copying to clipboard. Can be used to show "Copied" message
     */
    public boolean copyGps(@NonNull Context context) {
        final LatLng coordinates = location.getPosition();
        final ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        boolean success = false;
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("gps",
                    coordinates.latitude + ", " + coordinates.longitude));
            success = true;
        }
        return success;
    }

    /**
     * Open a navigation intent with the location coordinates.
     * @param context The context which provides the ability to start activities.
     */
    public void navigate(@NonNull Context context) {
        final LatLng coordinates = location.getPosition();
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    GeoUriBuilder.showMarkerAt(coordinates.latitude, coordinates.longitude,
                            location.getName())));
        } catch (ActivityNotFoundException e) {
            // Thrown when user has no installed map applications that handle geo: URIs
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    GeoUriBuilder.googleMapsMarkerAt(coordinates.latitude, coordinates.longitude,
                            location.getName())));
        }
    }

    /**
     * Launches a web browser, pointed to the location's more information URL.
     * @param context The context which provides the ability to start activities.
     * @param useCustomTabs Whether to attempt to use a Chrome Custom Tab intent.
     * @param customTabsSession Optional session to attach to when using Chrome Custom Tabs.
     * @return Success status of copying to clipboard. Can be used to show error message
     */
    public boolean moreInfo(@NonNull Context context,
                            boolean useCustomTabs, @Nullable CustomTabsSession customTabsSession) {
        final String infoURL = source.getInfoURL()
                .replace("${id}", "" + location.getId())
                .replace("${sid}", location.getSid());

        try {
            CustomTabsUtil.launchUrl(context, infoURL, useCustomTabs, customTabsSession);
            return true;
        } catch (Exception e) {
            // TODO: User-visible error or built-in WebView solution
            Log.e("LocationActions", "Error launching Intent for HTTP(S) link", e);
            return false;
        }
    }
}
