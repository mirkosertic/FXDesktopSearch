/**
 * FreeDesktopSearch - A Search Engine for your Desktop
 * Copyright (C) 2013 Mirko Sertic
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, see
 * <http://www.gnu.org/licenses/>.
 */
package de.mirkosertic.desktopsearch;

import javafx.application.Platform;
import javafx.concurrent.Worker.State;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;
import netscape.javascript.JSObject;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class DesktopSearchController implements Initializable {

    @FXML
    WebView webView;

    @FXML
    VBox statusBar;

    @FXML
    TextField statusText;

    private DesktopSearchMain application;

    private Backend backend;

    private Window window;

    class ProgressWatcherThread extends Thread {

        private final AtomicLong lastActivity;

        public ProgressWatcherThread() {
            super("Progress Watcher Thread");
            lastActivity = new AtomicLong();
            setName("UI Progress watcher thread");
        }

        void notifyProgress() {
            lastActivity.set(System.currentTimeMillis());
        }

        @Override
        public void start() {
            lastActivity.set(System.currentTimeMillis());
            super.start();
        }

        @Override
        public void run() {

            Platform.runLater(() -> statusBar.setVisible(true));

            while (!isInterrupted()) {

                if (lastActivity.get() < System.currentTimeMillis() - 5000) {
                    // Longer than five seconds nothing happened
                    interrupt();
                } else {
                    try {
                        sleep(5000);
                    } catch (final InterruptedException e) {
                    }
                }
            }

            Platform.runLater(() -> {
                statusBar.setVisible(false);
                statusBar.setManaged(false);
            });
        }
    }

    class FXProgressListener implements ProgressListener {

        private void wakeupThread() {
            if (!watcherThread.isAlive()) {
                watcherThread = new ProgressWatcherThread();
                watcherThread.start();
            }
        }

        @Override public void newFileFound(final String aFilename) {
            wakeupThread();
            watcherThread.notifyProgress();
            Platform.runLater(() -> statusText.setText(aFilename));
        }

        @Override public void crawlingFinished() {
            Platform.runLater(() -> statusText.setText(""));
        }
    }

    private ProgressWatcherThread watcherThread;

    private DesktopGateway gateway;

    public void configure(final DesktopSearchMain aApplication, final Backend aBackend, final String aSearchURL, final Window aWindow) {
        gateway = new DesktopGateway(aApplication, DesktopSearchController.this);
        window = aWindow;
        application = aApplication;
        backend = aBackend;
        backend.setProgressListener(new FXProgressListener());
        watcherThread = new ProgressWatcherThread();
        webView.getEngine().setJavaScriptEnabled(true);
        webView.getEngine().getLoadWorker().stateProperty().addListener((ov, t, t1) -> {
            if (t1 == State.SUCCEEDED) {
                final var windowGlobalJSNamespace = (JSObject) webView.getEngine().executeScript("window");
                windowGlobalJSNamespace.setMember("desktop", gateway);
            }
        });
        webView.setContextMenuEnabled(false);
        webView.getEngine().load(aSearchURL);
        webView.getEngine().setJavaScriptEnabled(true);

        if (aApplication.getConfigurationManager().getConfiguration().isCrawlOnStartup()) {
            // Scedule a crawl run 5 seconds after startup...
            final var theTimer = new Timer();
            theTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> recrawl());
                }
            }, 5000);
        }
    }

    @Override public void initialize(final URL aUrl, final ResourceBundle aResourceBundle) {
        Objects.requireNonNull(webView);
        Objects.requireNonNull(statusBar);
        Objects.requireNonNull(statusText);

        statusBar.setManaged(false);
        statusBar.setVisible(false);
    }

    public void close() {
        application.shutdown();
    }

    public void recrawl() {
        statusBar.setVisible(true);
        statusBar.setManaged(true);
        statusText.setText("");
        try {
            backend.crawlLocations();
        } catch (final Exception e) {
            log.error("Error crawling locations", e);
        }
    }

    public void configure() {
        try {
            final var stage = new Stage();
            stage.setResizable(false);
            stage.initStyle(StageStyle.UTILITY);

            final var theLoader = new FXMLLoader(getClass().getResource("/scenes/configuration.fxml"));
            final Parent theConfigurationRoot = theLoader.load();
            stage.setScene(new Scene(theConfigurationRoot));
            stage.setTitle("Configuration");
            stage.initModality(Modality.APPLICATION_MODAL);

            final ConfigurationController theConfigController = theLoader.getController();
            theConfigController.initialize(application.getConfigurationManager(), stage);
            stage.initOwner(window);
            stage.show();
        } catch (final IOException e) {
            log.error("Error running configuration dialog", e);
        }
    }
}