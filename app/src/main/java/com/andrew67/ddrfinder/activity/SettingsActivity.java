/*
 * Copyright (c) 2013 Luis Torres
 * Web: https://github.com/ltorres8890/Clima
 *
 * Copyright (c) 2015-2017 Andrés Cordero
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

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;

import com.andrew67.ddrfinder.R;

import org.piwik.sdk.Piwik;
import org.piwik.sdk.PiwikApplication;
import org.piwik.sdk.TrackHelper;
import org.piwik.sdk.Tracker;

import java.util.Arrays;

// Based on https://developer.android.com/guide/topics/ui/settings.html#Fragment
public class SettingsActivity extends Activity {

    public static final String KEY_PREF_API_SRC = "api_src";
    public static final String API_SRC_CUSTOM = "custom";

    public static final String KEY_PREF_ANALYTICS = "analyticsEnabled";
    public static final String KEY_PREF_LOCATION = "location";
    public static final String KEY_PREF_CUSTOMTABS = "customtabs";

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

        Tracker tracker = ((PiwikApplication) getApplication()).getTracker();
        TrackHelper.track().screen("/settings").title("Settings").with(tracker);
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

    public static class EnableLocationDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // From https://developer.android.com/guide/topics/ui/dialogs.html#DialogFragment
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final Tracker tracker = ((PiwikApplication) getActivity().getApplication()).getTracker();

            builder.setMessage(R.string.settings_location_dialog_message)
                    .setTitle(R.string.settings_location)
                    .setPositiveButton(R.string.settings_location_dialog_positive, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Recipe from http://stackoverflow.com/a/32983128
                            Intent i = new Intent();
                            i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            i.setData(Uri.fromParts("package", getActivity().getPackageName(), null));
                            startActivity(i);
                            TrackHelper.track().event("EnableLocationDialog", "clickedSettings").with(tracker);
                        }
                    })
                    .setNegativeButton(R.string.settings_location_dialog_negative, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                            TrackHelper.track().event("EnableLocationDialog", "cancelled").with(tracker);
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    public static class SettingsFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        Tracker tracker;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            // Set preference summaries to current values
            final SharedPreferences sharedPref = getPreferenceScreen().getSharedPreferences();

            findPreference(KEY_PREF_API_SRC).setSummary(
                    getPrefSummary(R.array.settings_src_entryValues, R.array.settings_src_entries,
                        sharedPref.getString(KEY_PREF_API_SRC, "")));

            // Set analytics option to match opt-out / dry-run status
            final Piwik piwik = ((PiwikApplication) getActivity().getApplication()).getPiwik();
            tracker = ((PiwikApplication) getActivity().getApplication()).getTracker();
            final Preference analyticsPref = findPreference(KEY_PREF_ANALYTICS);
            analyticsPref.setDefaultValue(!piwik.isOptOut() && !piwik.isDryRun());

            // Disable analytics option toggle if dry-run is overriding opt-out
            analyticsPref.setEnabled(!piwik.isDryRun());

            // Set changes to analytics option to set piwik persistent opt-out flag
            analyticsPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean enableAnalytics = (boolean) newValue;
                    piwik.setOptOut(!enableAnalytics);
                    return true;
                }
            });
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);

            // Disable the "Enable Current Location" option if we already have the permission
            if (ContextCompat.checkSelfPermission(getActivity(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                findPreference(KEY_PREF_LOCATION).setEnabled(false);
            }
            else {
                // Set up dialog for "Enable Current Location" option
                findPreference(KEY_PREF_LOCATION).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        new EnableLocationDialogFragment().show(getFragmentManager(), "dialog");
                        TrackHelper.track().event("Settings", "clickedEnableLocation").with(tracker);
                        return false;
                    }
                });
            }
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
                        String newSrc = sharedPref.getString(KEY_PREF_API_SRC, "");
                        pref.setSummary(getPrefSummary(R.array.settings_src_entryValues, R.array.settings_src_entries,
                                newSrc));

                        TrackHelper.track().event("Settings", "changedDataSource")
                                .name(newSrc).with(tracker);
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
