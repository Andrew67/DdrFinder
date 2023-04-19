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

package com.andrew67.ddrfinder.util;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsSession;

import com.andrew67.ddrfinder.R;

import org.chromium.customtabsdemos.CustomTabActivityHelper;
import org.chromium.customtabsdemos.WebviewFallback;

/**
 * Provides helper methods for dealing with the Custom Tabs Client library.
 */
public class CustomTabsUtil {

    /**
     * Launches the given url from the given context in a custom tab, using our app's primary color.
     * Will throw an {@link android.content.ActivityNotFoundException} if there is no browser installed.
     * @param activity Activity to launch from.
     * @param url URL to launch.
     * @param useCustomTabs Whether to attempt to use a custom tab or not (user preference).
     * @param customTabsSession When using custom tabs, optional session to attach to.
     */
    public static void launchUrl(@NonNull Activity activity, @NonNull String url,
                                 boolean useCustomTabs, @Nullable CustomTabsSession customTabsSession) {
        final Uri uri = Uri.parse(url);

        if (!useCustomTabs) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } else {
            final Resources.Theme theme = activity.getTheme();
            final CustomTabColorSchemeParams lightParams = new CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(ThemeUtil.getThemeColor(
                            theme, R.attr.colorPrimaryContainerLightTheme))
                    .setNavigationBarColor(ThemeUtil.getThemeColor(
                            theme, R.attr.colorSurfaceLightTheme))
                    .build();
            final CustomTabColorSchemeParams darkParams = new CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(ThemeUtil.getThemeColor(
                            theme, R.attr.colorPrimaryContainerDarkTheme))
                    .setNavigationBarColor(ThemeUtil.getThemeColor(
                            theme, R.attr.colorSurfaceDarkTheme))
                    .build();
            // For browsers that don't handle color schemes yet, send based on current theme
            final CustomTabColorSchemeParams defaultParams = new CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(ThemeUtil.getThemeColor(
                            theme, com.google.android.material.R.attr.colorPrimaryContainer))
                    .setNavigationBarColor(ThemeUtil.getThemeColor(
                            theme, com.google.android.material.R.attr.colorSurface))
                    .build();

            final CustomTabsIntent.Builder customTabsIntentBuilder = new CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                    .setColorScheme(ThemeUtil.getCustomTabsColorScheme())
                    .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_LIGHT, lightParams)
                    .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, darkParams)
                    .setDefaultColorSchemeParams(defaultParams);
            if (customTabsSession != null) customTabsIntentBuilder.setSession(customTabsSession);
            final CustomTabsIntent customTabsIntent = customTabsIntentBuilder.build();

            CustomTabActivityHelper.openCustomTab(activity, customTabsIntent,
                    uri, new WebviewFallback());
        }
    }

}
