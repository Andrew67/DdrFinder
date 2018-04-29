package com.andrew67.ddrfinder.arcades.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.andrew67.ddrfinder.R;

public class LocationActionsDialog extends BottomSheetDialogFragment {

    private static final String TAG = LocationActionsDialog.class.getSimpleName();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.location_actions_layout, container);

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

    private void onNavigateClicked(@SuppressWarnings("unused") View v) {
        Log.d(TAG, "Navigate clicked!");
    }

    private void onMoreInfoClicked(@SuppressWarnings("unused") View v) {
        Log.d(TAG, "More information clicked!");
    }

    private void onCopyClicked(@SuppressWarnings("unused") View v) {
        Log.d(TAG, "Copy GPS coordinates clicked!");
    }
}
