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
package com.andrew67.ddrfinder;

import org.piwik.sdk.DownloadTracker;
import org.piwik.sdk.PiwikApplication;
import org.piwik.sdk.TrackHelper;

import timber.log.Timber;

public class DdrFinderApp extends PiwikApplication {
    @Override
    public String getTrackerUrl() {
        return "https://analytics.andrew67.com/piwik/piwik.php";
    }

    @Override
    public Integer getSiteId() {
        return 3;
    }

    // From demo at https://github.com/piwik/piwik-sdk-android/blob/master/exampleapp/src/main/java/com/piwik/demo/DemoApp.java
    @Override
    public void onCreate() {
        super.onCreate();
        initPiwik();
    }


    private void initPiwik() {
        // Print debug output when working on an app.
        Timber.plant(new Timber.DebugTree());

        // When working on an app we don't want to skew tracking results.
        getPiwik().setDryRun(BuildConfig.DEBUG);

        // Track this app install, this will only trigger once per app version.
        // i.e. "http://com.piwik.demo:1/185DECB5CFE28FDB2F45887022D668B4"
        TrackHelper.track().download().identifier(DownloadTracker.Extra.APK_CHECKSUM).with(getTracker());
    }
}
