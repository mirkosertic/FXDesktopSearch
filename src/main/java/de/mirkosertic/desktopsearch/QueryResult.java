/*
 * FXDesktopSearch Copyright 2013 Mirko Sertic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mirkosertic.desktopsearch;

import java.io.File;
import java.util.List;

public class QueryResult {

    private final String searchTerm;
    private final long elapsedTime;
    private final List<QueryResultDocument> documents;
    private final List<FacetDimension> facetDimensions;
    private final long totalDocuments;
    private final List<QueryFilter> activeFilters;

    public QueryResult(final String searchTerm, final long elapsedTime, final List<QueryResultDocument> documents, final List<FacetDimension> aFacetDimensions, final long totalDocuments, final List<QueryFilter> activeFilters) {
        this.searchTerm = searchTerm;
        this.elapsedTime = elapsedTime;
        this.documents = documents;
        this.totalDocuments = totalDocuments;
        this.facetDimensions = aFacetDimensions;
        this.activeFilters = activeFilters;
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
        final var p = aFileName.lastIndexOf(File.separatorChar);
        if (p>0) {
            return aFileName.substring(p+1);
        }
        return aFileName;
    }

    public List<FacetDimension> getFacetDimensions() {
        return facetDimensions;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public List<QueryFilter> getActiveFilters() {
        return activeFilters;
    }
}
