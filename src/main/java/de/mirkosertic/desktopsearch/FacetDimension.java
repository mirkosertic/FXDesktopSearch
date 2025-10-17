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

import java.util.List;

public class FacetDimension {

    private final String name;
    private final String label;
    private final List<Facet> facets;

    public FacetDimension(final String name, final String label, final List<Facet> facets) {
        this.name = name;
        this.label = label;
        this.facets = facets;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public List<Facet> getFacets() {
        return facets;
    }
}
