/*
 * Copyright (c) 2013 Luis Torres
 * Web: https://github.com/ltorres8890/Clima
 *
 * Copyright (c) 2015-2023 Andrés Cordero
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
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.appcompat.widget.Toolbar;

import android.view.MenuItem;

import com.andrew67.ddrfinder.BuildConfig;
import com.andrew67.ddrfinder.R;
import com.andrew67.ddrfinder.util.CustomTabsUtil;
import com.andrew67.ddrfinder.util.LocaleUtil;
import com.andrew67.ddrfinder.util.ThemeUtil;

import okhttp3.HttpUrl;

// Based on https://developer.android.com/guide/topics/ui/settings.html#Fragment
public class SettingsActivity extends AppCompatActivity {

    public static final String KEY_PREF_FILTER_DDR_ONLY = "filter_ddr_only";
    public static final String KEY_PREF_API_SRC = "api_src";
    public static final String API_SRC_CUSTOM = "custom";

    public static final String KEY_PREF_THEME = "theme";
    public static final String KEY_PREF_LOCALE = "locale";
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

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Load the preferences from an XML resource
            setPreferencesFromResource(R.xml.preferences, rootKey);

            // Set preference summaries to current values
            final Preference srcPref = findPreference(KEY_PREF_API_SRC);
            if (srcPref != null) {
                srcPref.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
            }

            // Set Theme preference into OS when set
            final Preference themePref = findPreference(KEY_PREF_THEME);
            if (themePref != null) {
                themePref.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
                themePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    AppCompatDelegate.setDefaultNightMode(
                            ThemeUtil.getAppCompatDelegateMode(newValue.toString()));
                    return true;
                });
            }

            // Set/get Locale preference into OS when set
            final ListPreference localePref = findPreference(KEY_PREF_LOCALE);
            if (localePref != null) {
                // Hide the in-app language selector before Android 7.0, as language resolution
                // fails for some languages (e.g. zh-Hans and zh-Hant) before then.
                // See https://developer.android.com/guide/topics/resources/multilingual-support
                // System-level language will still work since we defined configs using country code
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    localePref.setVisible(false);
                }

                String[] supportedLocales = getResources()
                        .getStringArray(R.array.settings_locale_entryValues);

                // Check for app-specific locale(s)
                LocaleListCompat appLocales = AppCompatDelegate.getApplicationLocales();
                if (appLocales.size() > 0) {
                    String appLocale = LocaleUtil.getLanguagePref(appLocales, supportedLocales);
                    if (appLocale != null) {
                        localePref.setValue(appLocale);
                    }
                }

                localePref.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
                localePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    // If the new value is "auto", pass an empty list to revert to system-level locale
                    if ("auto".equals(newValue)) {
                        AppCompatDelegate
                                .setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
                    } else {
                        LocaleListCompat appLocale = LocaleListCompat
                                .forLanguageTags(newValue.toString());
                        AppCompatDelegate.setApplicationLocales(appLocale);
                    }

                    return true;
                });
            }

            // Set "About" and "Privacy Policy" listeners
            final SharedPreferences sharedPref = getPreferenceScreen().getSharedPreferences();

            final Preference aboutAction = findPreference("action_about");
            if (aboutAction != null && sharedPref != null) {
                aboutAction.setOnPreferenceClickListener(preference -> {
                    CustomTabsUtil.launchUrl(requireActivity(), getAboutUrl(),
                            sharedPref.getBoolean(KEY_PREF_CUSTOMTABS, true), null);
                    return true;
                });
            }

            final Preference privacyPolicyAction = findPreference("action_privacy_policy");
            if (privacyPolicyAction != null && sharedPref != null) {
                privacyPolicyAction.setOnPreferenceClickListener(preference -> {
                    CustomTabsUtil.launchUrl(requireActivity(), BuildConfig.PRIVACY_POLICY_URL,
                            sharedPref.getBoolean(KEY_PREF_CUSTOMTABS, true), null);
                    return true;
                });
            }
        }
    }
}
