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
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;

@Slf4j
public class DesktopSearchMain extends Application {

    public static void main(final String[] args) {
        launch(args);
    }

    private FrontendEmbeddedWebServer embeddedWebServer;
    private Backend backend;
    private Stage stage;
    private ConfigurationManager configurationManager;

    @Override
    public void start(final Stage aStage) throws Exception {

        // This is our base directory
        final var theBaseDirectory = new File(SystemUtils.getUserHome(), "FXDesktopSearch");
        theBaseDirectory.mkdirs();

        configurationManager = new ConfigurationManager(theBaseDirectory);

        final var theNotifier = new Notifier();

        stage = aStage;

        // Try to bring to front an existing instance
        try {
            // Inform the instance to bring it to front end terminate the current process.
            final var theURL = new URL(FrontendEmbeddedWebServer.getBringToFrontUrl());
            // Retrieve the content, but it can be safely ignored
            // There must only be the get request
            final var theContent = theURL.getContent();

            // Terminate the JVM. The window of the running instance is visible now.
            System.exit(0);
        } catch (Exception e)  {
            log.info("Failed to brint to front en existing instance. We assume we need to create a new one.");
        }

        // Create the known preview processors
        final var thePreviewProcessor = new PreviewProcessor();

        // Boot the search backend and set it up for listening to configuration changes
        backend = new Backend(theNotifier, configurationManager.getConfiguration(), thePreviewProcessor);
        configurationManager.addChangeListener(backend);

        // Boot embedded JSP container
        embeddedWebServer = new FrontendEmbeddedWebServer(aStage, backend, thePreviewProcessor);

        embeddedWebServer.start();

        aStage.setTitle("FXDesktopSearch");
        aStage.setWidth(800);
        aStage.setHeight(600);
        aStage.setMinWidth(640);
        aStage.setMinHeight(480);
        aStage.initStyle(StageStyle.TRANSPARENT);

        final var theLoader = new FXMLLoader(getClass().getResource("/scenes/mainscreen.fxml"));
        final AnchorPane theMainScene = theLoader.load();

        final DesktopSearchController theController = theLoader.getController();
        theController.configure(this, backend, FrontendEmbeddedWebServer.getSearchUrl(), stage.getOwner());

        final var theScene = new Scene(theMainScene);

        aStage.initStyle(StageStyle.DECORATED);
        aStage.setScene(theScene);
        aStage.getIcons().add(new Image(getClass().getResourceAsStream("/fds.png")));

        if (SystemTray.isSupported()) {
            Platform.setImplicitExit(false);
            final var theTray = SystemTray.getSystemTray();

            // We need to reformat the icon according to the current tray icon dimensions
            // this depends on the underlying OS
            final var theTrayIconImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/fds_small.png"));
            final var trayIconWidth = new TrayIcon(theTrayIconImage).getSize().width;
            final var theTrayIcon = new TrayIcon(theTrayIconImage.getScaledInstance(trayIconWidth, -1, java.awt.Image.SCALE_SMOOTH), "Free Desktop Search");
            theTrayIcon.setImageAutoSize(true);
            theTrayIcon.setToolTip("FXDesktopSearch");
            theTrayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
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
}