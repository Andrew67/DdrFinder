/*
 * Copyright (c) 2013-2019 Andrés Cordero
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

import com.andrew67.ddrfinder.activity.BrowserActivity;
import com.andrew67.ddrfinder.arcades.model.ArcadeLocation;
import com.andrew67.ddrfinder.arcades.model.DataSource;
import com.andrew67.ddrfinder.util.Analytics;
import com.andrew67.ddrfinder.util.CustomTabsUtil;
import com.andrew67.ddrfinder.util.GeoUriBuilder;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.analytics.FirebaseAnalytics;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
        FirebaseAnalytics.getInstance(context)
                .logEvent(Analytics.Event.LOCATION_ACTION_COPYGPS, null);

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
        FirebaseAnalytics.getInstance(context)
                .logEvent(Analytics.Event.LOCATION_ACTION_NAVIGATE, null);

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
     */
    public void moreInfo(@NonNull Context context, boolean useCustomTabs) {
        FirebaseAnalytics.getInstance(context)
                .logEvent(Analytics.Event.LOCATION_ACTION_MOREINFO, null);

        final String infoURL = source.getInfoURL()
                .replace("${id}", "" + location.getId())
                .replace("${sid}", location.getSid());

        try {
            CustomTabsUtil.launchUrl(context, infoURL, useCustomTabs);
        } catch (Exception e) {
            final Bundle params = new Bundle();
            params.putString(Analytics.Param.EXCEPTION_MESSAGE, e.getMessage());
            FirebaseAnalytics.getInstance(context)
                    .logEvent(Analytics.Event.LOCATION_MOREINFO_EXCEPTION, params);

            // Launch built-in WebView browser if there's an exception thrown attempting to launch a regular browser activity.
            Log.e("LocationActions", "Error launching Intent for HTTP(S) link; using built-in browser.", e);
            BrowserActivity.start(context, infoURL);
        }
    }
}
