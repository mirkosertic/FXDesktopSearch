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

import javafx.application.Application;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.URL;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class DesktopSearch {

    public static void main(final String[] args) {

        // Try to show an already running instance
        try {
            // Inform the instance to bring it to front end terminate the current process.
            final var theURL = new URL("http://localhost:8080/bringToFront");
            // Retrieve the content, but it can be safely ignored
            // There must only be the get request
            theURL.getContent();

            // Terminate the JVM. The window of the running instance is visible now.
            System.exit(0);
        } catch (final Exception e)  {
            log.info("Failed to bring to front the existing instance. We assume we need to create a new one.");
        }

        Application.launch(DesktopSearchMain.class, args);
    }
}