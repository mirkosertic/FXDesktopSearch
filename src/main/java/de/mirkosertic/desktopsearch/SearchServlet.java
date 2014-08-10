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

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SearchServlet extends HttpServlet {

    private Backend backend;

    public SearchServlet(Backend aBackend) {
        backend = aBackend;
    }

    @Override
    protected void doGet(HttpServletRequest aRequest, HttpServletResponse aResponse) throws ServletException, IOException {
        aRequest.setAttribute("querystring","");
        aRequest.getRequestDispatcher("index.ftl").forward(aRequest, aResponse);
    }

    @Override
    protected void doPost(HttpServletRequest aRequest, HttpServletResponse aResponse) throws ServletException, IOException {
        String theSearchString = aRequest.getParameter("querystring");
        if (!StringUtils.isEmpty(theSearchString)) {
            try {
                aRequest.setAttribute("queryResult", backend.performQuery(theSearchString));
            } catch (Exception e) {
                e.printStackTrace();
            }
            aRequest.setAttribute("querystring", StringEscapeUtils.escapeHtml4(theSearchString));
        } else {
            aRequest.setAttribute("querystring","");
        }

        aRequest.getRequestDispatcher("index.ftl").forward(aRequest, aResponse);
    }
}