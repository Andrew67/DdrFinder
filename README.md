DdrFinder
=========

Native Android version of [ddr-finder](https://github.com/Andrew67/ddr-finder) with Google Maps integration.

Live Demo
---------
APKs available in the [Releases](https://github.com/Andrew67/DdrFinder/releases) tab or on [Google Play](https://play.google.com/store/apps/details?id=com.andrew67.ddrfinder).

License
-------
MIT license (with some exceptions; see LICENSE).

Developing your own version
---------------------------
* Modify `applicationId` in `build.gradle` and package in `AndroidManifest.xml`.
* Request a Google Maps V2 API Key from the [Google API Developer Console](https://code.google.com/apis/console/) using the new package names.
* Modify `GOOGLE_MAPS_API_KEY` in `app/src/main/res/values/strings.xml` with your new key.
* Deploy your own version of [ddr-finder](https://github.com/Andrew67/ddr-finder) or compatible.
* Modify `API_BASE_URL` in `build.gradle` to point to your API endpoint.
* Modify `ABOUT_BASE_URL` in `build.gradle` to point to your about page.
* Modify `FALLBACK_INFO_URL` in `build.gradle` to your info redirection script.
* Deploy your own version of [ddr-finder-ng](https://github.com/Andrew67/ddr-finder-ng) or compatible.
* Modify `APPLINK_BASE_URL` in `build.gradle` to your equivalent deployed web app, or remove the URL share functionality.
* Modify intent filters in `AndroidManifest.xml` to point to your deployment URL.
* Set up a project in the [Firebase Console](https://console.firebase.google.com/) using the new package names, or remove Analytics functionality.
* Download the new `google-services.json` and replace the existing copy.

Acknowledgments
---------------
* [Zenius -I- vanisher.com](http://zenius-i-vanisher.com/) for inspiring me to make this.
* All acknowledgments listed in the [ddr-finder page](https://github.com/Andrew67/ddr-finder#acknowledgments).
