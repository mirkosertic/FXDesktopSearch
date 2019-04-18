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

import org.apache.solr.common.params.SolrParams;

import java.util.Iterator;
import java.util.Map;

public class SearchMapParams extends SolrParams {

    private final Map<String, Object> params;

    public SearchMapParams(final Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public String get(final String name) {
        final var o = params.get(name);
        if (o == null) {
            return null;
        }
        if (o instanceof String) {
            return (String)o;
        }
        if (o instanceof String[]) {
            final var strings = (String[])o;
            return strings.length == 0 ? null : strings[0];
        }
        return String.valueOf(o);
    }

    @Override
    public String[] getParams(final String name) {
        final var val = params.get(name);
        if (val instanceof String[]) {
            return (String[])val;
        }
        return val == null ? null : new String[]{String.valueOf(val)};
    }

    @Override
    public Iterator<String> getParameterNamesIterator() {
        return params.keySet().iterator();
    }
}
