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

package com.andrew67.ddrfinder.util;

import android.net.Uri;
import android.support.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Represents an app link (e.g. for positioning the map on app launch which can be shared).
 */
public class AppLink {

    private static String BASE_URL = "https://ddrfinder.andrew67.com/app";

    private final LatLng position;
    private final Float zoom;

    /**
     * Build the AppLink. Cannot be called directly.
     */
    private AppLink(@Nullable LatLng position, @Nullable Float zoom) {
        this.position = position;
        this.zoom = zoom;
    }

    /**
     * Return the positions for the center of the map, if set.
     * @return Center position, or null if not specified.
     */
    @Nullable
    public LatLng getPosition() {
        return position;
    }

    /**
     * Return the zoom level for the map, if set.
     * @return Zoom level, or null if not specified.
     */
    @Nullable
    public Float getZoom() {
        return zoom;
    }

    /**
     * Return the data source to use, if set.
     * @return Data source short name, or null if not specified.
     */
    @Nullable
    public String getSourceShortName() {
        return null;
    }

    /**
     * Builds an instance of AppLink by parsing the given Uri.
     * @param url Uri to parse. Can come straight from getIntent().getData().
     * @return Instance of AppLink.
     */
    public static AppLink parse(@Nullable Uri url) {
        Builder appLink = new Builder();
        if (url != null) {
            try {
                // For now we only handle /ng links, so no need to verify path segments.
                String ll = url.getQueryParameter("ll");
                if (ll != null) {
                    String[] llComponents = ll.split(",");
                    Double latitude = Double.valueOf(llComponents[0]);
                    Double longitude = Double.valueOf(llComponents[1]);
                    appLink.position(new LatLng(latitude, longitude));
                }
                String z = url.getQueryParameter("z");
                if (z != null) {
                    appLink.zoom(Float.valueOf(z));
                }
            } catch (NumberFormatException e) {
                // This exception is to be expected given user input.
            }
        }
        return appLink.build();
    }

    /**
     * Converts this AppLink into a URL string for sharing.
     * @return Absolute URL to the app with the state represented by this AppLink.
     */
    @Override
    public String toString() {
        StringBuilder url = new StringBuilder(BASE_URL);
        url.append("/map"); // Append activity; currently only map is available.
        // Append coordinates as "/@{lat},{long},{zoom}z".
        if (position != null && zoom != null) {
            NumberFormat sixDigitsFormatter = NumberFormat.getInstance(Locale.US);
            sixDigitsFormatter.setMinimumFractionDigits(0);
            sixDigitsFormatter.setMaximumFractionDigits(6);

            url.append("/@");
            url.append(sixDigitsFormatter.format(position.latitude));
            url.append(',');
            url.append(sixDigitsFormatter.format(position.longitude));
            url.append(',');
            url.append(zoom.intValue());
            url.append('z');
        }
        return url.toString();
    }

    /**
     * Mutable helper class for building AppLink instances.
     */
    public static class Builder {
        private LatLng position = null;
        private Float zoom = null;

        public Builder position(@Nullable LatLng position) {
            this.position = position;
            return this;
        }

        public Builder zoom(@Nullable Float zoom) {
            this.zoom = zoom;
            return this;
        }

        public AppLink build() {
            return new AppLink(position, zoom);
        }
    }

    /**
     * Constructs a new builder, copying this AppLink instance.
     * @return New builder with current parameters.
     */
    public Builder buildUpon() {
        return new Builder().position(position).zoom(zoom);
    }

}
