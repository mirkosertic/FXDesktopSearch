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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DesktopGateway {

    private final Application application;
    private final DesktopSearchController controller;

    DesktopGateway(final Application aApplication, final DesktopSearchController aController) {
        application = aApplication;
        controller = aController;
    }

    public void openFile(final String aFile) {
        application.getHostServices().showDocument(aFile);
    }

    public void configure() {
        Platform.runLater(() -> controller.configure());
    }

    public void completecrawl() {
        Platform.runLater(() -> controller.recrawl());
    }

    public void close() {
        Platform.runLater(() -> controller.close());
    }
}
