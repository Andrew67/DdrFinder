/*
 * Copyright (c) 2017 Andrés Cordero
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
import android.graphics.Color;
import android.util.TypedValue;

public class ThemeUtil {

    /**
     * Gets a color from the given theme.
     * Source: http://stackoverflow.com/questions/17277618/get-color-value-programmatically-when-its-a-reference-theme/17277714#17277714
     * @param theme Theme to extract color from
     * @param attr Reference to the color name, from R.attr
     */
    public static int getThemeColor(Resources.Theme theme, int attr) {
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    /**
     * Gets the hue value for a color from the given theme.
     * Generally used for Google Maps MarkerOptions which require a hue value.
     * @param theme Theme to extract color from
     * @param attr Reference to the color name, from R.attr
     */
    public static float getThemeColorHue(Resources.Theme theme, int attr) {
        int rgbColor = getThemeColor(theme, attr);
        float[] hsv = new float[3];
        Color.colorToHSV(rgbColor, hsv);
        return hsv[0];
    }
}
