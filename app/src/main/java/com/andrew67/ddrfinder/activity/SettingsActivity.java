/*
 * Copyright (c) 2013 Luis Torres
 * Web: https://github.com/ltorres8890/Clima
 *
 * Copyright (c) 2015-2020 Andrés Cordero
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
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;

import com.andrew67.ddrfinder.BuildConfig;
import com.andrew67.ddrfinder.R;
import com.andrew67.ddrfinder.util.Analytics;
import com.andrew67.ddrfinder.util.CustomTabsUtil;
import com.andrew67.ddrfinder.util.ThemeUtil;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Arrays;

import okhttp3.HttpUrl;

// Based on https://developer.android.com/guide/topics/ui/settings.html#Fragment
public class SettingsActivity extends AppCompatActivity {

    public static final String KEY_PREF_FILTER_DDR_ONLY = "filter_ddr_only";
    public static final String KEY_PREF_API_SRC = "api_src";
    public static final String API_SRC_CUSTOM = "custom";

    public static final String KEY_PREF_THEME = "theme";
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
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static String getAboutUrl() {
        return HttpUrl.get(BuildConfig.ABOUT_BASE_URL).newBuilder()
                .addQueryParameter("c", String.valueOf(BuildConfig.VERSION_CODE))
                .addQueryParameter("n", BuildConfig.VERSION_NAME)
                .build()
                .toString();
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

            // Hide the Theme preference on API < 24 until this bug is fixed:
            // https://issuetracker.google.com/issues/131851825
            if (Build.VERSION.SDK_INT < 24) {
                findPreference(KEY_PREF_THEME).setVisible(false);
            }

            // Set preference summaries to current values
            final SharedPreferences sharedPref = getPreferenceScreen().getSharedPreferences();
            findPreference(KEY_PREF_API_SRC).setSummary(
                    getPrefSummary(R.array.settings_src_entryValues, R.array.settings_src_entries,
                            sharedPref.getString(KEY_PREF_API_SRC, "")));
            findPreference(KEY_PREF_THEME).setSummary(
                    getPrefSummary(R.array.settings_theme_entryValues, R.array.settings_theme_entries,
                            sharedPref.getString(KEY_PREF_THEME, "")));

            // Set "About" and "Privacy Policy" listeners
            findPreference("action_about").setOnPreferenceClickListener(preference -> {
                CustomTabsUtil.launchUrl(requireActivity(), getAboutUrl(),
                        sharedPref.getBoolean(KEY_PREF_CUSTOMTABS, true));
                return true;
            });
            findPreference("action_privacy_policy").setOnPreferenceClickListener(preference -> {
                CustomTabsUtil.launchUrl(requireActivity(), BuildConfig.PRIVACY_POLICY_URL,
                        sharedPref.getBoolean(KEY_PREF_CUSTOMTABS, true));
                return true;
            });
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
                    case KEY_PREF_FILTER_DDR_ONLY:
                        boolean filterDDROnly = sharedPref.getBoolean(key, false);
                        trackPreferenceChanged("filter", filterDDROnly ? "ddr" : "none");
                        break;
                    case KEY_PREF_API_SRC:
                        String newSrc = sharedPref.getString(key, "");
                        pref.setSummary(getPrefSummary(R.array.settings_src_entryValues, R.array.settings_src_entries,
                                newSrc));
                        trackPreferenceChanged(key, newSrc);
                        break;
                    case KEY_PREF_THEME:
                        String newTheme = sharedPref.getString(key, "");
                        pref.setSummary(getPrefSummary(R.array.settings_theme_entryValues, R.array.settings_theme_entries,
                                newTheme));
                        trackPreferenceChanged(key, newTheme);
                        firebaseAnalytics.setUserProperty(Analytics.UserProperty.THEME, newTheme);
                        AppCompatDelegate.setDefaultNightMode(ThemeUtil.getAppCompatDelegateMode(newTheme));
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
        private void trackPreferenceChanged(@NonNull String key, @Nullable String value) {
            Bundle params = new Bundle();
            params.putString(Analytics.Param.PREFERENCE_KEY, key);
            params.putString(Analytics.Param.PREFERENCE_VALUE, value);
            firebaseAnalytics.logEvent(Analytics.Event.SET_PREFERENCE, params);
        }
    }
}
