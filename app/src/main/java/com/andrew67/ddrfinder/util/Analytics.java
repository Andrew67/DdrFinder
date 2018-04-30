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

        /** The user has forced a reload on the map */
        public static final String MAP_ACTION_RELOAD = "map_action_reload";
        /** The user has selected a map marker */
        public static final String MAP_MARKER_SELECTED = "map_marker_selected";

        /** The user has dismissed the location actions dialog */
        public static final String LOCATION_ACTIONS_DISMISSED = "location_actions_dismissed";

        /** The user has copied a location's GPS coordinates */
        public static final String LOCATION_ACTION_COPYGPS = "location_action_copygps";
        /** The user has clicked the navigation button for a location */
        public static final String LOCATION_ACTION_NAVIGATE = "location_action_navigate";
        /** The user has clicked the more info button for a location */
        public static final String LOCATION_ACTION_MOREINFO = "location_action_moreinfo";
        /** An exception was thrown while trying to load a browser for the more info action */
        public static final String LOCATION_MOREINFO_EXCEPTION = "location_moreinfo_exception";

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
        public static final String EXCEPTION_MESSAGE = "exception_message";
    }

    public static class UserProperty {
        /**
         * Data source active while performing a map/location action.
         * Tracking this can help segment user actions by data source.
         */
        public static final String ACTIVE_DATASRC = "active_datasrc";
    }

}
