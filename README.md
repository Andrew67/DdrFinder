DdrFinder
=========

Native Android version of [ddr-finder](https://github.com/Andrew67/ddr-finder) with Google Maps integration.

See [DdrFinder-legacy](https://github.com/Andrew67/DdrFinder-legacy) for the old Eclipse-based 1.x versions.

Live Demo
---------
Download the APK that uses the database from the ddr-finder [Live Demo](https://ddrfinder.andrew67.com/).

License
-------
MIT license (with some exceptions; see LICENSE).

Developing your own version
---------------------------
* Request a Google Maps V2 API Key from the [Google API Developer Console](https://code.google.com/apis/console/).
* Modify AndroidManifest.xml and add the new key.
* Deploy your own version of [ddr-finder](https://github.com/Andrew67/ddr-finder) or compatible.
* Modify api_endpoint defaultValue property in res/xml/preferences.xml to point to your API endpoint.
* Modify settings_api_version_default in res/values/strings.xml to set your endpoint API version as default.
* Modify about_url in res/values/strings.xml to point to your about page.
* Modify values in getFallback() in the model.v3.Source class to your info redirection script.
* Deploy your own version of [ddr-finder-app](https://github.com/Andrew67/ddr-finder-app) or compatible.
* Modify BASE_URL in the AppLink class to your equivalent deployed web app, or remove the URL share functionality.
* Modify intent filters in AndroidManifest.xml to point to your deployment URL.

Acknowledgments
---------------
* [Zenius -I- vanisher.com](http://zenius-i-vanisher.com/) for inspiring me to make this.
* All acknowledgments listed in the [ddr-finder page](https://github.com/Andrew67/ddr-finder#acknowledgments).
