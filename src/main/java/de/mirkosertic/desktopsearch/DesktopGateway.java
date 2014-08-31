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

import javafx.application.Platform;
import org.apache.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class DesktopGateway {

    private static final Logger LOGGER  = Logger.getLogger(DesktopGateway.class);

    private void open(String aFile) {
        try {
            LOGGER.info("Opening file " + aFile);
            Desktop.getDesktop().open(new File(aFile));
            LOGGER.info("Finished");
        } catch (IOException e) {
            LOGGER.error("Error opening file " + aFile, e);
        }
    }

    public void openFile(String aFile) {
        if (Desktop.isDesktopSupported()) {
            if (Platform.isFxApplicationThread()) {
                LOGGER.info("In FXApplicationThread");
                new Thread(() -> open(aFile)).start();
            } else {
                LOGGER.info("Not in FXApplicationThread");
                open(aFile);
            }
        } else {
            LOGGER.error("No Desktop is supported on this machine");
        }
    }
}
