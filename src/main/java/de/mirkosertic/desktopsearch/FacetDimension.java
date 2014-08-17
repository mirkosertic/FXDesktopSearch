package de.mirkosertic.desktopsearch;

import java.util.List;

public class FacetDimension {

    private final String name;
    private final List<Facet> facets;

    public FacetDimension(String name, List<Facet> facets) {
        this.name = name;
        this.facets = facets;
    }

    public String getName() {
        return name;
    }

    public List<Facet> getFacets() {
        return facets;
    }
}
