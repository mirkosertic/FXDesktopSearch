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

import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class SearchPreferences {

    public static final String EXISTS = "exists";
    public static final String INCLUDE_SIMILAR = "includeSimilar";
    public static final String NUMBER_SEARCH_RESULT = "numberSearchResult";
    public static final String INDEX_LOCATION = "indexLocation";
    public static final String LOCATION_NUM = "locationNum";
    public static final String LOC_ID = "locID";
    public static final String LOC_FILE = "locFile";
    private final Preferences preferences;

    public SearchPreferences() {
        preferences = Preferences.userRoot().node(this.getClass().getName());
    }

    public void save(Backend aBackend) throws BackingStoreException {
        preferences.clear();

        preferences.putBoolean(INCLUDE_SIMILAR, aBackend.isIncludeSimilarDocuments());
        preferences.putInt(NUMBER_SEARCH_RESULT, aBackend.getNumberOfSearchResults());
        preferences.put(INDEX_LOCATION, aBackend.getIndexLocation());

        List<FilesystemLocation> theLocations = aBackend.getFileSystemLocations();
        for (int i = 0; i < theLocations.size(); i++) {
            FilesystemLocation theLocation = theLocations.get(i);
            preferences.put(LOC_ID + i, theLocation.getId());
            preferences.put(LOC_FILE + i, theLocation.getDirectory().toString());
        }
        preferences.putInt(LOCATION_NUM, theLocations.size());
        preferences.putBoolean(EXISTS, true);

        preferences.flush();
    }

    public void initialize(Backend aBackend) throws IOException {

        if (!preferences.getBoolean(EXISTS, false)) {
            // The first time the app is started
            File theIndexLocation = new File(SystemUtils.getUserHome(), "FreeSearchIndexDir");
            theIndexLocation.mkdirs();

            aBackend.setIncludeSimilarDocuments(false);
            aBackend.setNumberOfSearchResults(50);
            aBackend.setIndexLocation(theIndexLocation);

        } else {

            aBackend.setIncludeSimilarDocuments(preferences.getBoolean(INCLUDE_SIMILAR, false));
            aBackend.setNumberOfSearchResults(preferences.getInt(NUMBER_SEARCH_RESULT, 50));

            try {
                File theIndexingLocation = new File(preferences.get(INDEX_LOCATION, ""));
                if (!theIndexingLocation.exists()) {
                    theIndexingLocation = new File(SystemUtils.getUserHome(), "FreeSearchIndexDir");
                    theIndexingLocation.mkdirs();
                }
                aBackend.setIndexLocation(theIndexingLocation);
            } catch (Exception e) {
                File theIndexingLocation = new File(SystemUtils.getUserHome(), "FreeSearchIndexDir");
                theIndexingLocation.mkdirs();
                aBackend.setIndexLocation(theIndexingLocation);
            }

            int theLocationNum = preferences.getInt(LOCATION_NUM, 0);
            for (int i = 0; i < theLocationNum; i++) {
                String theId = preferences.get(LOC_ID + i, "");
                String theLocation = preferences.get(LOC_FILE + i, "");

                File theFile = new File(theLocation);
                if (theFile.exists() && theFile.isDirectory()) {
                    aBackend.add(new FilesystemLocation(theId, new File(theLocation)));
                } else {
                    //TODO: Inform users about this
                }
            }
        }
    }
}
