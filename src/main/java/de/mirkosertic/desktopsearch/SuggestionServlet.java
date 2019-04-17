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

import org.codehaus.jackson.map.ObjectMapper;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

class SuggestionServlet extends HttpServlet {

    public static final String URL = "/suggestion";

    private final Backend backend;

    public SuggestionServlet(final Backend aBackend) {
        backend = aBackend;
    }

    @Override
    protected void service(final HttpServletRequest aRequest, final HttpServletResponse aResponse) throws IOException {
        final var theTerm = aRequest.getParameter("term");
        final var theTerms = backend.findSuggestionTermsFor(theTerm);

        aResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        aResponse.setHeader("Pragma", "no-cache");
        aResponse.setDateHeader("Expires", 0);
        aResponse.setContentType("application/json; charset=UTF-8");
        aResponse.setCharacterEncoding("UTF-8");

        final var theMapper = new ObjectMapper();
        theMapper.writeValue(aResponse.getWriter(), theTerms);
    }
}
