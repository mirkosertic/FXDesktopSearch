package de.mirkosertic.desktopsearch;

public class Notifier {
    public void showInformation(String aMessage) {
        /*Platform.runLater(() ->
            Notifications.create().title("FXDesktopSearch").text(aMessage).darkStyle().showInformation()
        );*/
    }

    public void showError(String aMessage, Exception aException) {
        /*Platform.runLater(() ->
            Notifications.create().title("FXDesktopSearch").text(aMessage).darkStyle().showError())
        ;*/
    }
}
