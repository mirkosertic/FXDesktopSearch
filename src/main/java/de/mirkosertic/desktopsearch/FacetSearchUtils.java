/**
 * FreeDesktopSearch - A Search Engine for your Desktop
 * Copyright (C) 2013 Mirko Sertic
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.mirkosertic.desktopsearch;

import java.util.Map;

final class FacetSearchUtils {

    private FacetSearchUtils() {
    }

    public static String encode(final String aDimension, final String aValue) {
        return aDimension+"="+aValue;
    }

    public static void addToMap(final String aDimensionCriteria, final Map<String, String> aDrilldownDimensions) {
        final int p = aDimensionCriteria.indexOf("=");
        aDrilldownDimensions.put(aDimensionCriteria.substring(0, p), aDimensionCriteria.substring(p + 1));
    }
}
