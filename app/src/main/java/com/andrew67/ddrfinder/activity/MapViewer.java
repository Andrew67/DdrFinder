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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.andrew67.ddrfinder.R;
import com.andrew67.ddrfinder.adapters.MapLoader;
import com.andrew67.ddrfinder.adapters.MapLoaderV3;
import com.andrew67.ddrfinder.handlers.LocationClusterRenderer;
import com.andrew67.ddrfinder.handlers.LocationActions;
import com.andrew67.ddrfinder.interfaces.ArcadeLocation;
import com.andrew67.ddrfinder.interfaces.DataSource;
import com.andrew67.ddrfinder.interfaces.MessageDisplay;
import com.andrew67.ddrfinder.util.Analytics;
import com.andrew67.ddrfinder.util.AppLink;
import com.andrew67.ddrfinder.util.AttributionGenerator;
import com.andrew67.ddrfinder.util.ThemeUtil;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.maps.android.clustering.ClusterManager;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MapViewer extends Activity implements MessageDisplay, OnMapReadyCallback {

    private static final int BASE_ZOOM = 12;
    private static final int PERMISSIONS_REQUEST_LOCATION = 1;
    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 2;

    /** Contains requested initial map state, if opened by app link. */
    private AppLink appLink;
    /** Contains mutable map state that can be built into a shareable AppLink. */
    private AppLink.Builder currentAppLink;
    /** When non-null, points to a user-specified Autocomplete-provided location to move the map to. */
    private Place autocompletedPlace = null;

    private GoogleMap mMap = null;
    private ClusterManager<ArcadeLocation> mClusterManager;
    private LocationClusterRenderer mClusterRenderer;
    private MenuItem reloadButton;
    private ProgressBar progressBar;

    private final Set<Integer> loadedLocationIds = new HashSet<>();
    // Set as ArrayList instead of List due to Bundle packing
    private final ArrayList<ArcadeLocation> loadedLocations = new ArrayList<>();
    private final ArrayList<LatLngBounds> loadedAreas =	new ArrayList<>();

    /**
     * Loaded data sources, keyed by source name
     */
    private final Map<String,DataSource> loadedSources = new HashMap<>();
    private TextView attributionText;

    private FirebaseAnalytics firebaseAnalytics;
    private SharedPreferences sharedPref;
    private SharedPreferences state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        state = getSharedPreferences(PREF_STATE, MODE_PRIVATE);

        setContentView(R.layout.map_viewer);

        attributionText = findViewById(R.id.attribution);
        progressBar = findViewById(R.id.progressBar);
        onCreateSavedInstanceState = savedInstanceState;

        // Parse out location/zoom from a passed in app link.
        appLink = AppLink.parse(getIntent().getData());
        // Initialize a mutable AppLink builder based on the initial AppLink, for sharing.
        currentAppLink = appLink.buildUpon();

        ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);

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

        // Load custom "Icy Blue" style on Material Style devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_icy_blue));
            } catch (Resources.NotFoundException e) {
                // Do nothing; default map style is used.
            }
        }

        mClusterManager = new ClusterManager<>(this, mMap);
        mClusterRenderer = new LocationClusterRenderer(this, mMap, mClusterManager);
        mClusterManager.setRenderer(mClusterRenderer);

        // Restore previously loaded areas locations, and sources if available
        // (and re-create the location markers)
        if (onCreateSavedInstanceState != null) {
            final ArrayList<LatLngBounds> savedLoadedAreas =
                    onCreateSavedInstanceState.getParcelableArrayList("loadedAreas");
            final ArrayList<ArcadeLocation> savedLoadedLocations =
                    onCreateSavedInstanceState.getParcelableArrayList("loadedLocations");
            final ArrayList<DataSource> savedLoadedSources =
                    onCreateSavedInstanceState.getParcelableArrayList("loadedSources");

            if (savedLoadedAreas != null && savedLoadedLocations != null && savedLoadedSources != null) {
                fillMap(savedLoadedAreas, savedLoadedLocations, savedLoadedSources);
            }
        }

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

        // Check for location permission, and request if disabled.
        // This permission allows the user to locate themselves on the map.
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            // Move camera to current location under 3 conditions:
            // - This onCreate is not the result of a rotation, etc.
            // - The application was not opened via app link.
            // - It has been over 4 hours since the last camera movement.
            // - The app isn't resuming from a Places API Autocomplete selection.
            if (onCreateSavedInstanceState == null &&
                    appLink.getPosition() == null &&
                    System.currentTimeMillis() - state.getLong(KEY_LAST_CAMERA_TIMESTAMP, 0) > 1000 * 60 * 60 * 4 &&
                    autocompletedPlace == null) {
                zoomToCurrentLocation();
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_LOCATION);
        }

        // If returning from a Places API Autocomplete selection, move the map to that location.
        if (autocompletedPlace != null) moveMapToAutocompletedPlace();

        mClusterManager.setOnClusterItemClickListener(actionModeEnabler);
        mClusterManager.setOnClusterItemInfoWindowClickListener(moreInfoListener);

        mMap.setOnCameraIdleListener(cameraIdleListener);
        mMap.setOnMapClickListener(actionModeDisabler);
        mMap.setOnMarkerClickListener(mClusterManager);
        mMap.setOnInfoWindowClickListener(mClusterManager);
    }

    /**
     * Listener class that stores the current map location and requests a map update.
     */
    private final GoogleMap.OnCameraIdleListener cameraIdleListener = new GoogleMap.OnCameraIdleListener() {
        @Override
        public void onCameraIdle() {
            // Store the last moved-to coordinates, to use as starting point on next app launch.
            // This is especially relevant to those who have the location permission disabled.
            final CameraPosition currentPosition = mMap.getCameraPosition();
            saveCameraToState(currentPosition);

            mClusterManager.onCameraIdle();
            updateMap(false);
        }
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
    private final String PREF_STATE = "state";
    private final String KEY_LATITUDE = "latitude";
    private final String KEY_LONGITUDE = "longitude";
    private final String KEY_ZOOM = "zoom";
    private final String KEY_LAST_CAMERA_TIMESTAMP = "lastCameraTimestamp";

    // Dallas, TX, US; zoomed-out default causes too much CPU stress on slower devices
    private final double DEFAULT_LATITUDE = 32.7157;
    private final double DEFAULT_LONGITUDE = -96.8088;
    private final float DEFAULT_ZOOM = 9;

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
        Long rawLatitude = state.getLong(KEY_LATITUDE, Long.MIN_VALUE);
        Long rawLongitude = state.getLong(KEY_LONGITUDE, Long.MIN_VALUE);
        final float zoom = state.getFloat(KEY_ZOOM, DEFAULT_ZOOM);

        final double latitude = (rawLatitude == Long.MIN_VALUE) ? DEFAULT_LATITUDE :
                Double.longBitsToDouble(rawLatitude);
        final double longitude = (rawLongitude == Long.MIN_VALUE) ? DEFAULT_LONGITUDE :
                Double.longBitsToDouble(rawLongitude);

        return new CameraPosition(new LatLng(latitude, longitude), zoom, 0, 0);
    }

    /**
     * Load arcade locations into the map
     * @param force Whether to ignore already loaded areas and load them again
     */
    private void updateMap(boolean force) {
        LatLngBounds box = mMap.getProjection().getVisibleRegion().latLngBounds;
        if (force || !alreadyLoaded(box)) {
            // Preload a slightly larger box area when zoomed in, for a smoother pan/zoom experience
            // as network requests are reduced.
            if (Math.abs(box.northeast.latitude - box.southwest.latitude) < 0.5
                    && Math.abs(box.northeast.longitude - box.southwest.longitude) < 0.5) {
                box = LatLngBounds.builder()
                        .include(new LatLng(box.northeast.latitude + 0.125, box.northeast.longitude + 0.125))
                        .include(new LatLng(box.southwest.latitude - 0.125, box.southwest.longitude - 0.125))
                        .build();
            }

            final String datasrc = sharedPref.getString(SettingsActivity.KEY_PREF_API_SRC, "");

            new MapLoaderV3(datasrc, mapLoaderCallback).execute(box);

            // Track forced refreshes by data source.
            if (force) trackMapAction("forced_refresh", datasrc);
        }
    }

    /**
     * Fill map with given parameters.
     * Loaded with either previously saved areas or new ones that come in from updateMap's loader.
     */
    private void fillMap(@NonNull List<LatLngBounds> newBounds,
                         @NonNull List<ArcadeLocation> newLocations,
                         @NonNull List<DataSource> newSources) {
        for (ArcadeLocation loc : newLocations)
        {
            if (!loadedLocationIds.contains(loc.getId())) {
                mClusterManager.addItem(loc);
                loadedLocationIds.add(loc.getId());
                loadedLocations.add(loc);
            }
        }
        // Required to force a re-render.
        mClusterManager.cluster();

        loadedAreas.addAll(newBounds);
        for (DataSource src : newSources) {
            loadedSources.put(src.getShortName(), src);
        }
        attributionText.setText(AttributionGenerator.fromSources(loadedSources.values()));
    }


    /** Update UI as MapLoader events happen (data loaded, error, etc.) */
    private MapLoader.Callback mapLoaderCallback = new MapLoader.Callback() {
        @Override
        public void onPreLoad() {
            showProgressBar();
        }

        @Override
        public void onLocationsLoaded(@NonNull LatLngBounds newBounds,
                                      @NonNull List<ArcadeLocation> newLocations,
                                      @NonNull List<DataSource> newSources) {
            fillMap(Collections.singletonList(newBounds), newLocations, newSources);
        }

        @Override
        public void onError(int errorCode, int errorMessageResourceId) {
            showMessage(errorMessageResourceId);
        }

        @Override
        public void onFinish() {
            hideProgressBar();
        }
    };

    /**
     * Test whether the given boundaries have already been loaded
     * @param box Bounding box
     * @return Whether data for the boundaries has been loaded
     */
    private boolean alreadyLoaded(LatLngBounds box) {
        // Test all four corners (best we can do)
        final LatLng northeast = box.northeast;
        final LatLng southwest = box.southwest;
        final LatLng northwest = new LatLng(northeast.latitude, southwest.longitude);
        final LatLng southeast = new LatLng(southwest.latitude, northeast.longitude);

        boolean loaded = false;
        boolean loadedNE = false;
        boolean loadedSW = false;
        boolean loadedNW = false;
        boolean loadedSE = false;

        for (LatLngBounds bounds : loadedAreas) {
            if (bounds.contains(northeast)) loadedNE = true;
            if (bounds.contains(southwest)) loadedSW = true;
            if (bounds.contains(northwest)) loadedNW = true;
            if (bounds.contains(southeast)) loadedSE = true;
            if (loadedNE && loadedSW && loadedNW && loadedSE) break;
        }
        if (loadedNE && loadedSW && loadedNW && loadedSE)
            loaded = true;

        return loaded;
    }

    /**
     * Clears map of all markers, and internal data structures of all loaded areas.
     */
    private void clearMap() {
        mClusterManager.clearItems();
        loadedLocationIds.clear();
        loadedLocations.clear();
        loadedAreas.clear();
        loadedSources.clear();
    }

    /**
     * Get the source corresponding to the given location.
     * If specific source not found, "fallback" source is returned.
     * @return Data source of location.
     */
    private DataSource getSource(ArcadeLocation location) {
        if (loadedSources.containsKey(location.getSrc()))
            return loadedSources.get(location.getSrc());
        else {
            Log.d("MapViewer", "failed to get source: " + location.getSrc());
            return loadedSources.get("fallback");
        }
    }

    /**
     * Zooms and moves the map to the user's last known current location, typically on app startup.
     */
    private void zoomToCurrentLocation() {
        final LocationManager locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        if (locationManager != null) {
            try {
                Location lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (lastKnown != null) {
                    mMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(lastKnown.getLatitude(),
                                            lastKnown.getLongitude()),
                                    BASE_ZOOM));
                }
            } catch (SecurityException e) {
                showMessage(R.string.error_perm_loc);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the list of currently loaded map areas and locations
        outState.putParcelableArrayList("loadedAreas", loadedAreas);
        outState.putParcelableArrayList("loadedLocations", loadedLocations);

        // Save the map of currently loaded sources, as a list
        final ArrayList<DataSource> currSources = new ArrayList<>(loadedSources.size());
        currSources.addAll(loadedSources.values());
        outState.putParcelableArrayList("loadedSources", currSources);

        // Save the current attribution text
        outState.putCharSequence("attributionText", attributionText.getText());
    }

    private void showProgressBar() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (reloadButton != null) reloadButton.setEnabled(false);
    }

    private void hideProgressBar() {
        if (reloadButton != null) reloadButton.setEnabled(true);
        if (progressBar != null) progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Keep toast references mapped by resource id, to prevent excessive repeated toasts.
    private final SparseArray<Toast> toasts = new SparseArray<>();
    @Override
    public void showMessage(int resourceId) {
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
            startPlaceAutocomplete();
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
            // Store current data source preference for future comparison in onResume.
            prevDatasrc = sharedPref.getString(SettingsActivity.KEY_PREF_API_SRC, "");
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private String prevDatasrc = null;
    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null) {
            // Clear all markers and reload current view when a relevant preference changed.
            // After app simplification for 3.0.6, only data source is relevant.
            final String currDatasrc = sharedPref.getString(SettingsActivity.KEY_PREF_API_SRC, "");
            if (prevDatasrc != null && !currDatasrc.equals(prevDatasrc)) {
                clearMap();
                updateMap(false);
            }

            // Enable "My Location" if resuming activity with location permission enabled.
            // i.e. User goes to Settings to enable, then comes back.
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }

            // Move to Places API Autocompleted place if coming back from it.
            if (autocompletedPlace != null) moveMapToAutocompletedPlace();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        mMap.setMyLocationEnabled(true);
                    } catch (SecurityException e) { /* Satisfy linter; it should be granted */ }
                    // Zoom to current location if application was not opened via app link.
                    if (appLink.getPosition() == null) zoomToCurrentLocation();
                } else {
                    showMessage(R.string.error_perm_loc);
                }
        }
    }

    /**
     * Starts the place autocomplete overlay activity (with filter for regions).
     * If Google Play Services requires an update, shows actionable error message to user.
     */
    private void startPlaceAutocomplete() {
        try {
            final AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                    .setTypeFilter(AutocompleteFilter.TYPE_FILTER_CITIES)
                    .build();
            final Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                    .setFilter(typeFilter)
                    .build(this);
            firebaseAnalytics.logEvent(Analytics.Event.PLACES_SEARCH_START, null);
            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesNotAvailableException e) {
            // This exception is not actionable
            e.printStackTrace();
        } catch (GooglePlayServicesRepairableException e) {
            // This exception is actionable; display Play Services update dialog to user
            final Dialog errorDialog = GoogleApiAvailability.getInstance()
                    .getErrorDialog(this, e.getConnectionStatusCode(), PLACE_AUTOCOMPLETE_REQUEST_CODE);
            if (errorDialog != null) errorDialog.show();
        }
    }

    /**
     * Handle results from startActivityForResult.
     * Currently used to handle result from Places Autocomplete widget.
     * Note: this function gets called before onResume.
     * In cases with low memory, this means the order is onCreate, onActivityResult, onResume (with onMapReady undetermined).
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                autocompletedPlace = PlaceAutocomplete.getPlace(this, data);
                firebaseAnalytics.logEvent(Analytics.Event.PLACES_SEARCH_COMPLETE, null);
            } else if (resultCode == RESULT_CANCELED) {
                firebaseAnalytics.logEvent(Analytics.Event.PLACES_SEARCH_CANCELED, null);
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                final Status status = PlaceAutocomplete.getStatus(this, data);
                Log.e("MapViewer", status.getStatusMessage());
                final Bundle params = new Bundle();
                params.putString(Analytics.Param.EXCEPTION_MESSAGE, status.getStatusMessage());
                firebaseAnalytics.logEvent(Analytics.Event.PLACES_SEARCH_ERROR, params);
            }
        }
    }

    /**
     * Move the map to the last user-specified autocomplete location, then sets it to null.
     * The user-specified location is set in onActivityResult (fires before onResume).
     * This function can only be called from onMapReady or onResume (when onMapReady has previously fired).
     */
    private void moveMapToAutocompletedPlace() {
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
                mMap.animateCamera(viewportCameraUpdate);
                success = true;
            } catch (IllegalStateException e) {
                // This exception is thrown when map layout has not yet occurred.
                // See: https://developers.google.com/android/reference/com/google/android/gms/maps/CameraUpdateFactory.html
                Log.e("MapViewer", "Places API: map layout has not occurred; falling back to latLng");
            }
        }
        if (!success) {
            Log.d("MapViewer", "Places API using latLng: " + latLng.toString());
            mMap.animateCamera(latLngCameraUpdate);
        }
        autocompletedPlace = null;
    }

    /**
     * Listener class that activates the action bar on marker click.
     */
    private final ClusterManager.OnClusterItemClickListener<ArcadeLocation> actionModeEnabler =
            new ClusterManager.OnClusterItemClickListener<ArcadeLocation>() {
        @Override
        public boolean onClusterItemClick(ArcadeLocation location) {
            if (actionMode == null) {
                actionMode = startActionMode(actionModeCallback);
            }
            selectedLocation = location;
            trackMapAction("marker_selected", getSource(location));
            return false; // keep the default action of moving view and showing info window
        }
    };

    /**
     * Listener class that de-activates the action bar on clicking elsewhere.
     */
    private final GoogleMap.OnMapClickListener actionModeDisabler = new GoogleMap.OnMapClickListener() {
        @Override
        public void onMapClick(LatLng coords) {
            if (actionMode != null) {
                actionMode.finish();
            }
        }
    };

    /**
     * Handles action mode creation, destruction, and actions.
     * Template: https://developer.android.com/guide/topics/ui/menus.html#CAB
     */
    private ActionMode actionMode = null;
    private ArcadeLocation selectedLocation = null;
    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.context_menu, menu);

            // Set status bar color to match action mode background color.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(
                        ThemeUtil.getThemeColor(getTheme(), R.attr.actionModeStatusBarColor));
            }

            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (selectedLocation == null) return false;

            final LocationActions actions =
                    new LocationActions(selectedLocation, getSource(selectedLocation));

            switch (item.getItemId()) {
                case R.id.action_navigate:
                    actions.navigate(MapViewer.this);
                    return true;
                case R.id.action_moreinfo:
                    final boolean useCustomTabs = sharedPref
                            .getBoolean(SettingsActivity.KEY_PREF_CUSTOMTABS, true);
                    actions.moreInfo(MapViewer.this, useCustomTabs);
                    return true;
                case R.id.action_copygps:
                    actions.copyGps(MapViewer.this, MapViewer.this);
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            if (selectedLocation != null) {
                final Marker selectedMarker = mClusterRenderer.getMarker(selectedLocation);
                if (selectedMarker != null) {
                    selectedMarker.hideInfoWindow();
                }
                trackMapAction("marker_deselected", getSource(selectedLocation));
            }

            // Set status bar color back to default app color.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(
                        ThemeUtil.getThemeColor(getTheme(), android.R.attr.colorPrimaryDark));
            }
        }

    };

    /**
     * Info window click listener which triggers the "More Info" action.
     */
    private final ClusterManager.OnClusterItemInfoWindowClickListener<ArcadeLocation> moreInfoListener =
            new ClusterManager.OnClusterItemInfoWindowClickListener<ArcadeLocation>() {
        @Override
        public void onClusterItemInfoWindowClick(ArcadeLocation location) {
            final DataSource source = getSource(location);
            final LocationActions actions = new LocationActions(location, source);
            trackMapAction("marker_infowindow_clicked", source);
            final boolean useCustomTabs = sharedPref
                    .getBoolean(SettingsActivity.KEY_PREF_CUSTOMTABS, true);
            actions.moreInfo(MapViewer.this, useCustomTabs);
        }
    };

    /** Track a user-initiated map action with the given active data source. */
    private void trackMapAction(@NonNull String actionType, @NonNull String activeSource) {
        Bundle params = new Bundle();
        params.putString(Analytics.Param.ACTION_TYPE, actionType);
        params.putString(Analytics.Param.ACTIVE_DATASRC, activeSource);
        firebaseAnalytics.logEvent(Analytics.Event.MAP_ACTION, params);
    }
    /** Track a user-initiated map action with the given active data source. */
    private void trackMapAction(@NonNull String actionType, @NonNull DataSource activeSource) {
        trackMapAction(actionType, activeSource.getShortName());
    }
}

