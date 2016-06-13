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
import org.apache.commons.lang3.SystemUtils;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.LockReleaseFailedException;

import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.BindException;
import java.net.URL;

public class DesktopSearch extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private FrontendEmbeddedWebServer embeddedWebServer;
    private Backend backend;
    private Stage stage;
    private ConfigurationManager configurationManager;

    @Override
    public void start(Stage aStage) throws Exception {

        // This is our base directory
        File theBaseDirectory = new File(SystemUtils.getUserHome(), "FreeSearchIndexDir");
        theBaseDirectory.mkdirs();

        configurationManager = new ConfigurationManager(theBaseDirectory);

        Notifier theNotifier = new Notifier();

        stage = aStage;

        // Create the known preview processors
        PreviewProcessor thePreviewProcessor = new PreviewProcessor();

        try {
            // Boot the search backend and set it up for listening to configuration changes
            backend = new Backend(theNotifier, configurationManager.getConfiguration(), thePreviewProcessor);
            configurationManager.addChangeListener(backend);

            // Boot embedded JSP container
            embeddedWebServer = new FrontendEmbeddedWebServer(aStage, backend, thePreviewProcessor, configurationManager);

            embeddedWebServer.start();
        } catch (BindException|LockReleaseFailedException|LockObtainFailedException e) {
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
        aStage.setMinWidth(640);
        aStage.setMinHeight(480);
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

            // We need to reformat the icon according to the current tray icon dimensions
            // this depends on the underlying OS
            java.awt.Image theTrayIconImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/fds_small.png"));
            int trayIconWidth = new TrayIcon(theTrayIconImage).getSize().width;
            TrayIcon theTrayIcon = new TrayIcon(theTrayIconImage.getScaledInstance(trayIconWidth, -1, java.awt.Image.SCALE_SMOOTH), "Free Desktop Search");
            theTrayIcon.setImageAutoSize(true);
            theTrayIcon.setToolTip("FXDesktopSearch");
            theTrayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 1) {
                        Platform.runLater(() -> {
                            if (stage.isIconified()) {
                                stage.setIconified(false);
                            }
                            stage.show();
                            stage.toFront();
                        });
                    }
                }
            });
            theTray.add(theTrayIcon);

            aStage.setOnCloseRequest(aEvent -> stage.hide());
        } else {

            aStage.setOnCloseRequest(aEvent -> shutdown());
        }

        aStage.setMaximized(true);
        aStage.show();
    }

    public void shutdown() {

        embeddedWebServer.stop();
        backend.shutdown();
        stage.hide();

        // Raw shutdown of all threads
        System.exit(0);
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    public Stage getStage() {
        return stage;
    }
}