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

package com.andrew67.ddrfinder.adapters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.andrew67.ddrfinder.BuildConfig;
import com.andrew67.ddrfinder.interfaces.ApiResult;
import com.andrew67.ddrfinder.interfaces.ArcadeLocation;
import com.andrew67.ddrfinder.interfaces.DataSource;
import com.andrew67.ddrfinder.interfaces.MessageDisplay;
import com.andrew67.ddrfinder.interfaces.ProgressBarController;
import com.andrew67.ddrfinder.model.v1.ApiResultV1;
import com.andrew67.ddrfinder.model.v1.ArcadeLocationV1;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.clustering.ClusterManager;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

public class MapLoaderV1 extends MapLoader {

    public MapLoaderV1(ClusterManager<ArcadeLocation> clusterManager,
                       List<ArcadeLocation> loadedLocations, Set<Integer> loadedArcadeIds,
                       ProgressBarController pbc, MessageDisplay display,
                       List<LatLngBounds> areas, Map<String,DataSource> sources,
                       SharedPreferences sharedPref, String apiUrl) {
        super(clusterManager, loadedLocations, loadedArcadeIds, pbc, display, areas, sources, sharedPref, apiUrl);
    }

    @Override
    protected ApiResult doInBackground(LatLngBounds... boxes) {
        // Fetch machine data in JSON format
        JSONArray jArray = new JSONArray();
        try {
            if (boxes.length == 0) throw new IllegalArgumentException("No boxes passed to doInBackground");
            final LatLngBounds box = boxes[0];

            final OkHttpClient client = new OkHttpClient();
            final HttpUrl requestURL = HttpUrl.parse(apiUrl).newBuilder()
                    .addQueryParameter("source", "android")
                    .addQueryParameter("latupper", "" + box.northeast.latitude)
                    .addQueryParameter("longupper", "" + box.northeast.longitude)
                    .addQueryParameter("latlower", "" + box.southwest.latitude)
                    .addQueryParameter("longlower", "" + box.southwest.longitude)
                    .build();

            Log.d("api", "Request URL: " + requestURL);
            final Request get = new Request.Builder()
                    .header("User-Agent", BuildConfig.APPLICATION_ID + " " + BuildConfig.VERSION_NAME
                            + "/Android?SDK=" + Build.VERSION.SDK_INT)
                    .url(requestURL)
                    .build();

            final Response response = client.newCall(get).execute();
            final int statusCode = response.code();
            Log.d("api", "Status code: " + statusCode);

            // Data loaded OK
            if (statusCode == 200) {
                final String jResponse = response.body().string();
                Log.d("api", "Raw API result: " + jResponse);
                jArray = new JSONArray(jResponse);
            }
            // Code used for invalid parameters; in this case exceeding
            // the limits of the boundary box
            else if (statusCode == 400) {
                return new ApiResultV1(ApiResultV1.ERROR_ZOOM);
            }
            // Unexpected error code
            else {
                return new ApiResultV1(ApiResultV1.ERROR_API);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        // Return list
        ArrayList<ArcadeLocation> out = new ArrayList<>();
        try{
            for (int i = 0; i < jArray.length(); ++i)
            {
                final JSONObject obj = (JSONObject) jArray.get(i);
                final String name = obj.getString("name");

                boolean closed = false;
                if (ArcadeLocation.CLOSED.matcher(name).matches()) {
                    closed = true;
                }
                // Fields added after ddr-finder 1.0 API should be
                // explicitly tested for, in order to maintain
                // compatibility with deployments of older versions
                boolean hasDDR = false;
                if (obj.has("hasDDR") && obj.getInt("hasDDR") == 1) {
                    hasDDR = true;
                }

                out.add(new ArcadeLocationV1(
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
        return new ApiResultV1(out, boxes[0]);
    }

}