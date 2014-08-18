package de.mirkosertic.desktopsearch;

import java.util.Map;

public final class FacetSearchUtils {

    private FacetSearchUtils() {
    }

    public static String encode(String aDimension, String aValue) {
        return aDimension+"="+aValue;
    }

    public static void addToMap(String aDimensionCriteria, Map<String, String> aDrilldownDimensions) {
        int p = aDimensionCriteria.indexOf("=");
        aDrilldownDimensions.put(aDimensionCriteria.substring(0, p), aDimensionCriteria.substring(p + 1));
    }
}
