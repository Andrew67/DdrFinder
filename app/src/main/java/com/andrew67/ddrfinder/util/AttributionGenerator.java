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

import com.andrew67.ddrfinder.interfaces.DataSource;

/**
 * Generate attribution text for the map view.
 */
public class AttributionGenerator {
    /**
     * Generate an attribution string from the given sources.
     * Sources with shortName "fallback" are ignored.
     * @param sources Sources to attribute.
     * @return Attribution string.
     */
    public static String fromSources(Iterable<DataSource> sources) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (DataSource source : sources) {
            if (first) first = false;
            else sb.append(" ");
            if (!source.getShortName().equals("fallback")) sb.append("© ").append(source.getName());
        }
        return sb.toString();
    }
}
