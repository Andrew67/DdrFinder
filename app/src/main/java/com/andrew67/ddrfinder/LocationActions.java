/*
 * Copyright (c) 2013 Andrés Cordero 
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

import com.andrew67.ddrfinder.adapters.ActionListAdapter;
import com.andrew67.ddrfinder.data.ArcadeLocation;
import com.google.android.gms.maps.model.LatLng;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ListActivity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class LocationActions extends ListActivity {
	private ArcadeLocation location;
	private ActionListAdapter adapter;
	private static final String MORE_INFO_PREFIX =
		"http://m.zenius-i-vanisher.com/arcadelocations_viewarcade.php?locationid=";
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			final ActionBar actionBar = getActionBar();
			if (actionBar != null) {
				actionBar.setDisplayHomeAsUpEnabled(true);
			}
		}
		
		location = (ArcadeLocation) getIntent().getExtras().get("location");
		
		setTitle(location.getName());
		adapter = new ActionListAdapter(this);
		setListAdapter(adapter);
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
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final LatLng coordinates = location.getLocation();
		switch ((int) adapter.getItemId(position)) {
		case ActionListAdapter.ACTION_NAVIGATE:
			final String label = location.getName()
					.replace('(', '[').replace(')', ']');
			startActivity(new Intent(Intent.ACTION_VIEW,
					Uri.parse("geo:" + coordinates.latitude + "," +
							coordinates.longitude + "?q=" + coordinates.latitude +
							"," + coordinates.longitude + "(" + label + ")")));
			break;
		case ActionListAdapter.ACTION_MOREINFO:
			startActivity(new Intent(Intent.ACTION_VIEW,
					Uri.parse(MORE_INFO_PREFIX + location.getId())));
			break;
		// API 11 code; this list item should not exist on older devices
		case ActionListAdapter.ACTION_COPYGPS:
			final ClipboardManager clipboard =
				(ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setPrimaryClip(ClipData.newPlainText("gps",
					coordinates.latitude + ", " + coordinates.longitude));
			Toast.makeText(this, R.string.copy_complete, Toast.LENGTH_SHORT).show();
			break;
		// This list item only exists when Sygic is installed
		case ActionListAdapter.ACTION_SYGIC_DRIVE:
			startActivity(new Intent(Intent.ACTION_VIEW,
					Uri.parse("com.sygic.aura://coordinate|"
							+ coordinates.longitude + "|" 
							+ coordinates.latitude + "|drive")));
			break;
		// This list item only exists when Sygic is installed
		case ActionListAdapter.ACTION_SYGIC_WALK:
			startActivity(new Intent(Intent.ACTION_VIEW,
					Uri.parse("com.sygic.aura://coordinate|"
							+ coordinates.longitude + "|" 
							+ coordinates.latitude + "|walk")));
			break;
		default:
			super.onListItemClick(l, v, position, id);
		}
	}
}
