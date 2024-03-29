/*
 * Copyright (c) 2017-2019 Andrés Cordero
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

import android.content.res.Resources;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.browser.customtabs.CustomTabsIntent;

import android.util.TypedValue;

public class ThemeUtil {

    /**
     * Gets a color from the given theme.
     * Source: <a href="http://stackoverflow.com/questions/17277618/get-color-value-programmatically-when-its-a-reference-theme/17277714#17277714">StackOverflow</a>
     * @param theme Theme to extract color from
     * @param attr Reference to the color name, from R.attr
     */
    @ColorInt
    public static int getThemeColor(Resources.Theme theme, @AttrRes int attr) {
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    /**
     * Converts the given theme preference string into an AppCompatDelegate mode.
     */
    public static int getAppCompatDelegateMode(@NonNull String themePreference) {
        int newMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        if (themePreference.equals("dark")) newMode = AppCompatDelegate.MODE_NIGHT_YES;
        else if (themePreference.equals("light")) newMode = AppCompatDelegate.MODE_NIGHT_NO;
        return newMode;
    }

    /**
     * Converts the given AppCompatDelegate mode into a Custom Tabs color scheme.
     */
    public static int getCustomTabsColorScheme(int appCompatDelegateMode) {
        switch (appCompatDelegateMode) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                return CustomTabsIntent.COLOR_SCHEME_LIGHT;
            case AppCompatDelegate.MODE_NIGHT_YES:
                return CustomTabsIntent.COLOR_SCHEME_DARK;
            default:
                return CustomTabsIntent.COLOR_SCHEME_SYSTEM;
        }
    }

    /**
     * Obtains a Custom Tabs color scheme from the currently active AppCompatDelegate mode.
     */
    public static int getCustomTabsColorScheme() {
        return getCustomTabsColorScheme(AppCompatDelegate.getDefaultNightMode());
    }
}
