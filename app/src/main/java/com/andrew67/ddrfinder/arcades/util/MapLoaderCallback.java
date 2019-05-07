package com.andrew67.ddrfinder.arcades.util;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.andrew67.ddrfinder.arcades.model.ApiResult;

public interface MapLoaderCallback {
    /**
     * Called before data starts being loaded.
     * Use for e.g. starting a progress bar.
     */
    void onPreLoad();
    /** Called on success loading arcade data. */
    void onLocationsLoaded(@NonNull ApiResult result);
    /** Called on error loading arcade data. */
    void onError(int errorCode, @StringRes int errorMessageResourceId);
    /**
     * Called regardless of success/error and after either one is called.
     * Use for cleanup, like stopping a progress bar.
     */
    void onFinish();
}
