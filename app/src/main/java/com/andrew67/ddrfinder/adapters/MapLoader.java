/*
 * Copyright (c) 2013 Luis Torres
 * Web: https://github.com/ltorres8890/Clima
 * 
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

package com.andrew67.ddrfinder.adapters;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

import com.andrew67.ddrfinder.R;
import com.andrew67.ddrfinder.data.ApiResult;
import com.andrew67.ddrfinder.data.ArcadeLocation;
import com.andrew67.ddrfinder.interfaces.MessageDisplay;
import com.andrew67.ddrfinder.interfaces.ProgressBarController;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapLoader extends AsyncTask<LatLngBounds, Void, ApiResult>{

		private static final String LOADER_API_URL = "http://www.ddrfinder.tk/locate.php";
		private final GoogleMap map;
		private final Map<Marker,ArcadeLocation> markers;
		private final ProgressBarController pbc;
		private final MessageDisplay display;
		private final List<LatLngBounds> areas;
		
		/** Maximum distance for box boundaries, in degrees */
		private int MAX_DISTANCE = 1;
		
		/** Precomiled pattern for searching for closed tag in names */
		private static final Pattern closedPattern =
				Pattern.compile(".*(?i:closed).*");
		
		public MapLoader(GoogleMap map, Map<Marker,ArcadeLocation> markers,
				ProgressBarController pbc, MessageDisplay display,
				List<LatLngBounds> areas) {
			super();
			this.map = map;
			this.markers = markers;
			this.pbc = pbc;
			this.display = display;
			this.areas = areas;
			
			// Show indeterminate progress bar
			// Assumes this class is constructed followed by a call to execute()
			// where the bar is hidden on data load completion
			pbc.showProgressBar();
		}
		
		@Override
		protected ApiResult doInBackground(LatLngBounds... boxes) {
			// Fetch machine data in JSON format
			JSONArray jArray = new JSONArray();
			try {
				if (boxes.length == 0) throw new Exception();
				final LatLngBounds box = boxes[0];
				
				// If box boundaries exceed valid API boundaries,
				// avoid making the HTTP request altogether.
				if (Math.abs(box.northeast.latitude - box.southwest.latitude) > MAX_DISTANCE
					|| Math.abs(box.northeast.latitude - box.southwest.latitude) > MAX_DISTANCE) {
					return new ApiResult(ApiResult.ERROR_ZOOM);
				}
				
				final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair("source", "android"));
				params.add(new BasicNameValuePair("latupper", "" + box.northeast.latitude));
				params.add(new BasicNameValuePair("longupper", "" + box.northeast.longitude));
				params.add(new BasicNameValuePair("latlower", "" + box.southwest.latitude));
				params.add(new BasicNameValuePair("longlower", "" + box.southwest.longitude));
				
				final HttpClient client = new DefaultHttpClient();
				final HttpGet get = new HttpGet(LOADER_API_URL + "?" + URLEncodedUtils.format(params, "utf-8"));
				
				final HttpResponse response = client.execute(get);
				final int statusCode = response.getStatusLine().getStatusCode();
				Log.d("api", "" + statusCode);
				
				// Data loaded OK
				if (statusCode == 200) {
					final InputStream is = response.getEntity().getContent();
					final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					final StringBuilder sb = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) sb.append(line);
					Log.d("api", sb.toString());
					jArray = new JSONArray(sb.toString());
				}
				// Code used for invalid parameters; in this case exceeding
				// the limits of the boundary box
				else if (statusCode == 400) {
					return new ApiResult(ApiResult.ERROR_ZOOM);
				}
				// Unexpected error code
				else {
					return new ApiResult(ApiResult.ERROR_API);
				}
			} 
			catch(Exception e)
			{
				e.printStackTrace();
			}
			
			// Return list
			ArrayList<ArcadeLocation> out = new ArrayList<ArcadeLocation>();
			try{
				for (int i = 0; i < jArray.length(); ++i)
				{
					final JSONObject obj = (JSONObject) jArray.get(i);
					final String name = obj.getString("name");
					
					boolean closed = false;
					if (closedPattern.matcher(name).matches()) {
						closed = true;
					}
					// Fields added after ddr-finder 1.0 API should be
					// explicitly tested for, in order to maintain
					// compatibility with deployments of older versions
					boolean hasDDR = false;
					if (obj.has("hasDDR") && obj.getInt("hasDDR") == 1) {
						hasDDR = true;
					}
					
					out.add(new ArcadeLocation(
							obj.getInt("id"),
							name,
							obj.getString("city"),
							new LatLng(obj.getDouble("latitude"),
									obj.getDouble("longitude")),
							hasDDR,
							closed));
				}
			}
			catch(Exception e)
			{
				out.clear();
			}
			return new ApiResult(out, boxes[0]);
		}
		
		@Override
		protected void onPostExecute(ApiResult result) {
			super.onPostExecute(result);
			pbc.hideProgressBar();
			
			switch(result.getErrorCode()) {
			case ApiResult.ERROR_NONE:
				fillMap(map, markers, result.getLocations());
				areas.add(result.getBounds());
				break;
			case ApiResult.ERROR_ZOOM:
				display.showMessage(R.string.error_zoom);
				break;
			default:
				display.showMessage(R.string.error_api);
			}
		}
		
		public static void fillMap(GoogleMap map,
				Map<Marker,ArcadeLocation> markers, List<ArcadeLocation> feed){
			for (ArcadeLocation loc : feed)
			{
				addMarker(map, markers, loc);
			}
		}
		
		public static void addMarker(GoogleMap map,
				Map<Marker,ArcadeLocation> markers, ArcadeLocation loc) {
			float hue = BitmapDescriptorFactory.HUE_RED;
			
			// Has the location been tagged as closed?
			if (loc.isClosed()) {
				hue = BitmapDescriptorFactory.HUE_ORANGE;
			}
			// Does the location have a DDR machine?
			else if (loc.hasDDR()) {
				hue = BitmapDescriptorFactory.HUE_AZURE;
			}
			
			markers.put(
					map.addMarker(
							new MarkerOptions()
							.position(loc.getLocation())
							.title(loc.getName())
							.snippet(loc.getCity())
							.icon(BitmapDescriptorFactory.defaultMarker(hue))),
					loc);
		}
	}