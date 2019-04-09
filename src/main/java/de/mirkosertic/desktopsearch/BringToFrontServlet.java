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

import javafx.application.Platform;
import javafx.stage.Stage;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
