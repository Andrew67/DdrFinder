/*
 * Copyright (c) 2018-2019 Andrés Cordero
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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.SingleLiveEvent;
import androidx.lifecycle.ViewModel;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.andrew67.ddrfinder.R;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.util.Arrays;
import java.util.List;

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
        if (!Places.isInitialized()) {
            Places.initialize(activity.getApplicationContext(),
                    activity.getString(R.string.GOOGLE_MAPS_API_KEY));
        }
        final List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME,
                Place.Field.LAT_LNG, Place.Field.VIEWPORT);
        final Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                .setTypeFilter(TypeFilter.GEOCODE)
                .build(activity);
        activity.startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
    }

    /**
     * This method must be called from the activity's own onActivityResult,
     * so that it can handle the result of the places autocomplete request
     * @return Whether the activity result was handled
     */
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                final Place place = Autocomplete.getPlaceFromIntent(data);
                autocompleteResponse.setValue(PlaceAutocompleteResponse.withPlace(place));
            } else if (resultCode == RESULT_CANCELED) {
                autocompleteError.setValue(PlaceAutocompleteError.CANCELED);
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                final String statusMessage = Autocomplete.getStatusFromIntent(data).getStatusMessage();
                Log.e("PlaceAutocompleteModel", "Error status message: " + statusMessage);
                autocompleteError.setValue(PlaceAutocompleteError.withError(statusMessage));
            }
            return true;
        }
        return false;
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
         * Result code from the {@link Autocomplete} call to {@link #onActivityResult}
         */
        public final int resultCode;

        /**
         * Error message, when result code is RESULT_ERROR
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
            return new PlaceAutocompleteError(AutocompleteActivity.RESULT_ERROR, errorMessage);
        }
    }
}
