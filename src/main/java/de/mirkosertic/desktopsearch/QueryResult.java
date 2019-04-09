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

import java.io.File;
import java.util.List;

public class QueryResult {

    private final long elapsedTime;
    private final List<QueryResultDocument> documents;
    private final List<FacetDimension> facetDimensions;
    private final long totalDocuments;
    private final String backLink;

    public QueryResult(final long elapsedTime, final List<QueryResultDocument> documents, final List<FacetDimension> aFacetDimensions, final long totalDocuments, final String aBackLink) {
        this.elapsedTime = elapsedTime;
        this.documents = documents;
        this.totalDocuments = totalDocuments;
        facetDimensions = aFacetDimensions;
        backLink = aBackLink;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public List<QueryResultDocument> getDocuments() {
        return documents;
    }

    public long getTotalDocuments() {
        return totalDocuments;
    }

    public String getEscapedFileName(final String aFileName) {
        if (aFileName == null) {
            return null;
        }
        return aFileName.replace("\\","\\\\");
    }

    public String getSimpleFileName(final String aFileName) {
        if (aFileName == null) {
            return null;
        }
        final int p = aFileName.lastIndexOf(File.separatorChar);
        if (p>0) {
            return aFileName.substring(p+1);
        }
        return aFileName;
    }

    public List<FacetDimension> getFacetDimensions() {
        return facetDimensions;
    }

    public String getBackLink() {
        return backLink;
    }
}
