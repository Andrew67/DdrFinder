/*
 * Copyright (c) 2013 Luis Torres
 * Web: https://github.com/ltorres8890/Clima
 * 
 * Copyright (c) 2013-2016 Andrés Cordero
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
import java.util.HashMap;
import java.util.Map;

import com.andrew67.ddrfinder.R;
import com.andrew67.ddrfinder.adapters.MapLoader;
import com.andrew67.ddrfinder.adapters.MapLoaderV1;
import com.andrew67.ddrfinder.adapters.MapLoaderV3;
import com.andrew67.ddrfinder.interfaces.ArcadeLocation;
import com.andrew67.ddrfinder.interfaces.DataSource;
import com.andrew67.ddrfinder.interfaces.MessageDisplay;
import com.andrew67.ddrfinder.interfaces.ProgressBarController;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.lsjwzh.widget.materialloadingprogressbar.CircleProgressBar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;


public class MapViewer extends FragmentActivity
	implements ProgressBarController, MessageDisplay {
	
	public static final int BASE_ZOOM = 12;
	private static final int PERMISSIONS_REQUEST_LOCATION = 1;
	
	private GoogleMap mMap;
	private MenuItem reloadButton;
	private CircleProgressBar progressBar;

	private final Map<Marker,ArcadeLocation> currentMarkers = new HashMap<>();
	// Set as ArrayList instead of List due to Bundle packing
	private final ArrayList<LatLngBounds> loadedAreas =	new ArrayList<>();

	/**
	 * Loaded data sources, keyed by source name
	 */
	private final Map<String,DataSource> loadedSources = new HashMap<>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.map_viewer);

		progressBar = (CircleProgressBar) findViewById(R.id.progressBar);
						
		final SupportMapFragment mMapFragment =
				(SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		mMap = mMapFragment.getMap();

		// Check for location permission, and request if disabled
		// This permission allows the user to locate themselves on the map
		if (ContextCompat.checkSelfPermission(this,
				android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			mMap.setMyLocationEnabled(true);
			if (savedInstanceState == null) {
				zoomToCurrentLocation();
			}
		} else {
			ActivityCompat.requestPermissions(this,
					new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
					PERMISSIONS_REQUEST_LOCATION);
		}
		
		// Restore previously loaded areas locations, and sources if available
		// (and re-create the location markers)
		if (savedInstanceState != null &&
				savedInstanceState.containsKey("loadedAreas") &&
				savedInstanceState.containsKey("loadedLocations") &&
				savedInstanceState.containsKey("loadedSources")) {
			final ArrayList<LatLngBounds> savedLoadedAreas =
					savedInstanceState.getParcelableArrayList("loadedAreas");
			if (savedLoadedAreas != null) loadedAreas.addAll(savedLoadedAreas);
			final ArrayList<ArcadeLocation> savedLoadedLocations =
					savedInstanceState.getParcelableArrayList("loadedLocations");

			final ArrayList<DataSource> savedLoadedSources =
					savedInstanceState.getParcelableArrayList("loadedSources");
			if (savedLoadedSources != null) {
				for (DataSource src : savedLoadedSources) {
					loadedSources.put(src.getShortName(), src);
				}
			}

			MapLoader.fillMap(mMap, currentMarkers, savedLoadedLocations);
		}
		
		mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
			
			@Override
			public void onCameraChange(CameraPosition position) {
				if (isAutoloadEnabled()) {
					updateMap(false);
				}
			}
		});
		mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
			
			@Override
			public void onInfoWindowClick(Marker marker) {
				final ArcadeLocation location = currentMarkers.get(marker);

				startActivity(new Intent(MapViewer.this, LocationActions.class)
						.putExtra("location", location)
                        .putExtra("source", getSource(location)));
			}
		});
	}

	/**
	 * Whether the autoload flag is enabled for map scrolling.
     */
	private boolean isAutoloadEnabled() {
		return PreferenceManager.getDefaultSharedPreferences(this)
				.getBoolean(SettingsActivity.KEY_PREF_AUTOLOAD, true);
	}
	
	/**
	 * Load arcade locations into the map
	 * @param force Whether to ignore already loaded areas and load them again
	 */
	private void updateMap(boolean force) {
		final LatLngBounds box = mMap.getProjection().getVisibleRegion().latLngBounds;
		final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		if (force || !alreadyLoaded(box)) {
            final int version = Integer.parseInt(
                    sharedPref.getString(SettingsActivity.KEY_PREF_API_VERSION, ""));
            switch (version) {
                case SettingsActivity.API_V11:
                    new MapLoaderV1(mMap, currentMarkers, this, this,
                            loadedAreas, loadedSources, sharedPref).execute(box);
                    break;
                case SettingsActivity.API_V30:
                    new MapLoaderV3(mMap, currentMarkers, this, this,
                            loadedAreas, loadedSources, sharedPref).execute(box);
                    break;
                default:
                    showMessage(R.string.error_api_ver);
                    Log.d("MapViewer", "unsupported API version requested: " + version);
                    break;
            }

		}
	}
	
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
		mMap.clear();
		currentMarkers.clear();
		loadedAreas.clear();
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
		Location lastKnown = null;
		try {
			lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		} catch (SecurityException e) {
			showMessage(R.string.error_perm_loc);
		}
		if (lastKnown != null) {
			mMap.animateCamera(
					CameraUpdateFactory.newLatLngZoom(
							new LatLng(lastKnown.getLatitude(),
									lastKnown.getLongitude()),
							BASE_ZOOM));
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		// Save the list of currently loaded map areas and locations
		outState.putParcelableArrayList("loadedAreas", loadedAreas);
		final ArrayList<ArcadeLocation> loadedLocations = new ArrayList<>(currentMarkers.size());
		loadedLocations.addAll(currentMarkers.values());
		outState.putParcelableArrayList("loadedLocations", loadedLocations);

		// Save the map of currently loaded sources, as a list
		final ArrayList<DataSource> currSources = new ArrayList<>(loadedSources.size());
		currSources.addAll(loadedSources.values());
		outState.putParcelableArrayList("loadedSources", currSources);
	}
	
	@Override
	public void showProgressBar() {
		progressBar.setVisibility(View.VISIBLE);
		if (reloadButton != null) reloadButton.setVisible(false);
	}
	
	@Override
	public void hideProgressBar() {
		if (reloadButton != null) reloadButton.setVisible(true);
		progressBar.setVisibility(View.INVISIBLE);
	}

	@Override
	public void showMessage(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	// Keep toast references mapped by resource id, to prevent excessive repeated toasts.
	private Map<Integer, Toast> toasts = new HashMap<>();
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
		case R.id.action_reload:
			updateMap(true);
			return true;
		case R.id.action_about:
			startActivity(new Intent(this, About.class));
			return true;
		case R.id.action_settings:
			// Store current preferences for future comparison in onResume.
			prevPrefs = PreferenceManager.getDefaultSharedPreferences(this).getAll();
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private Map<String, ?> prevPrefs = null;
	@Override
	protected void onResume() {
		super.onResume();
		// Clear all markers and reload current view when a preference changed.
		final Map<String, ?> currPrefs = PreferenceManager.getDefaultSharedPreferences(this).getAll();
		if (prevPrefs != null && !currPrefs.equals(prevPrefs)) {
			clearMap();
			if (isAutoloadEnabled()) {
				updateMap(false);
			}
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
					zoomToCurrentLocation();
				} else {
					showMessage(R.string.error_perm_loc);
				}
		}
	}
}

