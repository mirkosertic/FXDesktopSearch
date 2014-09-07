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
package de.mirkosertic.desktopsearch.predict;

import java.util.HashSet;
import java.util.Set;

class TermAssociation {

    private long usages;
    private final Set<Long> runs;

    public TermAssociation(long aUsageCount, long aRun) {
        usages = aUsageCount;
        runs = new HashSet<>();
        runs.add(aRun);
    }

    public void incrementUsageByOne(long aRun) {
        usages++;
        runs.add(aRun);
    }

    public long usages() {
        return usages;
    }

    Set<Long> getRuns() {
        return runs;
    }
}
