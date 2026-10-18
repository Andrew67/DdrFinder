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
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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

    // Formatters
    private var distanceFormat: NumberFormat? = null // 2 decimal digits
    // API 30+
    private var distanceFormatter: LocalizedNumberFormatter? = null // 2 decimal digits

    private lateinit var arcadeDistance: TextView
    private lateinit var arcadeHasDDR: TextView
    private lateinit var arcadeHasDDRIconYes: ImageView
    private lateinit var arcadeHasDDRIconNo: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.location_actions, container, false)

        arcadeDistance = view.findViewById(R.id.location_distance)
        arcadeHasDDR = view.findViewById(R.id.location_has_ddr)
        arcadeHasDDRIconYes = view.findViewById(R.id.location_has_ddr_icon_yes)
        arcadeHasDDRIconNo = view.findViewById(R.id.location_has_ddr_icon_no)

        view.findViewById<View>(R.id.action_navigate).setOnClickListener { onNavigateClicked() }
        view.findViewById<View>(R.id.action_moreinfo).setOnClickListener { onMoreInfoClicked() }
        view.findViewById<View>(R.id.action_copygps).setOnClickListener { onCopyClicked() }

        return view
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
                MaterialTheme(
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
                        Box(Modifier.height(6.dp))
                        arcadeLocation?.name?.let {
                            Text(
                                text = it,
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
     * Updates the UI with name/city and requests a new location to update the distance if empty
     */
    private val onSelectedLocationUpdated = Observer { selectedLocation: CompositeArcade? ->
        if (selectedLocation == null) {
            arcadeDistance.text = ""
        } else {
            // If distance is unset, leave blank.
            // Request a silent distance update. If location is disabled, it stays blank.
            // Otherwise, causes a newer object to get emitted with the updated distance.
            if (selectedLocation.distanceKm == null) {
                arcadeDistance.text = ""
            } else {
                arcadeDistance.text = getString(
                    R.string.distance_km,
                    formatDistance(selectedLocation.distanceKm)
                )
            }
            myLocationModel.requestMyLocationSilently(
                requireActivity()
            ) { userLocation: LatLng -> selectedLocationModel.updateUserLocation(userLocation) }

            // If DDR availability is available, update with the appropriate text and icon.
            // Otherwise, set it back to blank.
            if (selectedLocation.dataSource.hasDDR()) {
                arcadeHasDDR.visibility = View.VISIBLE
                arcadeHasDDRIconYes.setVisibility(
                    if (selectedLocation.arcadeLocation.hasDDR()) View.VISIBLE else View.INVISIBLE)
                arcadeHasDDRIconNo.setVisibility(
                    if (!selectedLocation.arcadeLocation.hasDDR()) View.VISIBLE else View.INVISIBLE)
            } else {
                arcadeHasDDR.visibility = View.INVISIBLE
                arcadeHasDDRIconYes.setVisibility(View.INVISIBLE)
                arcadeHasDDRIconNo.setVisibility(View.INVISIBLE)
            }

            // Set up LocationActions object that enables the available actions
            locationActions = LocationActions(
                selectedLocation.arcadeLocation,
                selectedLocation.dataSource
            )
            customTabActivityHelper.mayLaunchUrl(
                Uri.parse(locationActions.getInfoURL()), null, null
            )
        }
    }

    private fun formatDistance(distance: Float): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (distanceFormatter == null) {
                distanceFormatter = NumberFormatter
                    .withLocale(Locale.getDefault(Locale.Category.FORMAT))
                    .precision(Precision.maxFraction(2))
            }
            return distanceFormatter!!.format(distance.toDouble()).toString()
        }

        if (distanceFormat == null) {
            distanceFormat = NumberFormat.getNumberInstance()
            distanceFormat!!.setMaximumFractionDigits(2)
        }
        return distanceFormat!!.format(distance.toDouble())
    }

    private fun onNavigateClicked() {
        locationActions.navigate(requireActivity())
    }

    private var moreInfoErrorToast: Toast? = null
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
        if (!moreInfoOpenSuccess) {
            if (moreInfoErrorToast == null) moreInfoErrorToast = Toast.makeText(
                requireActivity(),
                R.string.error_opening_browser, Toast.LENGTH_LONG
            )
            moreInfoErrorToast!!.show()
        }
    }

    private var copiedToast: Toast? = null
    private fun onCopyClicked() {
        val copySuccess = locationActions.copyGps(requireActivity())
        if (copySuccess) {
            if (copiedToast == null) copiedToast = Toast.makeText(
                requireActivity(),
                R.string.copy_complete, Toast.LENGTH_SHORT
            )
            copiedToast!!.show()
        }
    }
}
