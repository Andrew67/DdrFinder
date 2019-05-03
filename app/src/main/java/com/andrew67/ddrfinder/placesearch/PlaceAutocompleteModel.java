/*
 * Copyright (c) 2018 Andrés Cordero
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

package com.andrew67.ddrfinder.placesearch;

import android.app.Activity;
import android.app.Dialog;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.SingleLiveEvent;
import android.arch.lifecycle.ViewModel;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.libraries.places.compat.AutocompleteFilter;
import com.google.android.libraries.places.compat.Place;
import com.google.android.libraries.places.compat.ui.PlaceAutocomplete;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

/**
 * With a little setup, provides a "Places Autocomplete" action which returns the location in a LiveData.
 * If Google Play Services is missing (or another error is encountered), displays the error dialog
 */
public class PlaceAutocompleteModel extends ViewModel {

    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 59573;
    /** LiveData that holds the most recent successful location response */
    private final SingleLiveEvent<PlaceAutocompleteResponse> autocompleteResponse;
    /** LiveData that holds an interrupted location request (dismissed/error) */
    private final SingleLiveEvent<PlaceAutocompleteError> autocompleteError;

    public PlaceAutocompleteModel() {
        super();
        autocompleteResponse = new SingleLiveEvent<>();
        autocompleteError = new SingleLiveEvent<>();
    }

    /**
     * Get the autocomplete response LiveData object.
     * Useful for setting up observers on startup, without triggering the search activity
     */
    @NonNull
    public LiveData<PlaceAutocompleteResponse> getAutocompleteResponse() {
        return autocompleteResponse;
    }

    /**
     * Get the autocomplete error LiveData object.
     * Use for attaching behaviors upon search dismissed or error.
     * Fires only once per call to {@link #startPlaceAutocomplete}
     */
    @NonNull
    public LiveData<PlaceAutocompleteError> getAutocompleteError() {
        return autocompleteError;
    }

    /**
     * Starts the place autocomplete overlay activity (with filter for regions).
     * If Google Play Services requires an update, shows actionable error message to user
     */
    public void startPlaceAutocomplete(@NonNull Activity activity) {
        try {
            final AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                    .setTypeFilter(AutocompleteFilter.TYPE_FILTER_GEOCODE)
                    .build();
            final Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                    .setFilter(typeFilter)
                    .build(activity);
            activity.startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesNotAvailableException e) {
            // This exception is not actionable
            e.printStackTrace();
            autocompleteError.setValue(PlaceAutocompleteError.withError(e.getMessage()));
        } catch (GooglePlayServicesRepairableException e) {
            // This exception is actionable; display Play Services update dialog to user
            final Dialog errorDialog = GoogleApiAvailability.getInstance()
                    .getErrorDialog(activity, e.getConnectionStatusCode(), PLACE_AUTOCOMPLETE_REQUEST_CODE);
            if (errorDialog != null) errorDialog.show();
        }
    }

    /**
     * This method must be called from the activity's own onActivityResult,
     * so that it can handle the result of the places autocomplete request
     */
    public void onActivityResult(@NonNull Context activity,
                                 int requestCode, int resultCode,
                                 Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                final Place place = PlaceAutocomplete.getPlace(activity, data);
                autocompleteResponse.setValue(PlaceAutocompleteResponse.withPlace(place));
            } else if (resultCode == RESULT_CANCELED) {
                autocompleteError.setValue(PlaceAutocompleteError.CANCELED);
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                final String statusMessage = PlaceAutocomplete.getStatus(activity, data).getStatusMessage();
                Log.e("PlaceAutocompleteModel", statusMessage);
                autocompleteError.setValue(PlaceAutocompleteError.withError(statusMessage));
            }
        }
    }

    /**
     * Represents a successful response to a "place autocomplete" request event
     */
    public static final class PlaceAutocompleteResponse {
        /**
         * Place result, when result code is RESULT_OK
         */
        public final @NonNull Place place;

        /**
         * Timestamp of when this response object was generated
         */
        public final long timestamp;

        private PlaceAutocompleteResponse(@NonNull Place place) {
            this.place = place;
            this.timestamp = System.currentTimeMillis();
        }

        /** Represents a place obtained response */
        static PlaceAutocompleteResponse withPlace(@NonNull Place place) {
            return new PlaceAutocompleteResponse(place);
        }
    }

    /**
     * Represents a successful response to a "place autocomplete" request event
     */
    public static final class PlaceAutocompleteError {
        /**
         * Result code from the {@link PlaceAutocomplete} call to {@link #onActivityResult}
         */
        public final int resultCode;

        /**
         * Error message, when result code is RESULT_ERROR,
         * or a {@link GooglePlayServicesNotAvailableException} was thrown
         */
        public final @Nullable String errorMessage;

        private PlaceAutocompleteError(int resultCode, @Nullable String errorMessage) {
            this.resultCode = resultCode;
            this.errorMessage = errorMessage;
        }

        /** Represents a "user canceled" response */
        static final PlaceAutocompleteError CANCELED = new PlaceAutocompleteError(
                RESULT_CANCELED, null);

        /** Represents an error response */
        static PlaceAutocompleteError withError(@Nullable String errorMessage) {
            return new PlaceAutocompleteError(PlaceAutocomplete.RESULT_ERROR, errorMessage);
        }
    }
}
