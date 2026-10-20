/*
 * Copyright (c) 2013 Luis Torres
 * Web: https://github.com/ltorres8890/Clima
 *
 * Copyright (c) 2015-2026 Andrés Cordero
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
package com.andrew67.ddrfinder.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import androidx.fragment.compose.AndroidFragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.andrew67.ddrfinder.BuildConfig
import com.andrew67.ddrfinder.R
import com.andrew67.ddrfinder.util.CustomTabsUtil
import com.andrew67.ddrfinder.util.LocaleUtil
import com.andrew67.ddrfinder.util.ThemeUtil
import okhttp3.HttpUrl.Companion.toHttpUrl

// Based on https://developer.android.com/guide/topics/ui/settings.html#Fragment
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SettingsScreen() }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SettingsScreen() {
        MaterialTheme (
            // TODO: App-specific color theme
            colorScheme = if (isSystemInDarkTheme())
                darkColorScheme() else lightColorScheme()
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.primary,
                            navigationIconContentColor = MaterialTheme.colorScheme.primary,
                        ),
                        title = { Text(stringResource(R.string.action_settings)) },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    painterResource(R.drawable.ic_arrow_back),
                                    contentDescription = "Back", // TODO: i18n
                                )
                            }
                        },
                    )
                },
            ) {
                padding ->
                AndroidFragment<SettingsFragment>(modifier = Modifier.padding(padding))
            }
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            // Load the preferences from an XML resource
            setPreferencesFromResource(R.xml.preferences, rootKey)

            // Set preference summaries to current values
            val srcPref = findPreference<Preference>(KEY_PREF_API_SRC)
            srcPref?.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance())

            // Set Theme preference into OS when set
            val themePref = findPreference<Preference>(KEY_PREF_THEME)
            themePref?.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance())
            themePref?.setOnPreferenceChangeListener { _, newValue: Any? ->
                AppCompatDelegate.setDefaultNightMode(
                    ThemeUtil.getAppCompatDelegateMode(newValue.toString())
                )
                true
            }

            // Set/get Locale preference into OS when set
            val localePref = findPreference<ListPreference>(KEY_PREF_LOCALE)
            if (localePref != null) {
                val supportedLocales = resources
                    .getStringArray(R.array.settings_locale_entryValues)

                // Check for app-specific locale(s)
                val appLocales = AppCompatDelegate.getApplicationLocales()
                if (!appLocales.isEmpty) {
                    val appLocale = LocaleUtil.getLanguagePref(appLocales, supportedLocales)
                    if (appLocale != null) {
                        localePref.setValue(appLocale)
                    }
                }

                localePref.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance())
                localePref.setOnPreferenceChangeListener { _, newValue: Any? ->
                    // If the new value is "auto", pass an empty list to revert to system-level locale
                    if ("auto" == newValue) {
                        AppCompatDelegate
                            .setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                    } else {
                        val appLocale = LocaleListCompat
                            .forLanguageTags(newValue.toString())
                        AppCompatDelegate.setApplicationLocales(appLocale)
                    }
                    true
                }
            }

            // Set "About" and "Privacy Policy" listeners
            val sharedPref = preferenceScreen.getSharedPreferences()

            val aboutAction = findPreference<Preference>("action_about")
            if (aboutAction != null && sharedPref != null) {
                aboutAction.setOnPreferenceClickListener {
                    CustomTabsUtil.launchUrl(
                        requireActivity(),
                        aboutUrl,
                        sharedPref.getBoolean(KEY_PREF_CUSTOMTABS, true),
                        null, false
                    )
                    true
                }
            }

            val privacyPolicyAction = findPreference<Preference>("action_privacy_policy")
            if (privacyPolicyAction != null && sharedPref != null) {
                privacyPolicyAction.onPreferenceClickListener =
                    Preference.OnPreferenceClickListener {
                        CustomTabsUtil.launchUrl(
                            requireActivity(), BuildConfig.PRIVACY_POLICY_URL,
                            sharedPref.getBoolean(KEY_PREF_CUSTOMTABS, true),
                            null, false
                        )
                        true
                    }
            }
        }
    }

    companion object {
        const val KEY_PREF_FILTER_DDR_ONLY = "filter_ddr_only"
        const val KEY_PREF_API_SRC = "api_src"
        const val API_SRC_CUSTOM = "custom"

        const val KEY_PREF_THEME = "theme"
        const val KEY_PREF_LOCALE = "locale"
        const val KEY_PREF_CUSTOMTABS = "customtabs"
        const val KEY_PREF_PARTIALHEIGHT = "customtabs_partialheight"

        private val aboutUrl
            get() = BuildConfig.ABOUT_BASE_URL.toHttpUrl().newBuilder()
                .addQueryParameter("c", BuildConfig.VERSION_CODE.toString())
                .addQueryParameter("n", BuildConfig.VERSION_NAME)
                .build()
                .toString()
    }
}
