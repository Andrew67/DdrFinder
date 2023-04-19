/*
 * Copyright (c) 2015-2023 Andrés Cordero
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

package com.andrew67.ddrfinder.arcades.util;

import android.os.AsyncTask;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.andrew67.ddrfinder.BuildConfig;
import com.andrew67.ddrfinder.R;
import com.andrew67.ddrfinder.arcades.model.ApiResult;
import com.google.android.gms.maps.model.LatLngBounds;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Loads arcades for given bounds box and data source using the DDR Finder API on the server
 */
public class NetworkMapLoader extends AsyncTask<LatLngBounds, Void, ApiResult> {
    private static final HttpUrl apiUrl = HttpUrl.get(BuildConfig.API_BASE_URL);
    private static final OkHttpClient client = new OkHttpClient();
    private static final JsonAdapter<ApiResult> jsonAdapter = new Moshi.Builder().build()
            .adapter(ApiResult.class);

    private final String datasrc;
    private final MapLoaderCallback callback;

    /**
     * Initialize a NetworkMapLoader instance
     * @param datasrc The data source to use for arcade locations
     * @param callback The callback to use in the UI thread.
     */
    NetworkMapLoader(@NonNull String datasrc, @NonNull MapLoaderCallback callback) {
        super();
        this.datasrc = datasrc;
        this.callback = callback;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        callback.onPreLoad();
    }

    @Override
    protected ApiResult doInBackground(LatLngBounds... boxes) {
        ApiResult result = null;
        try {
            if (boxes.length == 0) throw new IllegalArgumentException("No boxes passed to doInBackground");
            final LatLngBounds box = boxes[0];

            final HttpUrl requestUrl = apiUrl.newBuilder()
                    .addQueryParameter("version", "30")
                    .addQueryParameter("canHandleLargeDataset", "")
                    .addQueryParameter("datasrc", datasrc)
                    .addQueryParameter("latupper", "" + box.northeast.latitude)
                    .addQueryParameter("lngupper", "" + box.northeast.longitude)
                    .addQueryParameter("latlower", "" + box.southwest.latitude)
                    .addQueryParameter("lnglower", "" + box.southwest.longitude)
                    .build();

            if (BuildConfig.DEBUG) Log.d("NetworkMapLoader", "Request URL: " + requestUrl);
            else Log.d("NetworkMapLoader", "Performing URL request (use debug build to see URL)");
            final Request get = new Request.Builder()
                    .header("User-Agent", BuildConfig.APPLICATION_ID + " " + BuildConfig.VERSION_NAME
                            + "/Android?SDK=" + Build.VERSION.SDK_INT)
                    .url(requestUrl)
                    .build();

            final Response response = client.newCall(get).execute();
            final int statusCode = response.code();
            Log.d("NetworkMapLoader", "Status code: " + statusCode);

            // Data/error loaded OK
            if (statusCode == 200 || statusCode == 400) {
                final ResponseBody responseBody = response.body();
                assert responseBody != null;
                result = jsonAdapter.fromJson(responseBody.source());
                responseBody.close();
                assert result != null;
                result.setBounds(box);
                Log.d("NetworkMapLoader", "Response JSON parse complete");
            }
            // Unexpected error code
            else {
                throw new RuntimeException("Unexpected HTTP status code: " + statusCode);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    protected void onPostExecute(@Nullable ApiResult result) {
        super.onPostExecute(result);

        if (result == null) {
            callback.onError(ApiResult.ERROR_UNEXPECTED, R.string.error_api_unexpected);
        } else {
            switch(result.getErrorCode()) {
                case ApiResult.ERROR_OK:
                    if (result.getLocations().size() == 0) {
                        callback.onError(ApiResult.ERROR_NO_RESULTS, R.string.area_no_results);
                    }
                    callback.onLocationsLoaded(result);
                    break;
                case ApiResult.ERROR_OVERSIZED_BOX:
                    callback.onError(ApiResult.ERROR_OVERSIZED_BOX, R.string.error_zoom);
                    break;
                case ApiResult.ERROR_DATA_SOURCE:
                    callback.onError(ApiResult.ERROR_DATA_SOURCE, R.string.error_datasrc);
                    break;
                case ApiResult.ERROR_CLIENT_API_VERSION:
                    callback.onError(ApiResult.ERROR_CLIENT_API_VERSION, R.string.error_api_ver);
                    break;
                default:
                    callback.onError(ApiResult.ERROR_UNEXPECTED, R.string.error_api);
                    break;
            }
        }

        callback.onFinish();
    }
}
