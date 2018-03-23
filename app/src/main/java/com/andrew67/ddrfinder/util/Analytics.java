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

/**
 * Contains custom analytics constants.
 * See FirebaseAnalytics.Event and FirebaseAnalytics.Param for the vendor-supplied ones.
 */
public class Analytics {

    public static class Event {
        /** The user has changed a preference in the Settings dialog. */
        public static final String SET_PREFERENCE = "set_preference";
        /** The user has performed an action on the map. */
        public static final String MAP_ACTION = "map_action";
        /** The user has triggered an action on a location. */
        public static final String LOCATION_ACTION = "location_action";
        /** The user has requested their own location on the map */
        public static final String LOCATION_REQUESTED = "location_requested";
        /** The user has granted the location permission request and their location was found */
        public static final String LOCATION_FOUND = "location_found";
        /** The user has denied the location permission */
        public static final String LOCATION_PERMISSION_DENIED = "location_permission_denied";
        /** The user has started searching for a location via the Places API. */
        public static final String PLACES_SEARCH_START = "places_search_start";
        /** The user has successfully found and loaded a Places API search result. */
        public static final String PLACES_SEARCH_COMPLETE = "places_search_complete";
        /** The user has dismissed the Places API search screen without selecting a result. */
        public static final String PLACES_SEARCH_CANCELED = "places_search_canceled";
        /** The Places API has thrown an error. */
        public static final String PLACES_SEARCH_ERROR = "places_search_error";
    }

    public static class Param {
        public static final String PREFERENCE_KEY = "preference_key";
        public static final String PREFERENCE_VALUE = "preference_value";
        public static final String ACTION_TYPE = "action_type";
        /**
         * Data source active while performing a map/location action.
         * Tracking this can help segment user actions by data source.
         */
        public static final String ACTIVE_DATASRC = "active_datasrc";
        public static final String EXCEPTION_MESSAGE = "exception_message";
    }

}
