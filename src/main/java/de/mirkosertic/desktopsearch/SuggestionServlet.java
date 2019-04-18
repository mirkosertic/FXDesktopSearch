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

import com.fasterxml.jackson.databind.ObjectMapper;

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
