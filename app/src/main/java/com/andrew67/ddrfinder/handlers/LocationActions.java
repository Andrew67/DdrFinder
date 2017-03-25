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
import com.andrew67.ddrfinder.util.ThemeUtil;
import com.google.android.gms.maps.model.LatLng;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.util.Log;

import org.piwik.sdk.TrackHelper;
import org.piwik.sdk.Tracker;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 * Helper class for location actions.
 */
public class LocationActions {
	private final @NonNull ArcadeLocation location;
	private final @NonNull DataSource source;
    private final @Nullable Tracker tracker;

    /**
     * Set up a new location action helper.
     * @param location Location to act upon.
     * @param source Source metadata for location. Pass null to use fallback.
     * @param tracker Piwik tracker object. Pass null to skip event tracking.
     */
    public LocationActions(@NonNull ArcadeLocation location, @Nullable DataSource source,
                           @Nullable Tracker tracker) {
        this.location = location;
        this.source = (source != null) ? source : Source.getFallback();
        this.tracker = tracker;
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

        if (tracker != null) {
            TrackHelper.track().event("LocationActions", "copyGPS").with(tracker);
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

            if (tracker != null) {
                TrackHelper.track().event("LocationActions", "navigate").name("success").with(tracker);
            }
        } catch (UnsupportedEncodingException e) {
            // UTF-8 should always be a supported encoding
            e.printStackTrace();
        } catch (ActivityNotFoundException e) {
            // Thrown when user has no installed map applications that handle geo: URIs
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=com.google.android.apps.maps")));

            if (tracker != null) {
                TrackHelper.track().event("LocationActions", "navigate").name("ActivityNotFoundException").with(tracker);
            }
        }
    }

    /**
     * Launches a web browser, pointed to the location's more information URL.
     * @param context The context which provides the ability to start activities.
     * @param useCustomTabs Whether to attempt to use a Chrome Custom Tab intent.
     */
    public void moreInfo(@NonNull Context context, boolean useCustomTabs) {
        final String infoURL = source.getInfoURL()
                .replace("${id}", "" + location.getId())
                .replace("${sid}", location.getSid());

        try {
            final Uri infoURI = Uri.parse(infoURL);

            if (!useCustomTabs) {
                context.startActivity(new Intent(Intent.ACTION_VIEW, infoURI));
            } else {
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder.setToolbarColor(ThemeUtil.getThemeColor(context.getTheme(), android.R.attr.actionModeBackground));
                }
                builder.setShowTitle(true)
                        .addDefaultShareMenuItem()
                        .setCloseButtonIcon(BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.ic_arrow_back_white_24dp))
                        .setStartAnimations(context, R.anim.slide_in_right, R.anim.slide_out_left)
                        .setExitAnimations(context, R.anim.slide_in_left, R.anim.slide_out_right);
                CustomTabsIntent customTabsIntent = builder.build();

                // Chrome detection recipe based on http://stackoverflow.com/a/32656019
                // Otherwise, setPackage is not called, and falls back to user-selected browser.
                final String CHROME_PACKAGE_NAME = "com.android.chrome";
                customTabsIntent.intent.setData(infoURI);
                PackageManager packageManager = context.getPackageManager();
                List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(
                        customTabsIntent.intent, PackageManager.MATCH_DEFAULT_ONLY);

                for (ResolveInfo resolveInfo : resolveInfoList) {
                    if (CHROME_PACKAGE_NAME.equals(resolveInfo.activityInfo.packageName)) {
                        customTabsIntent.intent.setPackage(CHROME_PACKAGE_NAME);
                        break;
                    }
                }

                customTabsIntent.launchUrl(context, infoURI);
            }

            if (tracker != null) {
                TrackHelper.track().event("LocationActions", "moreInfo").name("success").with(tracker);
            }
        } catch (Exception e) {
            // Launch built-in WebView browser if there's an exception thrown attempting to launch a regular browser activity.
            Log.e("LocationActions", "Error launching Intent for HTTP(S) link; using built-in browser.", e);
            context.startActivity(new Intent(context, BrowserActivity.class)
                    .putExtra("url", infoURL)
                    .putExtra("title", location.getName()));

            if (tracker != null) {
                TrackHelper.track().event("LocationActions", "moreInfo").name(e.getClass().getName()).with(tracker);
            }
        }
    }
}
