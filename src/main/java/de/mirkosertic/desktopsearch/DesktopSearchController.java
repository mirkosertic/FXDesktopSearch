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
import javafx.concurrent.Worker.State;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import netscape.javascript.JSObject;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@JavaFXController
public class DesktopSearchController {

    @FXML
    WebView webView;

    @FXML
    VBox statusBar;

    @FXML
    TextField statusText;

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

        @Override
        public void infotext(final String infoTextToDisplay) {
            wakeupThread();
            watcherThread.notifyProgress();
            Platform.runLater(() -> statusText.setText(infoTextToDisplay));
        }

        @Override
        public void crawlingFinished() {
            Platform.runLater(() -> statusText.setText(""));
        }
    }

    private ProgressWatcherThread watcherThread;

    private final DesktopGateway gateway;

    private final ConfigurationManager configurationManager;

    private final Backend backend;

    private final ConfigurableApplicationContext context;

    private final Application application;

    public DesktopSearchController(final ConfigurableApplicationContext applicationContext, final Application application, final ConfigurationManager configurationManager, final Backend backend) {
        this.configurationManager = configurationManager;
        this.context = applicationContext;
        this.gateway = new DesktopGateway(application, this);
        this.application = application;
        this.backend = backend;
        this.backend.setProgressListener(new FXProgressListener());
        this.watcherThread = new ProgressWatcherThread();
    }

    public void initialize(final String aUrl) {
        webView.getEngine().setJavaScriptEnabled(true);
        webView.getEngine().load(aUrl);
        webView.getEngine().getLoadWorker().stateProperty().addListener((ov, t, t1) -> {
            if (t1 == State.SUCCEEDED) {
                final var windowGlobalJSNamespace = (JSObject) webView.getEngine().executeScript("window");
                windowGlobalJSNamespace.setMember("desktop", gateway);
            }
        });
        webView.setContextMenuEnabled(false);
        webView.getEngine().setJavaScriptEnabled(true);

        if (configurationManager.getConfiguration().isCrawlOnStartup()) {
            // Scedule a crawl run 5 seconds after startup...
            final var theTimer = new Timer();
            theTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> recrawl());
                }
            }, 5000);
        }

        statusBar.setManaged(false);
        statusBar.setVisible(false);
    }

    public void close() {
        try {
            application.stop();
        } catch (final Exception e) {
            log.error("Error stopping application", e);
        }
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
            theLoader.setClassLoader(DesktopSearchController.class.getClassLoader());
            theLoader.setControllerFactory(context::getBean);
            final Parent theConfigurationRoot = theLoader.load();
            stage.setScene(new Scene(theConfigurationRoot));
            stage.setTitle("Configuration");
            stage.initModality(Modality.APPLICATION_MODAL);

            final ConfigurationController theConfigController = theLoader.getController();
            theConfigController.initialize(stage);

            stage.initOwner(stage.getOwner());
            stage.show();
        } catch (final IOException e) {
            log.error("Error running configuration dialog", e);
        }
    }
}