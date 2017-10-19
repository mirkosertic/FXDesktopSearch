/**
 * FreeDesktopSearch - A Search Engine for your Desktop
 * Copyright (C) 2017 Mirko Sertic
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, see
 * <http://www.gnu.org/licenses/>.
 */
package de.mirkosertic.desktopsearch;

import org.apache.solr.common.params.SolrParams;

import java.util.Iterator;
import java.util.Map;

public class SearchMapParams extends SolrParams {

    private final Map<String, Object> params;

    public SearchMapParams(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public String get(String name) {
        Object o = params.get(name);
        if (o == null) {
            return null;
        }
        if (o instanceof String) {
            return (String)o;
        }
        if (o instanceof String[]) {
            String[] strings = (String[])((String[])o);
            return strings.length == 0 ? null : strings[0];
        }
        return String.valueOf(o);
    }

    @Override
    public String[] getParams(String name) {
        Object val = params.get(name);
        if (val instanceof String[]) {
            return (String[])((String[])val);
        }
        return val == null ? null : new String[]{String.valueOf(val)};
    }

    @Override
    public Iterator<String> getParameterNamesIterator() {
        return params.keySet().iterator();
    }
}
