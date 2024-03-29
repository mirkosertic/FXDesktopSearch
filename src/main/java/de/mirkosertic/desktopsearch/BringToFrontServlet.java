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

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javafx.application.Platform;
import javafx.stage.Stage;

public class BringToFrontServlet extends HttpServlet {

    public static final String URL = "/bringToFront";

    private final Stage stage;

    public BringToFrontServlet(final Stage aStage) {
        stage = aStage;
    }

    @Override
    protected void doGet(final HttpServletRequest aRequest, final HttpServletResponse aResponse) throws IOException {
        Platform.runLater(() -> {
            stage.show();
            stage.toFront();
        });
        aResponse.setStatus(HttpServletResponse.SC_OK);
        aResponse.setContentType("text/plain");
        aResponse.getWriter().print("Ok");
    }
}
