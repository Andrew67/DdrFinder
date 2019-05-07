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

package com.andrew67.ddrfinder.mylocation;

import android.app.Activity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.SingleLiveEvent;
import androidx.lifecycle.ViewModel;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;

/**
 * With a little setup, provides a "My Location" action which returns the location in a LiveData.
 * If the user has not accepted the permission, the platform permission request will be attempted.
 * If that request is denied, our own dialog is presented (App settings walkthrough)
 */
public class MyLocationModel extends ViewModel {

    private static final int PERMISSIONS_REQUEST_LOCATION = 57359;
    /** LiveData that holds the most recent user location */
    private final SingleLiveEvent<MyLocationResponse> locationResponse;
    /** LiveData that holds the permission granted status */
    private final SingleLiveEvent<Boolean> permissionGranted;

    /**
     * Holds whether permission was recently requested and denied.
     * This signals whether onResume should pop up our secondary request dialog, as instantiating
     * the dialog directly from {@link #onRequestPermissionsResult} is not possible
     */
    private boolean permissionDeniedFromPlatform;

    public MyLocationModel() {
        super();
        locationResponse = new SingleLiveEvent<>();
        permissionGranted = new SingleLiveEvent<>();
        permissionDeniedFromPlatform = false;
    }

    /**
     * Get the location response LiveData object.
     * Useful for setting up observers on startup, without triggering the location permission prompt
     */
    @NonNull
    public LiveData<MyLocationResponse> getLocationResponse() {
        return locationResponse;
    }

    /**
     * Get the permission granted LiveData object.
     * Use for attaching observers for setup when granted, or error message when denied.
     * Fires denied only when user requests location explicitly;
     * fires granted upon original grant and on every onResume thereafter.
     * When emitting true, SecurityException should not happen for ACCESS_FINE_LOCATION
     */
    @NonNull
    public LiveData<Boolean> getPermissionGranted() {
        return permissionGranted;
    }

    /**
     * Attempts to obtain the user's current location.
     * This will result in a new response emitted in locationResponse, either with the location or
     * a permission denied event.
     * If the permission is denied, attempts to guide the user to enabling it, before giving up
     */
    public void requestMyLocation(@NonNull Activity activity) {
        if (ContextCompat.checkSelfPermission(activity,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            loadLocation(activity, updateLocationResponse);
        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_LOCATION);
        }
    }

    /**
     * Attempts to obtain the user's current location silently once.
     * This is useful for startup operations that can be skipped if the permission has not been
     * granted.
     * Unlike {@link #requestMyLocation}, does not send an event to the location response LiveData
     */
    public void requestMyLocationSilently(@NonNull Context activity,
                                          @NonNull OnSuccessListener<LatLng> onSuccessListener) {
        if (ContextCompat.checkSelfPermission(activity,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            loadLocation(activity, onSuccessListener);
        }
    }

    /**
     * This method must be called from the activity's own onRequestPermissionsResult,
     * so that it can handle the location permission request grant/denial
     */
    public void onRequestPermissionsResult(@NonNull Context activity, int requestCode,
                                           @SuppressWarnings("unused") @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionGranted.setValue(true);
                loadLocation(activity, updateLocationResponse);
            } else {
                permissionDeniedFromPlatform = true;
            }
        }
    }

    /**
     * This method must be called from the activity's own onResume,
     * so that it can handle the location permission being granted via app settings
     */
    public void onResume(@NonNull FragmentActivity activity) {
        if (ContextCompat.checkSelfPermission(activity,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            permissionGranted.setValue(true);
        } else if (permissionDeniedFromPlatform) {
            permissionDeniedFromPlatform = false;

            // Keeping this instance around is tempting, but in most cases the user will not be
            // performing the action frequently enough to justify the extra memory cost.
            new EnableLocationDialogFragment()
                    .setCancelListener(() -> permissionGranted.setValue(false))
                    .show(activity.getSupportFragmentManager(), "dialog");
        }
    }

    /**
     * Return the user's location into the success callback
     * @throws SecurityException When called without first checking if the permission has been granted
     */
    private void loadLocation(Context activity, final OnSuccessListener<LatLng> onSuccessListener)
            throws SecurityException {
        final FusedLocationProviderClient fusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(activity);
        fusedLocationProviderClient.getLastLocation()
                // Convert Location to LatLng when successful
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        onSuccessListener.onSuccess(new LatLng(
                                location.getLatitude(),
                                location.getLongitude()
                        ));
                    }
                });
    }

    /**
     * Listener for {@link #loadLocation} which updates the locationResponse LiveData
     */
    private final OnSuccessListener<LatLng> updateLocationResponse = new OnSuccessListener<LatLng>() {
        @Override
        public void onSuccess(LatLng latLng) {
            locationResponse.postValue(MyLocationResponse.withLocation(latLng));
        }
    };

    /**
     * Represents a response to a "my location" request event
     */
    public static final class MyLocationResponse {
        /**
         * The user's most recently requested and obtained position.
         * Will be null if the response is not from an explicit request (onResume, etc)
         */
        public final @NonNull LatLng latLng;

        /**
         * Timestamp of when this response object was generated
         */
        public final long timestamp;

        private MyLocationResponse(@NonNull LatLng latLng) {
            this.latLng = latLng;
            this.timestamp = System.currentTimeMillis();
        }

        /** Represents a location obtained response */
        static MyLocationResponse withLocation(@NonNull LatLng latLng) {
            return new MyLocationResponse(latLng);
        }
    }
}
