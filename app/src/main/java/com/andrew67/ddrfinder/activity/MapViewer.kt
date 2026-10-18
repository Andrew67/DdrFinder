/*
 * Copyright (c) 2013 Luis Torres
 * Web: https://github.com/ltorres8890/Clima
 *
 * Copyright (c) 2013-2026 Andrés Cordero
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

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources.NotFoundException
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.SnackbarMessage.SnackbarObserver
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.andrew67.ddrfinder.R
import com.andrew67.ddrfinder.arcades.model.ArcadeLocation
import com.andrew67.ddrfinder.arcades.model.DataSource
import com.andrew67.ddrfinder.arcades.ui.AppDiscontinuedDialogFragment
import com.andrew67.ddrfinder.arcades.ui.LocationClusterRenderer
import com.andrew67.ddrfinder.arcades.vm.ArcadesModel
import com.andrew67.ddrfinder.arcades.vm.SelectedLocationModel
import com.andrew67.ddrfinder.arcades.vm.SelectedLocationModel.CompositeArcade
import com.andrew67.ddrfinder.mylocation.MyLocationModel
import com.andrew67.ddrfinder.placesearch.PlaceAutocompleteModel
import com.andrew67.ddrfinder.util.AppLink
import com.andrew67.ddrfinder.util.CustomTabsUtil
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.security.ProviderInstaller
import com.google.android.libraries.places.api.model.Place
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.ClusterManager.OnClusterItemClickListener
import kotlin.math.abs
import kotlin.math.max
import kotlin.time.Duration.Companion.hours

class MapViewer : AppCompatActivity(), OnMapReadyCallback {
    // State
    /** Contains arcades, sources, etc. to be shown on current map  */
    private lateinit var arcadesModel: ArcadesModel

    /** Contains requested initial map state, if opened by app link.  */
    private lateinit var appLink: AppLink

    /** Contains mutable map state that can be built into a shareable AppLink.  */
    private lateinit var currentAppLink: AppLink.Builder

    /** ViewModel that assists with user location requests.  */
    private lateinit var myLocationModel: MyLocationModel

    /** ViewModel that assists with places autocomplete  */
    private lateinit var placeAutocompleteModel: PlaceAutocompleteModel

    /** ViewModel that shares which is the currently selected arcade location  */
    private lateinit var selectedLocationModel: SelectedLocationModel

    // Map
    private var mMap: GoogleMap? = null
    private var mClusterManager: ClusterManager<ArcadeLocation>? = null
    private lateinit var mapView: View

    // UI
    private lateinit var reloadButton: MenuItem
    private lateinit var progressBar: ProgressBar
    private lateinit var attributionText: TextView
    private lateinit var locationActionsBehavior: BottomSheetBehavior<View>
    private lateinit var deprecationWarningBanner: TextView
    private var appDiscontinuedDialogFragment: AppDiscontinuedDialogFragment? = null

    // Helpers
    private lateinit var sharedPref: SharedPreferences
    private lateinit var state: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, true)
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        state = getSharedPreferences("state", MODE_PRIVATE)

        setContentView(R.layout.map_viewer)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        attributionText = findViewById(R.id.attribution)
        progressBar = findViewById(R.id.progressBar)
        locationActionsBehavior =
            BottomSheetBehavior.from(findViewById(R.id.locationActions)).apply {
                setState(BottomSheetBehavior.STATE_HIDDEN)
            }

        onCreateSavedInstanceState = savedInstanceState

        // Parse out location/zoom from a passed-in app link.
        appLink = AppLink.parse(intent.data)
        // Initialize a mutable AppLink builder based on the initial AppLink, for sharing.
        currentAppLink = appLink.buildUpon()

        // Set up arcades model and hook up attribution text, progress bar and errors to it
        val viewModelProvider = ViewModelProvider(this)
        arcadesModel = viewModelProvider[ArcadesModel::class.java].also {
            it.attribution.observe(this) { s -> attributionText.text = s }
            it.progress.observe(this) { aBoolean ->
                if (aBoolean) showProgressBar()
                else hideProgressBar()
            }
            it.errorMessage.observe(
                this,
                SnackbarObserver { resourceId -> this.showMessage(resourceId) })
        }

        myLocationModel = viewModelProvider[MyLocationModel::class.java]
        placeAutocompleteModel = viewModelProvider[PlaceAutocompleteModel::class.java]
        selectedLocationModel = viewModelProvider[SelectedLocationModel::class.java]

        // Callback for clearing the location to dismiss the location info when pressing "Back"
        // See Predictive Back API: https://developer.android.com/guide/navigation/predictive-back-gesture
        // Enable when showing location actions, disable when app can be exited
        val locationActionsDismissCallback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    selectedLocationModel.clearSelectedLocation()
                }
            }
        locationActionsDismissCallback
            .isEnabled = selectedLocationModel.selectedLocation.value != null
        onBackPressedDispatcher.addCallback(this, locationActionsDismissCallback)

        // Callback for the "search" floating action button
        val searchButton = findViewById<FloatingActionButton>(R.id.action_search)
        searchButton.setOnClickListener { openPlaceSearchOverlay() }

        // Callback for the "my location" floating action button
        val myLocationButton = findViewById<FloatingActionButton>(R.id.action_my_location)
        myLocationButton.setOnClickListener { myLocationModel.requestMyLocation(this) }

        // Observe the selected location state and reveal/hide the location actions, as well as move
        // the map to the location
        // For some reason, putting this code in onMapReady causes the bottom sheet to fly from
        // above, instead of the correct behavior of popping up from underneath
        selectedLocationModel.selectedLocation
            .observe(this) { selected: CompositeArcade? ->
                if (selected == null) {
                    locationActionsBehavior.isHideable = true
                    locationActionsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN)
                    locationActionsDismissCallback.isEnabled = false
                    searchButton.show()
                    myLocationButton.show()
                } else {
                    locationActionsBehavior.isHideable = false
                    locationActionsBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
                    locationActionsDismissCallback.isEnabled = true
                    searchButton.hide()
                    myLocationButton.hide()

                    if (mMap != null) {
                        val bottomPadding =
                            getResources().getDimension(R.dimen.locationActionsFullHeight)
                                .toDouble()
                        val originalLocation = selected.arcadeLocation.position
                        val adjustedLocation = LatLng(
                            originalLocation.latitude - convertPxHeightToLat(bottomPadding) / 2,
                            originalLocation.longitude
                        )
                        animateToLocation(adjustedLocation)
                    }
                }
            }

        mapView = findViewById(R.id.map)
        val supportMapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        supportMapFragment?.getMapAsync(this)

        // Restore selected arcade location across process death
        if (savedInstanceState != null) {
            val selectedLocation = savedInstanceState
                .getParcelable<ArcadeLocation?>(KEY_SELECTED_ARCADE)
            val selectedLocationSource = savedInstanceState
                .getParcelable<DataSource?>(KEY_SELECTED_ARCADE_SOURCE)
            if (selectedLocation != null && selectedLocationSource != null) {
                selectedLocationModel.setSelectedLocation(
                    selectedLocation,
                    selectedLocationSource
                )
            }
        }

        // For clients upgrading from <= 3.0.5/17 that had the now-removed "Custom" option selected, move to default.
        if (SettingsActivity.API_SRC_CUSTOM == sharedPref.getString(
                SettingsActivity.KEY_PREF_API_SRC,
                ""
            )
        ) {
            sharedPref.edit {
                putString(
                    SettingsActivity.KEY_PREF_API_SRC,
                    getResources().getString(R.string.settings_src_default)
                )
            }
        }

        // Deprecation warning banner / forced navigation to web version
        deprecationWarningBanner = findViewById(R.id.action_launch_web)
        deprecationWarningBanner.setOnClickListener {
            CustomTabsUtil.launchUrl(
                this, currentAppLink.build().toString(),
                true, null, false
            )
        }
        arcadesModel.deprecationLevel.observe(this, { deprecationLevel ->
            if (deprecationLevel!! > 0) deprecationWarningBanner.visibility = View.VISIBLE
            if (deprecationLevel > 1) {
                if (appDiscontinuedDialogFragment == null) {
                    appDiscontinuedDialogFragment = AppDiscontinuedDialogFragment().also {
                        it.setOkListener {
                            CustomTabsUtil.launchUrl(
                                this, currentAppLink.build().toString(),
                                true, null, false
                            )
                        }
                    }
                }
                if (!appDiscontinuedDialogFragment!!.isAdded) appDiscontinuedDialogFragment!!.show(
                    supportFragmentManager,
                    "dialog"
                )
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Persist currently selected arcade location across process death
        selectedLocationModel.selectedLocation.value?.let {
            outState.apply {
                putParcelable(KEY_SELECTED_ARCADE, it.arcadeLocation)
                putParcelable(KEY_SELECTED_ARCADE_SOURCE, it.dataSource)
            }
        }
    }

    /** Stores the savedInstanceState bundle received in onCreate, to be used by onMapReady  */
    private var onCreateSavedInstanceState: Bundle? = null

    /**
     * Finalize initialization steps that depend on GoogleMap (previously in onCreate).
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap.apply {
            isIndoorEnabled = false
            uiSettings.apply {
                isMyLocationButtonEnabled = false
                isMapToolbarEnabled = false
            }
        }

        // Load custom "Aubergine" style when device is in dark mode
        try {
            if ((getResources().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES
            ) {
                mMap!!.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.style_aubergine)
                )
            } else {
                // Load custom "Muted Blue" style otherwise
                mMap!!.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.style_muted_blue)
                )
            }
        } catch (_: NotFoundException) {
            // Do nothing and use the default map style
        }

        // Set up marker cluster manager
        mClusterManager = ClusterManager(this, mMap!!)
        mClusterManager!!.renderer = LocationClusterRenderer(this, mMap, mClusterManager)

        // Start the camera from the app link lat/lng/zoom (if specified and initial launch).
        // Otherwise, start the camera on the last known user-interacted view.
        if (onCreateSavedInstanceState == null && appLink.position != null && appLink.zoom != null) {
            mMap!!.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition(appLink.position!!, appLink.zoom!!, 0f, 0f)
                )
            )
        } else {
            mMap!!.moveCamera(CameraUpdateFactory.newCameraPosition(loadCameraFromState()))
        }

        // Move camera to current location under these conditions:
        // - User has previously granted the My Location permission.
        // - This onCreate is not the result of a rotation, etc.
        // - The application was not opened via app link.
        // - It has been over 4 hours since the last camera movement.
        // - The app isn't resuming from a Places API Autocomplete selection.
        if (onCreateSavedInstanceState == null && appLink.position == null && System.currentTimeMillis() - state.getLong(
                KEY_LAST_CAMERA_TIMESTAMP,
                0
            ) >
            4.hours.inWholeMilliseconds && placeAutocompleteModel.autocompleteResponse
                .getValue() == null
        ) {
            myLocationModel.requestMyLocationSilently(this)
                { latLng -> this.moveToLocation(latLng) }
        }

        // Register success and failure listeners for the My Location data and permission.
        myLocationModel.locationResponse
            .observe(this) { myLocationResponse ->
                selectedLocationModel.clearSelectedLocation()
                moveToLocation(myLocationResponse.latLng)
            }
        myLocationModel.permissionGranted
            .observe(this) { permissionGranted ->
                if (permissionGranted) {
                    // SecurityException will never happen when this value is true
                    try {
                        mMap!!.isMyLocationEnabled = true
                    } catch (_: SecurityException) {
                    }
                } else {
                    showMessage(R.string.error_perm_loc)
                }
            }

        // If returning from a Places API Autocomplete selection, move the map to that location.
        placeAutocompleteModel.autocompleteResponse
            .observe(this) { response ->
                selectedLocationModel.clearSelectedLocation()
                moveMapToAutocompletedPlace(response.place)
            }

        // Register map-filling code for when arcade model loads current arcades
        arcadesModel.arcadeLocations.observe(this)
            { newLocations -> this.fillMap(newLocations) }

        mClusterManager!!.setOnClusterItemClickListener(onClusterItemClickListener)

        mMap?.apply {
            setOnCameraMoveStartedListener(cameraMoveStartedListener)
            setOnCameraIdleListener(cameraIdleListener)
            setOnMapClickListener { selectedLocationModel.clearSelectedLocation() }
        }

        // Adjust map padding whenever location actions bottom sheet is collapsed
        // While the sheet no longer covers attributions while expanded, it still does
        // while collapsed (when width < 500dp where we don't use too much side margin)
        // This keeps the Google attribution in view
        locationActionsBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (getResources().getBoolean(R.bool.locationActionsPushAttributionUp)) {
                    val bottomPadding =
                        if (newState == BottomSheetBehavior.STATE_COLLAPSED) getResources().getDimension(
                            R.dimen.locationActionsPeekHeight
                        ).toInt() else 0
                    mMap!!.setPadding(0, 0, 0, bottomPadding)
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        })
    }

    /**
     * Listener class that stores the current map location and requests a map update.
     */
    private val cameraIdleListener = OnCameraIdleListener {
        // Store the last moved-to coordinates, to use as starting point on next app launch.
        // This is especially relevant to those who have the location permission disabled.
        saveCameraToState(mMap!!.cameraPosition)

        mClusterManager!!.onCameraIdle()
        updateMap(false)
    }

    /**
     * Listener class that collapses the location actions sheet whenever the user pans the map etc
     */
    private val cameraMoveStartedListener = OnCameraMoveStartedListener { reason ->
        if (reason == OnCameraMoveStartedListener.REASON_GESTURE &&
            locationActionsBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED
        ) {
            locationActionsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
        }
    }

    /**
     * Converts the given height in pixels (usually a bottom padding) into an amount of latitude,
     * in order to adjust the coordinates that we animate the map towards while the bottom sheet
     * is open. We don't use GoogleMap.setPadding due to unintended side-effects that are more
     * suited towards a static padding.
     * Returns 0 if the map has not completed layout (hitting occasionally when activity destroyed).
     */
    private fun convertPxHeightToLat(heightInPx: Double): Double {
        val mapContainerHeightInPx = mapView.height.toDouble()
        if (mapContainerHeightInPx == 0.0) return 0.0

        val mapContainerLatLngBounds = mMap!!.projection.visibleRegion.latLngBounds
        val mapContainerHeightInLat =
            abs(mapContainerLatLngBounds.northeast.latitude - mapContainerLatLngBounds.southwest.latitude)
        val mapContainerScaleFactor = mapContainerHeightInLat / mapContainerHeightInPx
        return mapContainerScaleFactor * heightInPx
    }

    /**
     * Pops open the share chooser populated with an AppLink that represents the current state.
     */
    private fun shareCurrentAppLink() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, currentAppLink.build().toString())
            type = "text/plain"
        }
        startActivity(
            Intent.createChooser(
                shareIntent,
                getResources().getText(R.string.action_share)
            )
        )
    }

    /**
     * Save the given camera position to the current state.
     * @param position The position to save.
     */
    private fun saveCameraToState(position: CameraPosition) {
        state.edit {
            putFloat(KEY_LATITUDE, position.target.latitude.toFloat())
            putFloat(KEY_LONGITUDE, position.target.longitude.toFloat())
            putFloat(KEY_ZOOM, position.zoom)
            putLong(KEY_LAST_CAMERA_TIMESTAMP, System.currentTimeMillis())
        }

        // Update current AppLink that would be shared from the share icon.
        currentAppLink.position(position.target).zoom(position.zoom)
    }

    /**
     * Load the camera position from the last saved state, or use the defaults.
     */
    private fun loadCameraFromState(): CameraPosition {
        // Default: Dallas, TX, US
        // Old zoomed-out default caused too much CPU stress on slower devices
        val latitude = state.getFloat(KEY_LATITUDE, 32.7157f).toDouble()
        val longitude = state.getFloat(KEY_LONGITUDE, -96.8088f).toDouble()
        val zoom = state.getFloat(KEY_ZOOM, 9f)
        return CameraPosition(LatLng(latitude, longitude), zoom, 0f, 0f)
    }

    /**
     * Load arcade locations into the map
     * @param force Whether to ignore already loaded areas and load them again
     */
    private fun updateMap(force: Boolean) {
        val box = mMap!!.projection.visibleRegion.latLngBounds

        // During app startup, an unnecessary request is being made for (0,0), (0,0)
        // Let's suppress it here (saves network request and error message)
        val zeroZero = LatLng(0.0, 0.0)
        if (box.northeast == zeroZero && box.southwest == zeroZero) return

        val hasDDROnly = sharedPref.getBoolean(SettingsActivity.KEY_PREF_FILTER_DDR_ONLY, false)
        val datasrc = sharedPref.getString(SettingsActivity.KEY_PREF_API_SRC, "ziv")!!
        arcadesModel.requestLocations(box, datasrc, hasDDROnly, force)
    }

    /**
     * Fill map with given locations
     */
    private fun fillMap(newLocations: MutableList<ArcadeLocation>) {
        // Destroy all markers first then add all
        // (saves diff calculations and removes off-screen markers)
        mClusterManager?.apply {
            clearItems()
            addItems(newLocations)
            cluster()
        }
        Log.d("LoadedLocations", newLocations.size.toString())
    }

    /**
     * Instantly moves the map to the given location, increasing zoom to base zoom if necessary
     */
    private fun moveToLocation(latLng: LatLng) {
        val currentZoom = mMap!!.cameraPosition.zoom
        mMap!!.moveCamera(
            CameraUpdateFactory.newLatLngZoom(latLng, max(currentZoom, BASE_ZOOM.toFloat()))
        )
    }

    /**
     * Animates the map to the given location, without changing the zoom level
     */
    private fun animateToLocation(latLng: LatLng) {
        mMap!!.animateCamera(CameraUpdateFactory.newLatLng(latLng), 300, null)
    }

    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
        reloadButton.isEnabled = false
    }

    private fun hideProgressBar() {
        reloadButton.isEnabled = true
        progressBar.visibility = View.INVISIBLE
    }

    // Keep toast references mapped by resource id, to prevent excessive repeated toasts.
    private val toasts = SparseArray<Toast>()
    private fun showMessage(@StringRes resourceId: Int) {
        val oldToast = toasts.get(resourceId)
        if (oldToast == null) {
            val newToast = Toast.makeText(this, resourceId, Toast.LENGTH_SHORT)
            toasts.put(resourceId, newToast)
            newToast.show()
        } else oldToast.apply {
            setText(resourceId)
            show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        reloadButton = menu.findItem(R.id.action_reload)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Opens the place search (autocomplete) overlay activity.
     */
    private fun openPlaceSearchOverlay() {
        var currentMapView: LatLngBounds? = null
        if (mMap != null) currentMapView = mMap!!.projection.visibleRegion.latLngBounds
        placeAutocompleteModel.startPlaceAutocomplete(this, currentMapView)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_ddr_calc -> {
                startActivity(Intent(this, DdrCalcLauncher::class.java))
                return true
            }
            R.id.action_share -> {
                shareCurrentAppLink()
                return true
            }
            R.id.action_reload -> {
                if (mMap != null) updateMap(true)
                return true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            openPlaceSearchOverlay()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        myLocationModel.onResume(this)

        // Update the security provider (required for older Android platform versions to receive
        // security fixes including dropping SSLv3 as a fallback which CloudFlare implemented)
        // See https://developer.android.com/training/articles/security-gms-provider.html
        try {
            ProviderInstaller.installIfNeeded(this)
        } catch (e: GooglePlayServicesRepairableException) {
            // This exception is actionable; display Play Services update dialog to user
            val errorDialog = GoogleApiAvailability.getInstance()
                .getErrorDialog(this, e.connectionStatusCode, 0)
            errorDialog?.show()
        } catch (e: GooglePlayServicesNotAvailableException) {
            // Non-actionable exception. If user is on API <= 21 this will definitely
            // be followed by seeing "Unexpected connection error" on map API requests
            e.printStackTrace()
        }

        // Re-check in case dataSrc was changed
        if (mMap != null) updateMap(false)

        // Update app link with dataSrc and gameFilter
        val hasDDROnly = sharedPref.getBoolean(SettingsActivity.KEY_PREF_FILTER_DDR_ONLY, false)
        val datasrc: String = sharedPref.getString(SettingsActivity.KEY_PREF_API_SRC, null)!!
        currentAppLink.apply {
            gameFilter(if (hasDDROnly) "ddr" else null)
            sourceShortName(datasrc)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        myLocationModel.onRequestPermissionsResult(
            this,
            requestCode, permissions, grantResults
        )
    }

    /**
     * Handle results from startActivityForResult.
     * Currently used to handle result from Places Autocomplete widget.
     * Note: this function gets called before onResume.
     * In cases with low memory, this means the order is onCreate, onActivityResult, onResume (with onMapReady undetermined).
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!placeAutocompleteModel.onActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    /**
     * Move the map to the user-specified autocomplete location
     */
    private fun moveMapToAutocompletedPlace(autocompletedPlace: Place) {
        val viewport = autocompletedPlace.viewport
        val latLng = autocompletedPlace.location

        // If LatLngBounds are available, move to those; otherwise use LatLng as center with base zoom.
        if (viewport != null) {
            try {
                Log.d("MapViewer", "Places API using viewport: $viewport")
                mMap!!.moveCamera(CameraUpdateFactory.newLatLngBounds(viewport, 0))
                return
            } catch (_: IllegalStateException) {
                // This exception is thrown when map layout has not yet occurred.
                // See: https://developers.google.com/android/reference/com/google/android/gms/maps/CameraUpdateFactory.html
                Log.e(
                    "MapViewer",
                    "Places API: map layout has not occurred; falling back to latLng"
                )
            }
        }

        Log.d("MapViewer", "Places API using latLng: $latLng")
        if (latLng != null) {
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(
                latLng, BASE_ZOOM.toFloat()
            ))
        }
    }

    /**
     * Listener class that activates the bottom action sheet on marker click.
     */
    private val onClusterItemClickListener: OnClusterItemClickListener<ArcadeLocation> =
        { arcadeLocation ->
            selectedLocationModel.setSelectedLocation(
                arcadeLocation,
                arcadesModel.getSource(arcadeLocation)
            )
            true
        }

    companion object {
        /** savedInstanceState key for the selected arcade location  */
        private const val KEY_SELECTED_ARCADE = "selectedArcade"

        /** savedInstanceState key for the selected arcade location's data source  */
        private const val KEY_SELECTED_ARCADE_SOURCE = "selectedArcadeSource"

        private const val BASE_ZOOM = 12

        // Preference names for storing last known coordinates.
        private const val KEY_LATITUDE = "latitudeV2"
        private const val KEY_LONGITUDE = "longitudeV2"
        private const val KEY_ZOOM = "zoomV2"
        private const val KEY_LAST_CAMERA_TIMESTAMP = "lastCameraTimestamp"
    }
}
