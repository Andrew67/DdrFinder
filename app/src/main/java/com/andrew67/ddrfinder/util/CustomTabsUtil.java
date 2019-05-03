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

package com.andrew67.ddrfinder.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;

import com.andrew67.ddrfinder.R;

import java.util.List;

/**
 * Provides helper methods for dealing with the Custom Tabs Client library.
 */
public class CustomTabsUtil {

    /**
     * Launches the given url from the given context in a custom tab, using our app's primary color.
     * Will throw an {@link android.content.ActivityNotFoundException} if there is no browser installed.
     * @param context Context to launch from.
     * @param url URL to launch.
     * @param useCustomTabs Whether to attempt to use a custom tab or not (user preference).
     */
    public static void launchUrl(@NonNull Context context, @NonNull String url, boolean useCustomTabs) {
        final Uri uri = Uri.parse(url);

        if (!useCustomTabs) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } else {
            final CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .addDefaultShareMenuItem()
                    .setToolbarColor(ThemeUtil.getThemeColor(
                            context.getTheme(), R.attr.colorPrimary))
                    .build();

            // Chrome detection recipe based on http://stackoverflow.com/a/32656019
            // Otherwise, setPackage is not called, and falls back to user-selected browser.
            final String CHROME_PACKAGE_NAME = "com.android.chrome";
            customTabsIntent.intent.setData(uri);
            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(
                    customTabsIntent.intent, PackageManager.MATCH_DEFAULT_ONLY);

            for (ResolveInfo resolveInfo : resolveInfoList) {
                if (CHROME_PACKAGE_NAME.equals(resolveInfo.activityInfo.packageName)) {
                    customTabsIntent.intent.setPackage(CHROME_PACKAGE_NAME);
                    break;
                }
            }

            customTabsIntent.launchUrl(context, uri);
        }
    }

}
