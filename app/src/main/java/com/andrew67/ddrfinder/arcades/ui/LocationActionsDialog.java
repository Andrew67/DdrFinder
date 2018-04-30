package com.andrew67.ddrfinder.arcades.ui;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.andrew67.ddrfinder.R;
import com.andrew67.ddrfinder.activity.SettingsActivity;
import com.andrew67.ddrfinder.arcades.util.LocationActions;
import com.andrew67.ddrfinder.arcades.vm.SelectedLocationModel;
import com.andrew67.ddrfinder.mylocation.MyLocationModel;
import com.andrew67.ddrfinder.util.Analytics;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.text.NumberFormat;

public class LocationActionsDialog extends BottomSheetDialogFragment {

    private SelectedLocationModel selectedLocationModel;
    private MyLocationModel myLocationModel;

    private LocationActions locationActions;
    private FirebaseAnalytics firebaseAnalytics;

    private NumberFormat distanceFormat; // 2 decimal digits
    private NumberFormat latLngFormat; // 5 decimal digits (good for 1m precision)

    private TextView arcadeName;
    private TextView arcadeCity;
    private TextView arcadeDistance;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.location_actions, container);

        arcadeName = view.findViewById(R.id.location_name);
        arcadeCity = view.findViewById(R.id.location_city);
        arcadeDistance = view.findViewById(R.id.location_distance);

        final ImageView icNavigate = view.findViewById(R.id.ic_action_navigate);
        icNavigate.setOnClickListener(this::onNavigateClicked);
        final TextView navigate = view.findViewById(R.id.action_navigate);
        navigate.setOnClickListener(this::onNavigateClicked);

        final ImageView icMoreInfo = view.findViewById(R.id.ic_action_moreinfo);
        icMoreInfo.setOnClickListener(this::onMoreInfoClicked);
        final TextView moreInfo = view.findViewById(R.id.action_moreinfo);
        moreInfo.setOnClickListener(this::onMoreInfoClicked);

        final ImageView icCopyGps = view.findViewById(R.id.ic_action_copygps);
        icCopyGps.setOnClickListener(this::onCopyClicked);
        final TextView copyGps = view.findViewById(R.id.action_copygps);
        copyGps.setOnClickListener(this::onCopyClicked);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        selectedLocationModel = ViewModelProviders.of(requireActivity())
                .get(SelectedLocationModel.class);
        selectedLocationModel.getSelectedLocation()
                .observe(this, onSelectedLocationUpdated);

        myLocationModel = ViewModelProviders.of(requireActivity()).get(MyLocationModel.class);

        firebaseAnalytics = FirebaseAnalytics.getInstance(requireActivity());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // https://medium.com/@BladeCoder/architecture-components-pitfalls-part-1-9300dd969808
        selectedLocationModel.getSelectedLocation().removeObserver(onSelectedLocationUpdated);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        firebaseAnalytics.logEvent(Analytics.Event.LOCATION_ACTIONS_DISMISSED, null);
    }

    /**
     * Updates the UI with name/city and requests a new location to update the distance if empty
     */
    private final Observer<SelectedLocationModel.CompositeArcade> onSelectedLocationUpdated =
            selectedLocation -> {
                if (selectedLocation == null) {
                    arcadeName.setText("");
                    arcadeCity.setText("");
                    arcadeDistance.setText("");
                } else {
                    arcadeName.setText(selectedLocation.arcadeLocation.getName());

                    // If city information is not available, show coordinates instead.
                    if (selectedLocation.arcadeLocation.getCity().isEmpty()) {
                        final LatLng coords = selectedLocation.arcadeLocation.getPosition();
                        arcadeCity.setText(getString(R.string.coordinates,
                                formatLatLng(coords.latitude), formatLatLng(coords.longitude)));
                    } else {
                        arcadeCity.setText(selectedLocation.arcadeLocation.getCity());
                    }

                    // If distance is unset, leave blank and request to update user's location.
                    // If user has location disabled, it stays blank silently.
                    // If enabled, a new location is emitted which includes distance.
                    if (selectedLocation.distanceKm == null) {
                        arcadeDistance.setText("");
                        myLocationModel.requestMyLocationSilently(requireActivity(),
                                selectedLocationModel::updateUserLocation);
                    } else {
                        arcadeDistance.setText(getString(R.string.distance_km,
                                formatDistance(selectedLocation.distanceKm)));
                    }

                    // Set up LocationActions object that enables the available actions
                    locationActions = new LocationActions(selectedLocation.arcadeLocation,
                            selectedLocation.dataSource);
                }
            };

    private String formatDistance(float distance) {
        if (distanceFormat == null) {
            distanceFormat = NumberFormat.getNumberInstance();
            distanceFormat.setMaximumFractionDigits(2);
        }
        return distanceFormat.format(distance);
    }

    private String formatLatLng(double coord) {
        if (latLngFormat == null) {
            latLngFormat = NumberFormat.getNumberInstance();
            latLngFormat.setMaximumFractionDigits(5);
        }
        return latLngFormat.format(coord);
    }

    private void onNavigateClicked(@SuppressWarnings("unused") View v) {
        locationActions.navigate(requireActivity());
    }

    private void onMoreInfoClicked(@SuppressWarnings("unused") View v) {
        final boolean useCustomTabs = PreferenceManager
                .getDefaultSharedPreferences(requireActivity())
                .getBoolean(SettingsActivity.KEY_PREF_CUSTOMTABS, true);
        locationActions.moreInfo(requireActivity(), useCustomTabs);
    }

    private void onCopyClicked(@SuppressWarnings("unused") View v) {
        final boolean copySuccess = locationActions.copyGps(requireActivity());
        if (copySuccess)
            Toast.makeText(requireActivity(), R.string.copy_complete, Toast.LENGTH_SHORT).show();
    }
}
