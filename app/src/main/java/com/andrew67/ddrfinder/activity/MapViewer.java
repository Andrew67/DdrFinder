/*
 * Copyright (c) 2013 Luis Torres
 * Web: https://github.com/ltorres8890/Clima
 *
 * Copyright (c) 2013-2018 Andrés Cordero
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
import com.andrew67.ddrfinder.arcades.ui.LocationActionsDialog;
import com.andrew67.ddrfinder.arcades.vm.ArcadesModel;
import com.andrew67.ddrfinder.arcades.ui.LocationClusterRenderer;
import com.andrew67.ddrfinder.arcades.vm.SelectedLocationModel;
import com.andrew67.ddrfinder.mylocation.MyLocationModel;
import com.andrew67.ddrfinder.arcades.model.ArcadeLocation;
import com.andrew67.ddrfinder.placesearch.PlaceAutocompleteModel;
import com.andrew67.ddrfinder.util.Analytics;
import com.andrew67.ddrfinder.util.AppLink;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
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
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.maps.android.clustering.ClusterManager;

import android.app.Dialog;
import android.arch.lifecycle.SnackbarMessage;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
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

    // Map
    private GoogleMap mMap;
    private ClusterManager<ArcadeLocation> mClusterManager;

    // UI
    private MenuItem reloadButton;
    private ProgressBar progressBar;
    private TextView attributionText;
    private LocationActionsDialog locationActionsDialog;

    // Helpers
    private FirebaseAnalytics firebaseAnalytics;
    private SharedPreferences sharedPref;
    private SharedPreferences state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        state = getSharedPreferences("state", MODE_PRIVATE);

        setContentView(R.layout.map_viewer);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        attributionText = findViewById(R.id.attribution);
        progressBar = findViewById(R.id.progressBar);
        onCreateSavedInstanceState = savedInstanceState;

        // Parse out location/zoom from a passed in app link.
        appLink = AppLink.parse(getIntent().getData());
        // Initialize a mutable AppLink builder based on the initial AppLink, for sharing.
        currentAppLink = appLink.buildUpon();

        // Set up arcades model and hook up attribution text, progress bar and errors to it
        arcadesModel = ViewModelProviders.of(this).get(ArcadesModel.class);

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

        myLocationModel = ViewModelProviders.of(this).get(MyLocationModel.class);
        placeAutocompleteModel = ViewModelProviders.of(this).get(PlaceAutocompleteModel.class);
        selectedLocationModel = ViewModelProviders.of(this).get(SelectedLocationModel.class);

        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                .getMapAsync(this);

        // For clients upgrading from <= 3.0.5/17 that had the now-removed "Custom" option selected, move to default.
        if (SettingsActivity.API_SRC_CUSTOM.equals(sharedPref.getString(SettingsActivity.KEY_PREF_API_SRC, ""))) {
            sharedPref.edit().putString(SettingsActivity.KEY_PREF_API_SRC,
                    getResources().getString(R.string.settings_src_default)).apply();
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
    }

    /**
     * Finalize initialization steps that depend on GoogleMap (previously in onCreate).
     */
    private Bundle onCreateSavedInstanceState = null;
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setIndoorEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        // Load custom "Aubergine" style
        try {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_aubergine));
        } catch (Resources.NotFoundException e) {
            // Do nothing; default map style is used.
        }

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
                firebaseAnalytics.logEvent(Analytics.Event.LOCATION_FOUND, null);
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
                firebaseAnalytics.logEvent(Analytics.Event.LOCATION_PERMISSION_DENIED, null);
                showMessage(R.string.error_perm_loc);
            }
        }));

        // If returning from a Places API Autocomplete selection, move the map to that location.
        placeAutocompleteModel.getAutocompleteResponse().observe(this, response -> {
            if (response == null) return;

            moveMapToAutocompletedPlace(response.place);
            firebaseAnalytics.logEvent(Analytics.Event.PLACES_SEARCH_COMPLETE, null);
        });
        placeAutocompleteModel.getAutocompleteError().observe(this, error -> {
            if (error == null) return;

            if (error.resultCode == RESULT_CANCELED) {
                firebaseAnalytics.logEvent(Analytics.Event.PLACES_SEARCH_CANCELED, null);
            } else if (error.resultCode == PlaceAutocomplete.RESULT_ERROR) {
                final Bundle params = new Bundle();
                params.putString(Analytics.Param.EXCEPTION_MESSAGE, error.errorMessage);
                firebaseAnalytics.logEvent(Analytics.Event.PLACES_SEARCH_ERROR, params);
            }
        });

        // Register map-filling code for when arcade model loads current arcades
        arcadesModel.getArcadeLocations().observe(this, this::fillMap);

        mClusterManager.setOnClusterItemClickListener(onClusterItemClickListener);

        mMap.setOnCameraIdleListener(cameraIdleListener);
        mMap.setOnMarkerClickListener(mClusterManager);
        mMap.setOnInfoWindowClickListener(mClusterManager);
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
     * Pops open the share chooser populated with an AppLink that represents the current state.
     */
    private void shareCurrentAppLink() {
        // Log share event for analytics.
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "map");
        params.putString(FirebaseAnalytics.Param.ITEM_ID, "redacted");
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, params);

        // Prepare and start share intent.
        Intent shareIntent = new Intent();
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
        Long rawLatitude = state.getLong(KEY_LATITUDE, Long.MIN_VALUE);
        Long rawLongitude = state.getLong(KEY_LONGITUDE, Long.MIN_VALUE);
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
        final String datasrc = sharedPref.getString(SettingsActivity.KEY_PREF_API_SRC, "");
        arcadesModel.requestLocations(box, datasrc, force);

        // Track forced refreshes by data source.
        if (force) firebaseAnalytics.logEvent(Analytics.Event.MAP_ACTION_RELOAD, null);
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
                CameraUpdateFactory.newLatLngZoom(
                        new LatLng(latLng.latitude,
                                latLng.longitude),
                        Math.max(currentZoom, BASE_ZOOM)));
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_search:
            firebaseAnalytics.logEvent(Analytics.Event.PLACES_SEARCH_START, null);
            placeAutocompleteModel.startPlaceAutocomplete(this);
            return true;
        case R.id.action_my_location:
            firebaseAnalytics.logEvent(Analytics.Event.LOCATION_REQUESTED, null);
            myLocationModel.requestMyLocation(this);
            return true;
        case R.id.action_share:
            shareCurrentAppLink();
            return true;
        case R.id.action_reload:
            if (mMap != null) updateMap(true);
            return true;
        case R.id.action_about:
            startActivity(new Intent(this, About.class));
            return true;
        case R.id.action_settings:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
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

        // Set user property for "Active Datasource" for user segmentation
        firebaseAnalytics.setUserProperty(Analytics.UserProperty.ACTIVE_DATASRC,
                sharedPref.getString(SettingsActivity.KEY_PREF_API_SRC, ""));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
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
        placeAutocompleteModel.onActivityResult(this, requestCode, resultCode, data);
    }

    /**
     * Move the map to the user-specified autocomplete location
     */
    private void moveMapToAutocompletedPlace(Place autocompletedPlace) {
        final LatLngBounds viewport = autocompletedPlace.getViewport();
        final CameraUpdate viewportCameraUpdate = viewport == null ?
                null : CameraUpdateFactory.newLatLngBounds(viewport, 0);
        final LatLng latLng = autocompletedPlace.getLatLng();
        final CameraUpdate latLngCameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, BASE_ZOOM);

        boolean success = false;

        // If LatLngBounds are available, move to those; otherwise use LatLng as center with base zoom.
        if (viewportCameraUpdate != null) {
            try {
                Log.d("MapViewer", "Places API using viewport: " + viewport.toString());
                mMap.moveCamera(viewportCameraUpdate);
                success = true;
            } catch (IllegalStateException e) {
                // This exception is thrown when map layout has not yet occurred.
                // See: https://developers.google.com/android/reference/com/google/android/gms/maps/CameraUpdateFactory.html
                Log.e("MapViewer", "Places API: map layout has not occurred; falling back to latLng");
            }
        }
        if (!success) {
            Log.d("MapViewer", "Places API using latLng: " + latLng.toString());
            mMap.moveCamera(latLngCameraUpdate);
        }
    }

    /**
     * Listener class that activates the bottom action sheet on marker click.
     */
    private final ClusterManager.OnClusterItemClickListener<ArcadeLocation>
            onClusterItemClickListener = arcadeLocation -> {
        firebaseAnalytics.logEvent(Analytics.Event.MAP_MARKER_SELECTED, null);

        selectedLocationModel.setSelectedLocation(arcadeLocation,
                arcadesModel.getSource(arcadeLocation));

        // animateCamera is too jittery to look good here
        mMap.moveCamera(CameraUpdateFactory.newLatLng(arcadeLocation.getPosition()));

        if (locationActionsDialog == null) locationActionsDialog = new LocationActionsDialog();
        locationActionsDialog.show(getSupportFragmentManager(),
                LocationActionsDialog.class.getSimpleName());

        return true; // cancel the default behavior
    };
}
