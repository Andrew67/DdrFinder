package com.andrew67.ddrfinder.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

public class LocaleUtil {
    /**
     * Given a list of locales, return the tag that matches one of our available preferences.
     * This is because the user may have settings such as es-US (we have "es")
     * or zh-CN (we have "zh-Hans").
     */
    @Nullable
    public static String getLanguagePref(@NonNull LocaleListCompat appLocales,
                                         @NonNull String[] supportedLocales) {
        if (appLocales.size() == 0) return null;
        Locale appLocale = appLocales.getFirstMatch(supportedLocales);
        if (appLocale == null) return null;

        for (String supportedLocaleTag: supportedLocales) {
            Locale supportedLocale = Locale.forLanguageTag(supportedLocaleTag);
            if (LocaleListCompat.matchesLanguageAndScript(supportedLocale, appLocale)) {
                return supportedLocaleTag;
            }
        }
        return null;
    }
}
