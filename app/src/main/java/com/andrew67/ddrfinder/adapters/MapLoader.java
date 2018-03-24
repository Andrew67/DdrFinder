/*
 * Copyright (c) 2015-2018 Andrés Cordero
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
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;

import com.andrew67.ddrfinder.BuildConfig;
import com.andrew67.ddrfinder.R;
import com.andrew67.ddrfinder.model.v3.ApiResult;
import com.andrew67.ddrfinder.model.v3.ArcadeLocation;
import com.andrew67.ddrfinder.model.v3.DataSource;
import com.google.android.gms.maps.model.LatLngBounds;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.lang.ref.WeakReference;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MapLoader extends AsyncTask<LatLngBounds, Void, ApiResult> {
    private static final HttpUrl apiUrl = HttpUrl.parse(BuildConfig.API_BASE_URL);
    private static final OkHttpClient client = new OkHttpClient();
    private static final JsonAdapter<ApiResult> jsonAdapter = new Moshi.Builder().build()
            .adapter(ApiResult.class);

    private final String datasrc;
    private final WeakReference<Callback> callbackWeakReference;

    /**
     * Initialize a MapLoader instance
     * @param datasrc The data source to use for arcade locations
     * @param callback The callback to use in the UI thread. Do not use a lambda, as a weak reference is used to aid in garbage collection of views.
     */
    public MapLoader(@NonNull String datasrc, @Nullable Callback callback) {
        super();
        this.datasrc = datasrc;
        this.callbackWeakReference = new WeakReference<>(callback);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        final Callback callback = callbackWeakReference.get();
        if (callback != null) callback.onPreLoad();
    }

    @Override
    protected ApiResult doInBackground(LatLngBounds... boxes) {
        ApiResult result = null;
        try {
            if (boxes.length == 0) throw new IllegalArgumentException("No boxes passed to doInBackground");
            final LatLngBounds box = boxes[0];

            assert apiUrl != null;
            final HttpUrl requestUrl = apiUrl.newBuilder()
                    .addQueryParameter("version", "30")
                    .addQueryParameter("canHandleLargeDataset", "")
                    .addQueryParameter("datasrc", datasrc)
                    .addQueryParameter("latupper", "" + box.northeast.latitude)
                    .addQueryParameter("lngupper", "" + box.northeast.longitude)
                    .addQueryParameter("latlower", "" + box.southwest.latitude)
                    .addQueryParameter("lnglower", "" + box.southwest.longitude)
                    .build();

            Log.d("api", "Request URL: " + requestUrl);
            final Request get = new Request.Builder()
                    .header("User-Agent", BuildConfig.APPLICATION_ID + " " + BuildConfig.VERSION_NAME
                            + "/Android?SDK=" + Build.VERSION.SDK_INT)
                    .url(requestUrl)
                    .build();

            final Response response = client.newCall(get).execute();
            final int statusCode = response.code();
            Log.d("api", "Status code: " + statusCode);

            // Data/error loaded OK
            if (statusCode == 200 || statusCode == 400) {
                final ResponseBody responseBody = response.body();
                assert responseBody != null;
                result = jsonAdapter.fromJson(responseBody.source());
                assert result != null;
                result.setBounds(box);
                Log.d("api", "Response JSON parse complete");
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

        final Callback callback = callbackWeakReference.get();
        if (callback == null) return;

        if (result == null) {
            callback.onError(ApiResult.ERROR_UNEXPECTED, R.string.error_api_unexpected);
        } else {
            switch(result.getErrorCode()) {
                case ApiResult.ERROR_OK:
                    if (result.getLocations().size() == 0) {
                        callback.onError(ApiResult.ERROR_NO_RESULTS, R.string.area_no_results);
                    } else {
                        callback.onLocationsLoaded(result.getBounds(),
                                result.getLocations(), result.getSources());
                    }
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

    public interface Callback {
        /**
         * Called before data starts being loaded.
         * Use for e.g. starting a progress bar.
         */
        void onPreLoad();
        /** Called on success loading arcade data. */
        void onLocationsLoaded(@NonNull LatLngBounds newBounds,
                               @NonNull List<ArcadeLocation> newLocations,
                               @NonNull List<DataSource> newSources);
        /** Called on error loading arcade data. */
        void onError(int errorCode, @StringRes int errorMessageResourceId);
        /**
         * Called regardless of success/error and after either one is called.
         * Use for cleanup, like stopping a progress bar.
         */
        void onFinish();
    }
}
