package org.chromium.customtabsdemos;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.collection.ArraySet;

import java.util.List;
import java.util.Set;

/**
 * Helps launch URIs with native apps instead of the browser.
 * This implementation avoids having to declare package intent queries in the manifest.
 *
 * <p>Built from <a href="https://developer.chrome.com/docs/android/custom-tabs/howto-custom-tab-native-apps/">this guide</a>.
 */
public class NativeLaunchHelper {
    @RequiresApi(api = Build.VERSION_CODES.R)
    static boolean launchNativeApi30(Context context, Uri uri) {
        Intent nativeAppIntent = new Intent(Intent.ACTION_VIEW, uri)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER);
        try {
            context.startActivity(nativeAppIntent);
            return true;
        } catch (ActivityNotFoundException ex) {
            return false;
        }
    }

    private static Set<String> extractPackageNames(List<ResolveInfo> resolveInfoList) {
        Set<String> packageNames = new ArraySet<>(resolveInfoList.size());
        for (ResolveInfo resolveInfo: resolveInfoList) {
            packageNames.add(resolveInfo.activityInfo.packageName);
        }
        return packageNames;
    }

    private static boolean launchNativeBeforeApi30(Context context, Uri uri) {
        PackageManager pm = context.getPackageManager();

        // Get all Apps that resolve a generic url
        Intent browserActivityIntent = new Intent()
                .setAction(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.parse("https://example.com"));
        Set<String> genericResolvedList = extractPackageNames(
                pm.queryIntentActivities(browserActivityIntent, 0));

        // Get all apps that resolve the specific Url
        Intent specializedActivityIntent = new Intent(Intent.ACTION_VIEW, uri)
                .addCategory(Intent.CATEGORY_BROWSABLE);
        Set<String> resolvedSpecializedList = extractPackageNames(
                pm.queryIntentActivities(specializedActivityIntent, 0));

        // Keep only the Urls that resolve the specific, but not the generic
        // urls.
        resolvedSpecializedList.removeAll(genericResolvedList);

        // If the list is empty, no native app handlers were found.
        if (resolvedSpecializedList.isEmpty()) {
            return false;
        }

        // We found native handlers. Launch the Intent.
        specializedActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(specializedActivityIntent);
        return true;
    }

    /**
     * Attempts to launch a URI using a non-browser native app.
     * @return Whether the app was launched. If not, there was no app that matched and you should launch using a web browser.
     */
    public static boolean launchUri(Context context, Uri uri) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ?
                launchNativeApi30(context, uri) :
                launchNativeBeforeApi30(context, uri);
    }
}
