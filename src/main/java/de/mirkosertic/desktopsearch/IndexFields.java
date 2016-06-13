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

import org.apache.lucene.util.Version;

public interface IndexFields {

    Version LUCENE_VERSION = Version.LATEST;

    String UNIQUEID = "uniqueid";
    String FILENAME = "filename";
    String EXTENSION = "extension";
    String LANGUAGESTORED = "language_s";
    String LANGUAGEFACET = "language_f";
    String CONTENT = "content";
    String CONTENT_NOT_STEMMED = "contentnotstemmed";
    String CONTENTMD5 = "contentmd5";
    String FILESIZE = "filesize";
    String LASTMODIFIED = "lastmodified";
    String LOCATIONID = "locationId";
}