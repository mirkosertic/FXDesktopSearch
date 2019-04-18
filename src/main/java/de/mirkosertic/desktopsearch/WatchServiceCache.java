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

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;

public class WatchServiceCache {

    private final Map<FileSystem, WatchService> watchServices;

    public WatchServiceCache() {
        watchServices = new HashMap<>();
    }

    public synchronized WatchService getWatchServiceFor(final Path aPath) throws IOException {
        final var theFileSystem = aPath.getFileSystem();
        var theService = watchServices.get(theFileSystem);
        if (theService == null) {
            theService = theFileSystem.newWatchService();
            watchServices.put(theFileSystem, theService);
        }

        return theService;
    }
}
