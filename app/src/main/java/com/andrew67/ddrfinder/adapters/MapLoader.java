/*
 * Copyright (c) 2015-2017 Andrés Cordero
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

import android.os.AsyncTask;

import com.andrew67.ddrfinder.R;
import com.andrew67.ddrfinder.interfaces.ApiResult;
import com.andrew67.ddrfinder.interfaces.ArcadeLocation;
import com.andrew67.ddrfinder.interfaces.DataSource;
import com.andrew67.ddrfinder.interfaces.MessageDisplay;
import com.andrew67.ddrfinder.interfaces.ProgressBarController;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.clustering.ClusterManager;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class MapLoader extends AsyncTask<LatLngBounds, Void, ApiResult> {
    private final ClusterManager<ArcadeLocation> clusterManager;
    private final List<ArcadeLocation> loadedLocations;
    private final Set<Integer> loadedArcadeIds;
    private final ProgressBarController pbc;
    private final MessageDisplay display;
    private final List<LatLngBounds> areas;
    private final Map<String,DataSource> sources;
    final String apiUrl;
    final String datasrc;

    MapLoader(ClusterManager<ArcadeLocation> clusterManager,
                 List<ArcadeLocation> loadedLocations, Set<Integer> loadedArcadeIds,
                 ProgressBarController pbc, MessageDisplay display,
                 List<LatLngBounds> areas, Map<String,DataSource> sources,
                 String apiUrl, String datasrc) {
        super();
        this.clusterManager = clusterManager;
        this.loadedLocations = loadedLocations;
        this.loadedArcadeIds = loadedArcadeIds;
        this.pbc = pbc;
        this.display = display;
        this.areas = areas;
        this.sources = sources;
        this.apiUrl = apiUrl;
        this.datasrc = datasrc;

        // Show indeterminate progress bar
        // Assumes this class is constructed followed by a call to execute()
        // where the bar is hidden on data load completion
        pbc.showProgressBar();
    }


    @Override
    protected void onPostExecute(ApiResult result) {
        super.onPostExecute(result);
        pbc.hideProgressBar();

        if (result == null) {
            display.showMessage(R.string.error_api_unexpected);
            return;
        }

        switch(result.getErrorCode()) {
            case ApiResult.ERROR_OK:
                if (result.getLocations().size() == 0) {
                    display.showMessage(R.string.area_no_results);
                }

                fillMap(clusterManager, loadedLocations, loadedArcadeIds, result.getLocations());
                areas.add(result.getBounds());

                for (DataSource src : result.getSources()) {
                    sources.put(src.getShortName(), src);
                }

                break;
            case ApiResult.ERROR_OVERSIZED_BOX:
                display.showMessage(R.string.error_zoom);
                break;
            case ApiResult.ERROR_DATA_SOURCE:
                display.showMessage(R.string.error_datasrc);
                break;
            case ApiResult.ERROR_CLIENT_API_VERSION:
                display.showMessage(R.string.error_api_ver);
            default:
                display.showMessage(R.string.error_api);
        }
    }

    public static void fillMap(ClusterManager<ArcadeLocation> clusterManager, List<ArcadeLocation> loadedLocations,
                               Set<Integer> loadedArcadeIds, List<ArcadeLocation> feed) {
        for (ArcadeLocation loc : feed)
        {
            if (!loadedArcadeIds.contains(loc.getId())) {
                clusterManager.addItem(loc);
                loadedArcadeIds.add(loc.getId());
                loadedLocations.add(loc);
            }
        }
        // Required to force a re-render.
        clusterManager.cluster();
    }
}
