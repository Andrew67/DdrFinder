/*
 * Copyright (c) 2013 Luis Torres
 * Web: https://github.com/ltorres8890/Clima
 *
 * Copyright (c) 2013-2023 Andrés Cordero
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

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.andrew67.ddrfinder.R;
import com.andrew67.ddrfinder.arcades.model.DataSource;
import com.andrew67.ddrfinder.arcades.vm.ArcadesModel;
import com.andrew67.ddrfinder.arcades.ui.LocationClusterRenderer;
import com.andrew67.ddrfinder.arcades.vm.SelectedLocationModel;
import com.andrew67.ddrfinder.mylocation.MyLocationModel;
import com.andrew67.ddrfinder.arcades.model.ArcadeLocation;
import com.andrew67.ddrfinder.placesearch.PlaceAutocompleteModel;
import com.andrew67.ddrfinder.util.AppLink;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.security.ProviderInstaller;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.android.clustering.ClusterManager;

import android.app.Dialog;

import androidx.activity.OnBackPressedCallback;
import androidx.core.splashscreen.SplashScreen;
import androidx.lifecycle.SnackbarMessage;
import androidx.lifecycle.ViewModelProvider;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MapViewer extends AppCompatActivity implements OnMapReadyCallback {

    private static final int BASE_ZOOM = 12;

    // State
    /** Contains arcades, sources, etc. to be shown on current map */
    private ArcadesModel arcadesModel;
    /** Contains requested initial map state, if opened by app link. */
    private AppLink appLink;
    /** Contains mutable map state that can be built into a shareable AppLink. */
    private AppLink.Builder currentAppLink;
    /** ViewModel that assists with user location requests. */
    private MyLocationModel myLocationModel;
    /** ViewModel that assists with places autocomplete */
    private PlaceAutocompleteModel placeAutocompleteModel;
    /** ViewModel that shares which is the currently selected arcade location */
    private SelectedLocationModel selectedLocationModel;
    /** savedInstanceState key for the selected arcade location */
    private final String KEY_SELECTED_ARCADE = "selectedArcade";
    /** savedInstanceState key for the selected arcade location's data source */
    private final String KEY_SELECTED_ARCADE_SOURCE = "selectedArcadeSource";

    // Map
    private GoogleMap mMap;
    private ClusterManager<ArcadeLocation> mClusterManager;
    private View mapView;

    // UI
    private MenuItem reloadButton;
    private ProgressBar progressBar;
    private TextView attributionText;
    private BottomSheetBehavior<View> locationActionsBehavior;

    // Helpers
    private SharedPreferences sharedPref;
    private SharedPreferences state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        state = getSharedPreferences("state", MODE_PRIVATE);

        setContentView(R.layout.map_viewer);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        attributionText = findViewById(R.id.attribution);
        progressBar = findViewById(R.id.progressBar);
        locationActionsBehavior = BottomSheetBehavior.from(findViewById(R.id.locationActions));
        locationActionsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        onCreateSavedInstanceState = savedInstanceState;

        // Parse out location/zoom from a passed in app link.
        appLink = AppLink.parse(getIntent().getData());
        // Initialize a mutable AppLink builder based on the initial AppLink, for sharing.
        currentAppLink = appLink.buildUpon();

        // Set up arcades model and hook up attribution text, progress bar and errors to it
        final ViewModelProvider viewModelProvider = new ViewModelProvider(this);
        arcadesModel = viewModelProvider.get(ArcadesModel.class);

        arcadesModel.getAttribution().observe(this, s -> {
            if (s != null) attributionText.setText(s);
        });

        arcadesModel.getProgress().observe(this, aBoolean -> {
            if (aBoolean == null) return;
            if (aBoolean) showProgressBar();
            else hideProgressBar();
        });

        arcadesModel.getErrorMessage().observe(this,
                (SnackbarMessage.SnackbarObserver) this::showMessage);

        myLocationModel = viewModelProvider.get(MyLocationModel.class);
        placeAutocompleteModel = viewModelProvider.get(PlaceAutocompleteModel.class);
        selectedLocationModel = viewModelProvider.get(SelectedLocationModel.class);

        // Callback for clearing the location to dismiss the location info when pressing "Back"
        // See Predictive Back API: https://developer.android.com/guide/navigation/predictive-back-gesture
        // Enable when showing location actions, disable when app can be exited
        OnBackPressedCallback locationActionsDismissCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                selectedLocationModel.clearSelectedLocation();
            }
        };
        locationActionsDismissCallback
                .setEnabled(selectedLocationModel.getSelectedLocation().getValue() != null);
        getOnBackPressedDispatcher().addCallback(this, locationActionsDismissCallback);

        // Callback for the "search" floating action button
        final FloatingActionButton searchButton = findViewById(R.id.action_search);
        searchButton.setOnClickListener(view -> openPlaceSearchOverlay());

        // Callback for the "my location" floating action button
        final FloatingActionButton myLocationButton = findViewById(R.id.action_my_location);
        myLocationButton.setOnClickListener(view -> myLocationModel.requestMyLocation(this));

        // Observe the selected location state and reveal/hide the location actions, as well as move
        // the map to the location
        // For some reason, putting this code in onMapReady causes the bottom sheet to fly from
        // above, instead of the correct behavior of popping up from underneath
        selectedLocationModel.getSelectedLocation().observe(this, selected -> {
            if (selected == null) {
                locationActionsBehavior.setHideable(true);
                locationActionsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                locationActionsDismissCallback.setEnabled(false);
                searchButton.show();
                myLocationButton.show();
            } else {
                locationActionsBehavior.setHideable(false);
                locationActionsBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                locationActionsDismissCallback.setEnabled(true);
                searchButton.hide();
                myLocationButton.hide();

                if (mMap != null) {
                    final double bottomPadding = getResources().getDimension(R.dimen.locationActionsFullHeight);
                    final LatLng originalLocation = selected.arcadeLocation.getPosition();
                    final LatLng adjustedLocation = new LatLng(
                            originalLocation.latitude - convertPxHeightToLat(bottomPadding) / 2,
                            originalLocation.longitude);
                    animateToLocation(adjustedLocation);
                }
            }
        });

        mapView = findViewById(R.id.map);
        final SupportMapFragment supportMapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (supportMapFragment != null) supportMapFragment.getMapAsync(this);

        // Restore selected arcade location across process death
        if (savedInstanceState != null) {
            final ArcadeLocation selectedLocation = savedInstanceState
                    .getParcelable(KEY_SELECTED_ARCADE);
            final DataSource selectedLocationSource = savedInstanceState
                    .getParcelable(KEY_SELECTED_ARCADE_SOURCE);
            if (selectedLocation != null && selectedLocationSource != null) {
                selectedLocationModel.setSelectedLocation(selectedLocation, selectedLocationSource);
            }
        }

        // For clients upgrading from <= 3.0.5/17 that had the now-removed "Custom" option selected, move to default.
        if (SettingsActivity.API_SRC_CUSTOM.equals(sharedPref.getString(SettingsActivity.KEY_PREF_API_SRC, ""))) {
            sharedPref.edit().putString(SettingsActivity.KEY_PREF_API_SRC,
                    getResources().getString(R.string.settings_src_default)).apply();
        }

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // Persist currently selected arcade location across process death
        final SelectedLocationModel.CompositeArcade selectedArcade =
                selectedLocationModel.getSelectedLocation().getValue();
        if (selectedArcade != null) {
            outState.putParcelable(KEY_SELECTED_ARCADE, selectedArcade.arcadeLocation);
            outState.putParcelable(KEY_SELECTED_ARCADE_SOURCE, selectedArcade.dataSource);
        }
    }

    /** Stores the savedInstanceState bundle received in onCreate, to be used by onMapReady */
    private Bundle onCreateSavedInstanceState = null;
    /**
     * Finalize initialization steps that depend on GoogleMap (previously in onCreate).
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setIndoorEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        // Load custom "Aubergine" style when device is in dark mode
        if ((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES) {
            try {
                mMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(this, R.raw.style_aubergine));
            } catch (Resources.NotFoundException e) {
                // Do nothing; default map style is used.
            }
        }

        // Set up marker cluster manager
        mClusterManager = new ClusterManager<>(this, mMap);
        mClusterManager.setRenderer(
                new LocationClusterRenderer(this, mMap, mClusterManager));

        // Start the camera from the app link lat/lng/zoom (if specified and initial launch).
        // Otherwise, start the camera on the last known user-interacted view.
        if (onCreateSavedInstanceState == null &&
                appLink.getPosition() != null && appLink.getZoom() != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                    new CameraPosition(appLink.getPosition(), appLink.getZoom(), 0, 0)
            ));
        } else {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(loadCameraFromState()));
        }

        // Move camera to current location under these conditions:
        // - User has previously granted the My Location permission.
        // - This onCreate is not the result of a rotation, etc.
        // - The application was not opened via app link.
        // - It has been over 4 hours since the last camera movement.
        // - The app isn't resuming from a Places API Autocomplete selection.
        if (onCreateSavedInstanceState == null &&
                appLink.getPosition() == null &&
                System.currentTimeMillis() - state.getLong(KEY_LAST_CAMERA_TIMESTAMP, 0) >
                        TimeUnit.HOURS.toMillis(4) &&
                placeAutocompleteModel.getAutocompleteResponse().getValue() == null) {
            myLocationModel.requestMyLocationSilently(this, this::moveToLocation);
        }

        // Register success and failure listeners for the My Location data and permission.
        myLocationModel.getLocationResponse().observe(this, myLocationResponse -> {
            if (myLocationResponse != null) {
                selectedLocationModel.clearSelectedLocation();
                moveToLocation(myLocationResponse.latLng);
            }
        });
        myLocationModel.getPermissionGranted().observe(this, (permissionGranted -> {
            if (permissionGranted == null) return;

            if (permissionGranted) {
                // SecurityException will never happen when this value is true
                try { mMap.setMyLocationEnabled(true); } catch (SecurityException ignored) { }
            }
            else {
                showMessage(R.string.error_perm_loc);
            }
        }));

        // If returning from a Places API Autocomplete selection, move the map to that location.
        placeAutocompleteModel.getAutocompleteResponse().observe(this, response -> {
            if (response == null) return;

            selectedLocationModel.clearSelectedLocation();
            moveMapToAutocompletedPlace(response.place);
        });

        // Register map-filling code for when arcade model loads current arcades
        arcadesModel.getArcadeLocations().observe(this, this::fillMap);

        mClusterManager.setOnClusterItemClickListener(onClusterItemClickListener);

        mMap.setOnCameraMoveStartedListener(cameraMoveStartedListener);
        mMap.setOnCameraIdleListener(cameraIdleListener);
        mMap.setOnMapClickListener(onMapClickListener);

        // Adjust map padding whenever location actions bottom sheet is collapsed
        // While the sheet no longer covers attributions while expanded, it still does
        // while collapsed (when width < 500dp where we don't use too much side margin)
        // This keeps the Google attribution in view
        locationActionsBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (getResources().getBoolean(R.bool.locationActionsPushAttributionUp)) {
                    final int bottomPadding = (newState == BottomSheetBehavior.STATE_COLLAPSED) ?
                            (int) getResources().getDimension(R.dimen.locationActionsPeekHeight) : 0;
                    mMap.setPadding(0, 0, 0, bottomPadding);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
    }

    /**
     * Listener class that stores the current map location and requests a map update.
     */
    private final GoogleMap.OnCameraIdleListener cameraIdleListener = () -> {
        // Store the last moved-to coordinates, to use as starting point on next app launch.
        // This is especially relevant to those who have the location permission disabled.
        final CameraPosition currentPosition = mMap.getCameraPosition();
        saveCameraToState(currentPosition);

        mClusterManager.onCameraIdle();
        updateMap(false);
    };

    /**
     * Listener class that collapses the location actions sheet whenever the user pans the map etc
     */
    private final GoogleMap.OnCameraMoveStartedListener cameraMoveStartedListener = reason -> {
        if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE &&
                locationActionsBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            locationActionsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    };

    /**
     * Converts the given height in pixels (usually a bottom padding) into an amount of latitude,
     * in order to adjust the coordinates that we animate the map towards while the bottom sheet
     * is open. We don't use GoogleMap.setPadding due to unintended side-effects that are more
     * suited towards a static padding.
     * Returns 0 if the map has not completed layout (hitting occasionally when activity destroyed).
     */
    private double convertPxHeightToLat(double heightInPx) {
        final double mapContainerHeightInPx = mapView.getHeight();
        if (mapContainerHeightInPx == 0) return 0;

        final LatLngBounds mapContainerLatLngBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        final double mapContainerHeightInLat = Math.abs(mapContainerLatLngBounds.northeast.latitude - mapContainerLatLngBounds.southwest.latitude);
        final double mapContainerScaleFactor = mapContainerHeightInLat / mapContainerHeightInPx;
        return mapContainerScaleFactor * heightInPx;
    }

    /**
     * Pops open the share chooser populated with an AppLink that represents the current state.
     */
    private void shareCurrentAppLink() {
        final Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, currentAppLink.build().toString());
        shareIntent.setType("text/plain");
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.action_share)));
    }

    // Preference names for storing last known coordinates.
    private final String KEY_LATITUDE = "latitude";
    private final String KEY_LONGITUDE = "longitude";
    private final String KEY_ZOOM = "zoom";
    private final String KEY_LAST_CAMERA_TIMESTAMP = "lastCameraTimestamp";

    /**
     * Save the given camera position to the current state.
     * @param position The position to save.
     */
    private void saveCameraToState(CameraPosition position) {
        state.edit()
                .putLong(KEY_LATITUDE, Double.doubleToRawLongBits(position.target.latitude))
                .putLong(KEY_LONGITUDE, Double.doubleToRawLongBits(position.target.longitude))
                .putFloat(KEY_ZOOM, position.zoom)
                .putLong(KEY_LAST_CAMERA_TIMESTAMP, System.currentTimeMillis())
                .apply();

        // Update current AppLink that would be shared from the share icon.
        currentAppLink.position(position.target).zoom(position.zoom);
    }

    /**
     * Load the camera position from the last saved state, or use the defaults.
     */
    private CameraPosition loadCameraFromState() {
        // Default: Dallas, TX, US
        // Old zoomed-out default caused too much CPU stress on slower devices
        final long rawLatitude = state.getLong(KEY_LATITUDE, Long.MIN_VALUE);
        final long rawLongitude = state.getLong(KEY_LONGITUDE, Long.MIN_VALUE);
        final float zoom = state.getFloat(KEY_ZOOM, 9);

        final double latitude = (rawLatitude == Long.MIN_VALUE) ? 32.7157 :
                Double.longBitsToDouble(rawLatitude);
        final double longitude = (rawLongitude == Long.MIN_VALUE) ? -96.8088 :
                Double.longBitsToDouble(rawLongitude);

        return new CameraPosition(new LatLng(latitude, longitude), zoom, 0, 0);
    }

    /**
     * Load arcade locations into the map
     * @param force Whether to ignore already loaded areas and load them again
     */
    private void updateMap(boolean force) {
        final LatLngBounds box = mMap.getProjection().getVisibleRegion().latLngBounds;

        // During app startup, an unnecessary request is being made for (0,0), (0,0)
        // Let's suppress it here (saves network request and error message)
        final LatLng zeroZero = new LatLng(0, 0);
        if (box.northeast.equals(zeroZero) && box.southwest.equals(zeroZero)) return;

        final boolean hasDDROnly = sharedPref.getBoolean(SettingsActivity.KEY_PREF_FILTER_DDR_ONLY, false);
        final String datasrc = sharedPref.getString(SettingsActivity.KEY_PREF_API_SRC, "ziv");
        arcadesModel.requestLocations(box, datasrc, hasDDROnly, force);
    }

    /**
     * Fill map with given locations
     */
    private void fillMap(@Nullable List<ArcadeLocation> newLocations) {
        if (newLocations == null) return;

        // Destroy all markers first then add all
        // (saves diff calculations and removes off-screen markers)
        mClusterManager.clearItems();
        mClusterManager.addItems(newLocations);
        mClusterManager.cluster();
        Log.d("LoadedLocations", String.valueOf(newLocations.size()));
    }

    /**
     * Instantly moves the map to the given location, increasing zoom to base zoom if necessary
     */
    private void moveToLocation(@NonNull LatLng latLng) {
        final float currentZoom = mMap.getCameraPosition().zoom;
        mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(latLng, Math.max(currentZoom, BASE_ZOOM)));
    }

    /**
     * Animates the map to the given location, without changing the zoom level
     */
    private void animateToLocation(@NonNull LatLng latLng) {
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng), 300, null);
    }

    private void showProgressBar() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (reloadButton != null) reloadButton.setEnabled(false);
    }

    private void hideProgressBar() {
        if (reloadButton != null) reloadButton.setEnabled(true);
        if (progressBar != null) progressBar.setVisibility(View.INVISIBLE);
    }

    // Keep toast references mapped by resource id, to prevent excessive repeated toasts.
    private final SparseArray<Toast> toasts = new SparseArray<>();
    private void showMessage(@StringRes int resourceId) {
        final Toast oldToast = toasts.get(resourceId);
        if (oldToast == null) {
            final Toast newToast = Toast.makeText(this, resourceId, Toast.LENGTH_SHORT);
            toasts.put(resourceId, newToast);
            newToast.show();
        } else {
            oldToast.setText(resourceId);
            oldToast.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        reloadButton = menu.findItem(R.id.action_reload);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Opens the place search (autocomplete) overlay activity.
     */
    private void openPlaceSearchOverlay() {
        LatLngBounds currentMapView = null;
        if (mMap != null) currentMapView = mMap.getProjection().getVisibleRegion().latLngBounds;
        placeAutocompleteModel.startPlaceAutocomplete(this, currentMapView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.action_ddr_calc) {
            startActivity(new Intent(this, DdrCalcLauncher.class));
            return true;
        } else if (itemId == R.id.action_share) {
            shareCurrentAppLink();
            return true;
        } else if (itemId == R.id.action_reload) {
            if (mMap != null) updateMap(true);
            return true;
        } else if (itemId == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            openPlaceSearchOverlay();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        myLocationModel.onResume(this);

        // Update the security provider (required for older Android platform versions to receive
        // security fixes including dropping SSLv3 as a fallback which CloudFlare implemented)
        // See https://developer.android.com/training/articles/security-gms-provider.html
        try {
            ProviderInstaller.installIfNeeded(this);
        } catch (GooglePlayServicesRepairableException e) {
            // This exception is actionable; display Play Services update dialog to user
            final Dialog errorDialog = GoogleApiAvailability.getInstance()
                    .getErrorDialog(this, e.getConnectionStatusCode(), 0);
            if (errorDialog != null) errorDialog.show();
        } catch (GooglePlayServicesNotAvailableException e) {
            // Non-actionable exception. If user is on API <= 21 this will definitely
            // be followed by seeing "Unexpected connection error" on map API requests
            e.printStackTrace();
        }

        // Re-check in case dataSrc was changed
        if (mMap != null) updateMap(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        myLocationModel.onRequestPermissionsResult(this,
                requestCode, permissions, grantResults);
    }

    /**
     * Handle results from startActivityForResult.
     * Currently used to handle result from Places Autocomplete widget.
     * Note: this function gets called before onResume.
     * In cases with low memory, this means the order is onCreate, onActivityResult, onResume (with onMapReady undetermined).
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!placeAutocompleteModel.onActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Move the map to the user-specified autocomplete location
     */
    private void moveMapToAutocompletedPlace(Place autocompletedPlace) {
        final LatLngBounds viewport = autocompletedPlace.getViewport();
        final CameraUpdate viewportCameraUpdate = viewport == null ?
                null : CameraUpdateFactory.newLatLngBounds(viewport, 0);
        final LatLng latLng = autocompletedPlace.getLatLng();
        final CameraUpdate latLngCameraUpdate = latLng == null ?
                null : CameraUpdateFactory.newLatLngZoom(latLng, BASE_ZOOM);

        boolean success = false;

        // If LatLngBounds are available, move to those; otherwise use LatLng as center with base zoom.
        if (viewportCameraUpdate != null) {
            try {
                Log.d("MapViewer", "Places API using viewport: " + viewport);
                mMap.moveCamera(viewportCameraUpdate);
                success = true;
            } catch (IllegalStateException e) {
                // This exception is thrown when map layout has not yet occurred.
                // See: https://developers.google.com/android/reference/com/google/android/gms/maps/CameraUpdateFactory.html
                Log.e("MapViewer", "Places API: map layout has not occurred; falling back to latLng");
            }
        }
        if (!success) {
            Log.d("MapViewer", "Places API using latLng: " + latLng);
            if (latLngCameraUpdate != null) {
                mMap.moveCamera(latLngCameraUpdate);
            }
        }
    }

    /**
     * Listener class that activates the bottom action sheet on marker click.
     */
    private final ClusterManager.OnClusterItemClickListener<ArcadeLocation>
            onClusterItemClickListener = arcadeLocation -> {
        selectedLocationModel.setSelectedLocation(arcadeLocation,
                arcadesModel.getSource(arcadeLocation));

        // Return value false moves the map; true suppresses it
        return true;
    };

    /**
     * Listener class that clears the currently selected location on map (non-marker) click
     */
    private final GoogleMap.OnMapClickListener onMapClickListener =
            point -> selectedLocationModel.clearSelectedLocation();
}
