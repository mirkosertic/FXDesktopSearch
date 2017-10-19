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
        Rectangle2D theScreenBounds = Screen.getPrimary().getVisualBounds();
        Notification.Notifier.setWidth(theScreenBounds.getWidth() / 4);
        notifier = Notification.Notifier.INSTANCE;
    }

    public void showInformation(String aMessage) {
        LOGGER.info(aMessage);
        /*Platform.runLater(() ->
            notifier.notifyInfo("FXDesktopSearch", aMessage)
        );*/
    }

    public void showError(String aMessage, Exception aException) {
        LOGGER.error(aMessage, aException);
        Platform.runLater(() ->
            notifier.notifyError("FXDesktopSearch", aMessage)
        );
    }
}