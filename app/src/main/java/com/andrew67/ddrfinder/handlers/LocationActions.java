/*
 * Copyright (c) 2013-2016 Andrés Cordero
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

import com.andrew67.ddrfinder.R;
import com.andrew67.ddrfinder.activity.BrowserActivity;
import com.andrew67.ddrfinder.interfaces.ArcadeLocation;
import com.andrew67.ddrfinder.interfaces.DataSource;
import com.andrew67.ddrfinder.interfaces.MessageDisplay;
import com.andrew67.ddrfinder.model.v3.Source;
import com.google.android.gms.maps.model.LatLng;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Helper class for location actions.
 */
public class LocationActions {
	private final @NonNull ArcadeLocation location;
	private final @NonNull DataSource source;

    /**
     * Set up a new location action helper.
     * @param location Location to act upon.
     * @param source Source metadata for location. Pass null to use fallback.
     */
    public LocationActions(@NonNull ArcadeLocation location, @Nullable DataSource source) {
        this.location = location;
        this.source = (source != null) ? source : Source.getFallback();
    }

    /**
     * Copy the location's GPS coordinates to the context clipboard.
     * @param context The context which provides the clipboard service.
     * @param display Optional message display provider, to show "Copied" message.
     */
    public void copyGps(@NonNull Context context, @Nullable MessageDisplay display) {
        final LatLng coordinates = location.getPosition();
        final ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("gps",
                coordinates.latitude + ", " + coordinates.longitude));
        if (display != null) {
            display.showMessage(R.string.copy_complete);
        }
    }

    /**
     * Open a navigation intent with the location coordinates.
     * @param context The context which provides the ability to start activities.
     */
    public void navigate(@NonNull Context context) {
        final LatLng coordinates = location.getPosition();
        try {
            final String label = URLEncoder.encode(location.getName(), "UTF-8");
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("geo:" + coordinates.latitude + "," +
                            coordinates.longitude + "?q=" + coordinates.latitude +
                            "," + coordinates.longitude + "(" + label + ")")));
        } catch (UnsupportedEncodingException e) {
            // UTF-8 should always be a supported encoding
            e.printStackTrace();
        } catch (ActivityNotFoundException e) {
            // Thrown when user has no installed map applications that handle geo: URIs
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=com.google.android.apps.maps")));
        }
    }

    /**
     * Launches a web browser, pointed to the location's more information URL.
     * @param context The context which provides the ability to start activities.
     */
    public void moreInfo(@NonNull Context context) {
        final String infoURL = source.getInfoURL()
                .replace("${id}", "" + location.getId())
                .replace("${sid}", location.getSid());

        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(infoURL)));
        } catch (Exception e) {
            // Launch built-in WebView browser if there's an exception thrown attempting to launch a regular browser activity.
            Log.e("LocationActions", "Error launching Intent for HTTP(S) link; using built-in browser.", e);
            context.startActivity(new Intent(context, BrowserActivity.class)
                    .putExtra("url", infoURL)
                    .putExtra("title", location.getName()));
        }
    }
}