/*
 * Copyright (c) 2013 Luis Torres
 * Web: https://github.com/ltorres8890/Clima
 *
 * Copyright (c) 2015-2018 Andrés Cordero
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

package com.andrew67.ddrfinder.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.andrew67.ddrfinder.R;
import com.andrew67.ddrfinder.util.Analytics;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Arrays;

// Based on https://developer.android.com/guide/topics/ui/settings.html#Fragment
public class SettingsActivity extends AppCompatActivity {

    public static final String KEY_PREF_API_SRC = "api_src";
    public static final String API_SRC_CUSTOM = "custom";

    public static final String KEY_PREF_ANALYTICS = "analyticsEnabled";
    public static final String KEY_PREF_CUSTOMTABS = "customtabs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.action_settings);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        private FirebaseAnalytics firebaseAnalytics;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            firebaseAnalytics = FirebaseAnalytics.getInstance(requireActivity());
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Load the preferences from an XML resource
            setPreferencesFromResource(R.xml.preferences, rootKey);

            // Set preference summaries to current values
            final SharedPreferences sharedPref = getPreferenceScreen().getSharedPreferences();
            findPreference(KEY_PREF_API_SRC).setSummary(
                    getPrefSummary(R.array.settings_src_entryValues, R.array.settings_src_entries,
                            sharedPref.getString(KEY_PREF_API_SRC, "")));
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPref,
                                              String key) {
            final Preference pref = findPreference(key);
            if (pref != null) {
                switch (key) {
                    case KEY_PREF_API_SRC:
                        String newSrc = sharedPref.getString(key, "");
                        pref.setSummary(getPrefSummary(R.array.settings_src_entryValues, R.array.settings_src_entries,
                                newSrc));
                        trackPreferenceChanged(key, newSrc);
                        break;
                    case KEY_PREF_CUSTOMTABS:
                        boolean customTabsEnabled = sharedPref.getBoolean(key, true);
                        trackPreferenceChanged(key, Boolean.toString(customTabsEnabled));
                        break;
                    case KEY_PREF_ANALYTICS:
                        // Set changes to analytics option to set persistent opt-out flag in Firebase
                        boolean analyticsEnabled = sharedPref.getBoolean(key, true);
                        firebaseAnalytics.setAnalyticsCollectionEnabled(analyticsEnabled);
                        break;
                }
            }
        }

        /**
         * Find the user-friendly description of a ListPreference value key
         * @param keys ID of keys array
         * @param values ID of user-friendly values array
         * @param key Current key
         * @return User-friendly value description, or the key itself if not found
         */
        private String getPrefSummary(int keys, int values, String key) {
            String[] keys_arr = getResources().getStringArray(keys);
            String[] values_arr = getResources().getStringArray(values);
            int idx = Arrays.asList(keys_arr).indexOf(key);
            if (idx == -1) return key;
            else return values_arr[idx];
        }

        /**
         * Track a preference change in Firebase Analytics (when analytics are on).
         * @param key Preference key (use one of the constants).
         * @param value String value (may require casting).
         */
        private void trackPreferenceChanged(@NonNull String key, @NonNull String value) {
            Bundle params = new Bundle();
            params.putString(Analytics.Param.PREFERENCE_KEY, key);
            params.putString(Analytics.Param.PREFERENCE_VALUE, value);
            firebaseAnalytics.logEvent(Analytics.Event.SET_PREFERENCE, params);
        }
    }
}
