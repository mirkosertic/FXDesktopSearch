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

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class EntityBlacklist {

    private final Set<String> entries;

    public EntityBlacklist(final InputStream blacklistData) {
        entries = new HashSet<>();
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(blacklistData))) {
            while(br.ready()) {
                final String line = br.readLine();
                if (line != null && line.length() > 0) {
                    entries.add(line);
                }
            }
        } catch (final Exception e) {
            log.warn("Error reading black list", e);
        }
    }

    public synchronized boolean isBlacklisted(final String aEntity) {
        return entries.contains(aEntity);
    }
}
