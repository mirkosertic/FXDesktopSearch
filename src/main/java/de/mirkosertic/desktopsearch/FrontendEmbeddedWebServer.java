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

import javafx.stage.Stage;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;

import java.net.BindException;

class FrontendEmbeddedWebServer {

    private static final int PORT_NUMMER = 4711;

    private final Server jetty;

    public FrontendEmbeddedWebServer(
            final Stage aStage, final Backend aBackend, final PreviewProcessor aPreviewProcessor) {
        jetty = new Server(PORT_NUMMER);

        final var theWebApp = new WebAppContext();
        theWebApp.setContextPath("/");
        theWebApp.setBaseResource(Resource.newClassPathResource("/webapp"));
        theWebApp.setDescriptor("WEB-INF/web.xml");
        theWebApp.setClassLoader(getClass().getClassLoader());
        theWebApp.addServlet(new ServletHolder(new SearchServlet(aBackend, "http://127.0.0.1:" + PORT_NUMMER)), SearchServlet.URL + "/*");
        theWebApp.addServlet(new ServletHolder(new BringToFrontServlet(aStage)), BringToFrontServlet.URL);
        theWebApp.addServlet(new ServletHolder(new SuggestionServlet(aBackend)), SuggestionServlet.URL);
        theWebApp.addServlet(new ServletHolder(new ThumbnailServlet(aBackend, aPreviewProcessor)), ThumbnailServlet.URL + "/*");

        jetty.setHandler(theWebApp);
    }

    public void start() throws BindException {
        try {
            jetty.start();
        } catch (final BindException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            jetty.stop();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getSearchUrl() {
        return "http://127.0.0.1:" + PORT_NUMMER + SearchServlet.URL;
    }

    public static String getBringToFrontUrl() {
        return "http://127.0.0.1:" + PORT_NUMMER + BringToFrontServlet.URL;
    }
}