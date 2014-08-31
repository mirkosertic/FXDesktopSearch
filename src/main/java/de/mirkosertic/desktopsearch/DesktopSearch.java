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

import insidefx.undecorator.Undecorator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.lucene.store.LockReleaseFailedException;

import java.awt.*;
import java.net.BindException;
import java.net.URL;

public class DesktopSearch extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private FrontendEmbeddedWebServer embeddedWebServer;
    private Backend backend;
    private Stage stage;
    private SearchPreferences searchPreferences;

    @Override
    public void start(Stage aStage) throws Exception {

        Notifier theNotifier = new Notifier();

        stage = aStage;

        searchPreferences = new SearchPreferences();

        // Backend booten
        backend = new Backend(theNotifier);

        try {
            searchPreferences.initialize(backend);

            // Boot embedded JSP container
            embeddedWebServer = new FrontendEmbeddedWebServer(aStage, backend);

            embeddedWebServer.start();
        } catch (BindException|LockReleaseFailedException e) {
            // In this case, there is already an instance of DesktopSearch running
            // Inform the instance to bring it to front end terminate the current process.
            URL theURL = new URL(FrontendEmbeddedWebServer.getBringToFrontUrl());
            // Retrieve the content, but it can be safely ignored
            // There must only be the get request
            Object theContent = theURL.getContent();

            // Terminate the JVM. The window of the running instance is visible now.
            System.exit(0);
        }

        aStage.setTitle("Free Desktop Search");
        aStage.setWidth(800);
        aStage.setHeight(600);
        aStage.initStyle(StageStyle.TRANSPARENT);

        FXMLLoader theLoader = new FXMLLoader(getClass().getResource("/scenes/mainscreen.fxml"));
        AnchorPane theMainScene = theLoader.load();

        final DesktopSearchController theController = theLoader.getController();
        theController.configure(this, backend, FrontendEmbeddedWebServer.getSearchUrl(), stage.getOwner());

        Undecorator theUndecorator = new Undecorator(stage, theMainScene);
        theUndecorator.getStylesheets().add("/skin/undecorator.css");

        Scene theScene = new Scene(theUndecorator);

        // Hacky, but works...
        theUndecorator.setStyle("-fx-background-color: rgba(0, 0, 0, 0);");

        theScene.setFill(Color.TRANSPARENT);
        aStage.setScene(theScene);

        aStage.getIcons().add(new Image(getClass().getResourceAsStream("/fds.png")));

        if (SystemTray.isSupported()) {
            Platform.setImplicitExit(false);
            SystemTray theTray = SystemTray.getSystemTray();

            PopupMenu theMenu = new PopupMenu();
            MenuItem theCloseItem = new MenuItem("Close");
            theCloseItem.addActionListener(e -> Platform.runLater(this::shutdown));
            theMenu.add(theCloseItem);

            MenuItem theShowItem = new MenuItem("Show");
            theShowItem.addActionListener(e -> Platform.runLater(() -> {
                stage.show();
                stage.toFront();
            }));
            theMenu.add(theShowItem);

            java.awt.Image theSystrayIcon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/fds_small.png"));
            TrayIcon theTrayIcon = new TrayIcon(theSystrayIcon, "Free Desktop Search", theMenu);
            theTray.add(theTrayIcon);

            aStage.setOnCloseRequest(aEvent -> stage.hide());
        } else {

            aStage.setOnCloseRequest(aEvent -> shutdown());
        }

        aStage.show();
    }

    public void shutdown() {
        embeddedWebServer.stop();
        backend.shutdown();
        stage.hide();

        try {
            searchPreferences.save(backend);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Raw shutdown of all threads
        System.exit(0);
    }
}