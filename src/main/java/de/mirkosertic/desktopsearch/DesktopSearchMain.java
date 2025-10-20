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

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;

import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

@Slf4j
@SpringBootApplication
public class DesktopSearchMain {

    @Autowired
    private Backend backend;

    @Autowired
    private Stage stage;

    @Autowired
    private ConfigurableApplicationContext context;

    @EventListener
    public void fullyInitialize(final ApplicationStartedEvent startedEvent) {
        Platform.runLater(() -> {
            try {
                stage.setTitle("FXDesktopSearch");
                stage.setWidth(800);
                stage.setHeight(600);
                stage.setMinWidth(640);
                stage.setMinHeight(480);
                stage.initStyle(StageStyle.TRANSPARENT);

                final var theLoader = new FXMLLoader(getClass().getResource("/scenes/mainscreen.fxml"));
                theLoader.setControllerFactory(context::getBean);
                final AnchorPane theMainScene = theLoader.load();

                final DesktopSearchController theController = theLoader.getController();
                theController.initialize("http://localhost:8080/search");

                final var theScene = new Scene(theMainScene);

                stage.initStyle(StageStyle.DECORATED);
                stage.setScene(theScene);
                stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/fds.png"))));

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

                    stage.setOnCloseRequest(x -> stage.hide());
                } else {

                    stage.setOnCloseRequest(x -> shutdown());
                }

                SplashScreen.hideMe();

                stage.show();

                Platform.runLater(() -> {
                    final Scene scene = stage.getScene();
                    scene.getRoot().resize(scene.getWidth(), scene.getHeight());
                    scene.getRoot().requestLayout();
                });

            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void shutdown() {

        backend.shutdown();
        stage.hide();

        // Raw shutdown of all threads
        System.exit(0);
    }

    public void bringToFront() {
        stage.show();
        stage.toFront();
    }

    public Suggestion[] findSuggestionTermsFor(final String term) {
        return backend.findSuggestionTermsFor(term);
    }
}