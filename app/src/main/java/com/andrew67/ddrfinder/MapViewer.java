/*
 * Copyright (c) 2013 Luis Torres
 * Web: https://github.com/ltorres8890/Clima
 * 
 * Copyright (c) 2013-2015 Andrés Cordero
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

package com.andrew67.ddrfinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.andrew67.ddrfinder.adapters.MapLoader;
import com.andrew67.ddrfinder.interfaces.ArcadeLocation;
import com.andrew67.ddrfinder.interfaces.MessageDisplay;
import com.andrew67.ddrfinder.interfaces.ProgressBarController;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;


public class MapViewer extends FragmentActivity
	implements ProgressBarController, MessageDisplay {
	
	public static final int BASE_ZOOM = 12;
	
	private GoogleMap mMap;
	private MenuItem reloadButton;

	private final Map<Marker,ArcadeLocation> currentMarkers = new HashMap<>();
	// Set as ArrayList instead of List due to Bundle packing
	private final ArrayList<LatLngBounds> loadedAreas =	new ArrayList<>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.map_viewer);
		
		setProgressBarIndeterminate(true);
		setProgressBarIndeterminateVisibility(false);
						
		final SupportMapFragment mMapFragment =
				(SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		mMap = mMapFragment.getMap();

		try {
			mMap.setMyLocationEnabled(true);
			final LocationManager locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
			final Location lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			// Animate to user's current location if first load
			if (lastKnown != null && savedInstanceState == null) {
				mMap.animateCamera(
						CameraUpdateFactory.newLatLngZoom(
								new LatLng(lastKnown.getLatitude(),
										lastKnown.getLongitude()),
								BASE_ZOOM));
			}
		} catch (SecurityException e) {
			showMessage(R.string.error_perm_loc);
		}
		
		// Restore previously loaded areas and locations if available
		// (and re-create the location markers)
		if (savedInstanceState != null &&
				savedInstanceState.containsKey("loadedAreas") &&
				savedInstanceState.containsKey("loadedLocations")) {
			final ArrayList<LatLngBounds> savedLoadedAreas =
					savedInstanceState.getParcelableArrayList("loadedAreas");
			if (savedLoadedAreas != null) loadedAreas.addAll(savedLoadedAreas);
			final ArrayList<ArcadeLocation> savedLoadedLocations =
					savedInstanceState.getParcelableArrayList("loadedLocations");
			MapLoader.fillMap(mMap, currentMarkers, savedLoadedLocations);
		}
		
		mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
			
			@Override
			public void onCameraChange(CameraPosition position) {
				updateMap(false);
			}
		});
		mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
			
			@Override
			public void onInfoWindowClick(Marker marker) {
				final ArcadeLocation location = currentMarkers.get(marker);

				startActivity(new Intent(MapViewer.this, LocationActions.class)
						.putExtra("location", location));
			}
		});
	}
	
	/**
	 * Load arcade locations into the map
	 * @param force Whether to ignore already loaded areas and load them again
	 */
	private void updateMap(boolean force) {
		final LatLngBounds box = mMap.getProjection().getVisibleRegion().latLngBounds;
		final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		if (force || !alreadyLoaded(box)) {
			new MapLoader(mMap, currentMarkers, this, this, loadedAreas, sharedPref).execute(box);
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
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		// Save the list of currently loaded map areas and locations
		outState.putParcelableArrayList("loadedAreas", loadedAreas);
		final ArrayList<ArcadeLocation> loadedLocations = new ArrayList<>(currentMarkers.size());
		loadedLocations.addAll(currentMarkers.values());
		outState.putParcelableArrayList("loadedLocations", loadedLocations);
	}
	
	@Override
	public void showProgressBar() {
		if (reloadButton != null) reloadButton.setVisible(false);
		setProgressBarIndeterminateVisibility(true);
	}
	
	@Override
	public void hideProgressBar() {
		setProgressBarIndeterminateVisibility(false);
		if (reloadButton != null) reloadButton.setVisible(true);
	}

	@Override
	public void showMessage(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void showMessage(int resourceId) {
		Toast.makeText(this, resourceId, Toast.LENGTH_SHORT).show();
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
			updateMap(false);
		}
	}
}

