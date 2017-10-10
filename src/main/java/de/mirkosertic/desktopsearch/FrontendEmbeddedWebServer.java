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

import javafx.stage.Stage;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;

import java.net.BindException;

class FrontendEmbeddedWebServer {

    private static final int PORT_NUMMER = 4711;

    private final Server jetty;

    public FrontendEmbeddedWebServer(Stage aStage, Backend aBackend, PreviewProcessor aPreviewProcessor, ConfigurationManager aConfigurationManager) {
        jetty = new Server(PORT_NUMMER);

        WebAppContext theWebApp = new WebAppContext();
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
        } catch (BindException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            jetty.stop();
        } catch (Exception e) {
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