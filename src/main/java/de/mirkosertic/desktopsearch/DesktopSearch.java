package de.mirkosertic.desktopsearch;

import javafx.application.Application;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

@Slf4j
public final class DesktopSearch {

    static void main(final String[] args) {

        // Try to show an already running instance
        try {
            // Inform the instance to bring it to front end terminate the current process.
            final var theURL = URI.create("http://localhost:8080/bringToFront").toURL();
            // Retrieve the content, but it can be safely ignored
            // There must only be the get request
            theURL.getContent();

            // Terminate the JVM. The window of the running instance is visible now.
            System.exit(0);
        } catch (final Exception e)  {
            log.info("Failed to bring to front the existing instance. We assume we need to create a new one.");
        }

        Application.launch(DesktopSearchApplication.class, args);
    }
}
