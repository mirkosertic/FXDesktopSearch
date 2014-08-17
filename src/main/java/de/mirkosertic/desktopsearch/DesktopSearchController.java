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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import netscape.javascript.JSObject;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;

public class DesktopSearchController implements Initializable {

    @FXML
    MenuItem menuItemConfigure;

    @FXML
    MenuItem menuItemRecrawl;

    @FXML
    MenuItem menuItemClose;

    @FXML
    WebView webView;

    @FXML
    VBox statusBar;

    @FXML
    ProgressIndicator progessIndicator;

    @FXML
    TextField statusText;

    @FXML
    MenuItem searchDocumentItem;

    @FXML
    MenuItem showSunburstItem;

    private DesktopSearch application;

    private Backend backend;

    private Window window;

    class ProgressWatcherThread extends Thread {

        private final AtomicLong lastActivity;

        public ProgressWatcherThread() {
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

            Platform.runLater(() -> {
                statusBar.setVisible(true);
                statusBar.setManaged(true);
                menuItemRecrawl.setDisable(true);
            });

            while (!isInterrupted()) {

                if (lastActivity.get() < System.currentTimeMillis() - 5000) {
                    // Longer than five seconds nothing happened
                    interrupt();
                } else {
                    try {
                        sleep(5000);
                    } catch (InterruptedException e) {
                    }
                }
            }

            Platform.runLater(() -> {
                statusBar.setVisible(false);
                statusBar.setManaged(false);
                menuItemRecrawl.setDisable(false);
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

        public void newFileFound(final String aFilename, final long aNumNewFiles, final long aNumIndexedFiles) {
            wakeupThread();
            watcherThread.notifyProgress();
            Platform.runLater(() -> {
                double theProgress = (double) aNumIndexedFiles / aNumNewFiles;
                progessIndicator.setProgress(theProgress);
                statusText.setText(aFilename);
            });
        }

        public void indexingProgress(final long aNumNewFiles, final long aNumIndexedFiles) {
            wakeupThread();
            watcherThread.notifyProgress();
            Platform.runLater(() -> {
                double theProgress = (double) aNumIndexedFiles / aNumNewFiles;
                progessIndicator.setProgress(theProgress);
            });
        }

        public void crawlingFinished() {
            Platform.runLater(() -> {
                statusText.setText("");
            });
        }
    }

    private ProgressWatcherThread watcherThread;

    private String searchURL;

    private String sunburstURL;

    public void configure(DesktopSearch aApplication, Backend aBackend, String aSearchURL, String aSunburstURL, Window aWindow) {
        window = aWindow;
        application = aApplication;
        backend = aBackend;
        searchURL = aSearchURL;
        sunburstURL = aSunburstURL;
        backend.setProgressListener(new FXProgressListener());
        watcherThread = new ProgressWatcherThread();
        webView.getEngine().setJavaScriptEnabled(true);
        webView.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {

            public void changed(ObservableValue<? extends State> ov, State t, State t1) {
                if (t1 == Worker.State.SUCCEEDED) {
                    JSObject window = (JSObject) webView.getEngine().executeScript("window");
                    window.setMember("desktop", new DesktopGateway());
                }
            }
        });
        webView.setContextMenuEnabled(false);
        webView.getEngine().load(aSearchURL);
        webView.getEngine().setJavaScriptEnabled(true);
    }

    public void initialize(URL aUrl, ResourceBundle aResourceBundle) {
        assert menuItemConfigure != null;
        assert menuItemRecrawl != null;
        assert menuItemClose != null;
        assert webView != null;
        assert statusBar != null;
        assert statusText != null;
        assert searchDocumentItem != null;
        assert showSunburstItem != null;

        menuItemConfigure.setOnAction(actionEvent -> {
            configure();
        });
        menuItemRecrawl.setOnAction(actionEvent -> {
            recrawl();
        });
        menuItemClose.setOnAction(actionEvent -> {
            close();
        });

        searchDocumentItem.setOnAction(actionEvent -> {
            webView.getEngine().load(searchURL);
        });
        showSunburstItem.setOnAction(actionEvent -> {
            webView.getEngine().load(sunburstURL);
        });

        statusBar.setManaged(false);
        statusBar.setVisible(false);
    }

    public void close() {
        application.shutdown();
    }

    public void recrawl() {
        statusBar.setVisible(true);
        statusBar.setManaged(true);
        progessIndicator.setProgress(0);
        menuItemRecrawl.setDisable(true);
        statusText.setText("");
        try {
            backend.crawlLocations();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void configure() {
        try {
            Stage stage = new Stage();
            stage.setResizable(false);
            stage.initStyle(StageStyle.UTILITY);

            FXMLLoader theLoader = new FXMLLoader(getClass().getResource("/scenes/configuration.fxml"));
            AnchorPane theConfigurationRoot = theLoader.load();
            stage.setScene(new Scene(theConfigurationRoot));
            stage.setTitle("Configuration");
            stage.initModality(Modality.APPLICATION_MODAL);

            ConfigurationController theConfigController = theLoader.getController();
            theConfigController.initializeWithValues(backend, stage);
            stage.initOwner(window);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}