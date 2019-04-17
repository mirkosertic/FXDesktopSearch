/**
 * FreeDesktopSearch - A Search Engine for your Desktop
 * Copyright (C) 2013 Mirko Sertic
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.mirkosertic.desktopsearch;

import javafx.application.Application;
import javafx.application.Platform;
import org.apache.log4j.Logger;

public class DesktopGateway {

    private static final Logger LOGGER  = Logger.getLogger(DesktopGateway.class);

    private final Application application;
    private final DesktopSearchController controller;

    DesktopGateway(final Application aApplication, final DesktopSearchController aController) {
        application = aApplication;
        controller = aController;
    }

    public void openFile(final String aFile) {
        application.getHostServices().showDocument(aFile);
    }

    public void configure() {
        Platform.runLater(() -> controller.configure());
    }

    public void completecrawl() {
        Platform.runLater(() -> controller.recrawl());
    }

    public void close() {
        Platform.runLater(() -> controller.close());
    }
}
