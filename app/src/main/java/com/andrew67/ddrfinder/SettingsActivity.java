/*
 * Copyright (c) 2013 Luis Torres
 * Web: https://github.com/ltorres8890/Clima
 *
 * Copyright (c) 2015 Andrés Cordero
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

import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import java.util.Arrays;

// Based on https://developer.android.com/guide/topics/ui/settings.html#Fragment
public class SettingsActivity extends Activity {

    public static final String KEY_PREF_API_SRC = "api_src";
    public static final String KEY_PREF_API_SRC_CUSTOM = "api_src_custom";
    public static final String KEY_PREF_API_URL = "api_endpoint";
    public static final String KEY_PREF_API_VERSION = "api_version";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.action_settings);
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            // Set preference summaries to current values
            final SharedPreferences sharedPref = getPreferenceScreen().getSharedPreferences();

            Preference pref = findPreference(KEY_PREF_API_URL);
            pref.setSummary(sharedPref.getString(KEY_PREF_API_URL, ""));

            pref = findPreference(KEY_PREF_API_SRC_CUSTOM);
            pref.setSummary(sharedPref.getString(KEY_PREF_API_SRC_CUSTOM, ""));

            pref = findPreference(KEY_PREF_API_SRC);
            pref.setSummary(getPrefSummary(R.array.settings_src_entryValues, R.array.settings_src_entries,
                    sharedPref.getString(KEY_PREF_API_SRC, "")));

            pref = findPreference(KEY_PREF_API_VERSION);
            pref.setSummary(getPrefSummary(R.array.settings_api_version_entryValues, R.array.settings_api_version_entries,
                    sharedPref.getString(KEY_PREF_API_VERSION, "")));
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
                    case KEY_PREF_API_SRC_CUSTOM:
                    case KEY_PREF_API_URL:
                        // Set summary to be the selected value
                        pref.setSummary(sharedPref.getString(key, ""));
                        break;
                    case KEY_PREF_API_SRC:
                        pref.setSummary(getPrefSummary(R.array.settings_src_entryValues, R.array.settings_src_entries,
                                sharedPref.getString(KEY_PREF_API_SRC, "")));
                        break;
                    case KEY_PREF_API_VERSION:
                        pref.setSummary(getPrefSummary(R.array.settings_api_version_entryValues, R.array.settings_api_version_entries,
                                sharedPref.getString(KEY_PREF_API_VERSION, "")));
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

    }
}
