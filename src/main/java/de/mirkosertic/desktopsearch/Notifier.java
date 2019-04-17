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

import eu.hansolo.enzo.notification.Notification;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import org.apache.log4j.Logger;

class Notifier {

    private static final Logger LOGGER = Logger.getLogger(Notifier.class);

    private final Notification.Notifier notifier;

    Notifier() {
        final var theScreenBounds = Screen.getPrimary().getVisualBounds();
        Notification.Notifier.setWidth(theScreenBounds.getWidth() / 4);
        notifier = Notification.Notifier.INSTANCE;
    }

    public void showInformation(final String aMessage) {
        LOGGER.info(aMessage);
        /*Platform.runLater(() ->
            notifier.notifyInfo("FXDesktopSearch", aMessage)
        );*/
    }

    public void showError(final String aMessage, final Exception aException) {
        LOGGER.error(aMessage, aException);
        Platform.runLater(() ->
            notifier.notifyError("FXDesktopSearch", aMessage)
        );
    }
}