/*
 * Copyright (c) 2018-2026 Andrés Cordero
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
package com.andrew67.ddrfinder.arcades.ui

import android.icu.number.LocalizedNumberFormatter
import android.icu.number.NumberFormatter
import android.icu.number.Precision
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.andrew67.ddrfinder.R
import com.andrew67.ddrfinder.activity.SettingsActivity
import com.andrew67.ddrfinder.arcades.util.LocationActions
import com.andrew67.ddrfinder.arcades.vm.SelectedLocationModel
import com.andrew67.ddrfinder.arcades.vm.SelectedLocationModel.CompositeArcade
import com.andrew67.ddrfinder.mylocation.MyLocationModel
import com.google.android.gms.maps.model.LatLng
import org.chromium.customtabsdemos.CustomTabActivityHelper
import java.text.NumberFormat
import java.util.Locale

class LocationActionsFragment : Fragment(), LifecycleEventObserver {
    private lateinit var selectedLocationModel: SelectedLocationModel
    private lateinit var myLocationModel: MyLocationModel

    private lateinit var locationActions: LocationActions
    private lateinit var customTabActivityHelper: CustomTabActivityHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.location_actions,
            container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModelProvider = ViewModelProvider(requireActivity())

        selectedLocationModel = viewModelProvider[SelectedLocationModel::class.java]
        selectedLocationModel.selectedLocation
            .observe(getViewLifecycleOwner(), onSelectedLocationUpdated)

        myLocationModel = viewModelProvider[MyLocationModel::class.java]

        customTabActivityHelper = CustomTabActivityHelper()
        lifecycle.addObserver(this)

        view.findViewById<ComposeView>(R.id.compose_view).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val selectedLocation = selectedLocationModel.selectedLocation.observeAsState()
                val arcadeLocation = selectedLocation.value?.arcadeLocation
                val distanceKm = selectedLocation.value?.distanceKm
                val dataSource = selectedLocation.value?.dataSource

                MaterialTheme(
                    // TODO: App-specific color theme
                    colorScheme = if (isSystemInDarkTheme())
                        darkColorScheme() else lightColorScheme()
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Box(
                            Modifier
                            .size(width = 24.dp, height = 4.dp)
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(Modifier.height(6.dp))
                        arcadeLocation?.name?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1, // TODO: Improve
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        arcadeLocation?.city?.let {
                            Text(
                                it,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1, // TODO: Improve
                                overflow = TextOverflow.StartEllipsis,
                            )
                        }
                        Button(
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                            onClick = { onNavigateClicked() }
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_navigation_black_24dp),
                                contentDescription = null,
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text(stringResource(R.string.location_actions_navigate))
                            distanceKm?.let {
                                Spacer(Modifier.width(24.dp))
                                Text(
                                    stringResource(
                                        R.string.distance_km,
                                        formatDistance(it)
                                    ),
                                )
                            }
                        }
                        FilledTonalButton(
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                            onClick = { onMoreInfoClicked() }
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_info_outline_black_24dp),
                                contentDescription = null,
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text(stringResource(R.string.location_actions_info))
                            dataSource?.hasDDR().let {
                                Spacer(Modifier.width(24.dp))
                                Text(stringResource(R.string.has_ddr))
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                if (arcadeLocation?.hasDDR() == true) {
                                    Icon(
                                        painterResource(R.drawable.ic_check_black_24dp),
                                        contentDescription =
                                            stringResource(R.string.has_ddr_yes),
                                    )
                                } else {
                                    Icon(
                                        painterResource(R.drawable.ic_close_black_24dp),
                                        contentDescription =
                                            stringResource(R.string.has_ddr_no),
                                        )
                                }
                            }
                        }
                        FilledTonalButton(
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                            onClick = { onCopyClicked() }
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_content_copy_black_24dp),
                                contentDescription = null,
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text(stringResource(R.string.location_actions_copy))
                        }
                    }
                }
            }
        }
    }

    /**
     * Observe the lifecycle in order to manage the Custom Tab Session
     */
    override fun onStateChanged(
        source: LifecycleOwner,
        event: Lifecycle.Event
    ) {
        if (event == Lifecycle.Event.ON_START) {
            customTabActivityHelper.bindCustomTabsService(requireActivity())
        }
        if (event == Lifecycle.Event.ON_STOP) {
            customTabActivityHelper.unbindCustomTabsService(requireActivity())
        }
    }

    /**
     * One-time setup tasks per location selection
     */
    private val onSelectedLocationUpdated = Observer { selectedLocation: CompositeArcade? ->
        if (selectedLocation != null) {
            // Request a silent distance update. If location is disabled, it stays blank.
            // Otherwise, causes a newer object to get emitted with the updated distance.
            myLocationModel.requestMyLocationSilently(
                requireActivity()
            ) { userLocation: LatLng -> selectedLocationModel.updateUserLocation(userLocation) }

            // Set up LocationActions object that enables the available actions
            locationActions = LocationActions(
                selectedLocation.arcadeLocation,
                selectedLocation.dataSource
            )
            customTabActivityHelper.mayLaunchUrl(
                Uri.parse(locationActions.getInfoURL()),
                null, null
            )
        }
    }

    // Formatters
    /** 2 decimal digits */
    private val distanceFormat: NumberFormat by lazy {
        NumberFormat.getNumberInstance().apply {
            setMaximumFractionDigits(2)
        }
    }
    // API 30+
    @delegate:RequiresApi(Build.VERSION_CODES.R)
    private val distanceFormatter: LocalizedNumberFormatter by lazy {
        NumberFormatter
            .withLocale(Locale.getDefault(Locale.Category.FORMAT))
            .precision(Precision.maxFraction(2))
    }
    private fun formatDistance(distance: Float): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return distanceFormatter.format(distance.toDouble()).toString()
        }
        return distanceFormat.format(distance.toDouble())
    }

    private fun onNavigateClicked() {
        locationActions.navigate(requireActivity())
    }

    private val moreInfoErrorToast: Toast by lazy {
        Toast.makeText(
            requireActivity(),
            R.string.error_opening_browser, Toast.LENGTH_LONG
        )
    }
    private fun onMoreInfoClicked() {
        val preferences = PreferenceManager
            .getDefaultSharedPreferences(requireActivity())
        val useCustomTabs = preferences
            .getBoolean(SettingsActivity.KEY_PREF_CUSTOMTABS, true)
        val usePartialHeight = preferences
            .getBoolean(SettingsActivity.KEY_PREF_PARTIALHEIGHT, true)
        val moreInfoOpenSuccess = locationActions
            .moreInfo(
                requireActivity(), useCustomTabs,
                customTabActivityHelper.getSession(), usePartialHeight
            )
        if (!moreInfoOpenSuccess) moreInfoErrorToast.show()
    }

    private val copiedToast: Toast by lazy {
        Toast.makeText(
            requireActivity(),
            R.string.copy_complete, Toast.LENGTH_SHORT
        )
    }
    private fun onCopyClicked() {
        val copySuccess = locationActions.copyGps(requireActivity())
        if (copySuccess) copiedToast.show()
    }
}
