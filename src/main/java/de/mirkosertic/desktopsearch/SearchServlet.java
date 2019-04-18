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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class SearchServlet extends HttpServlet {

    public static final String URL = "/search";

    private final Backend backend;
    private final String basePath;
    private final String serverBase;

    public SearchServlet(final Backend aBackend, final String aServerBase) {
        serverBase = aServerBase;
        backend = aBackend;
        basePath = serverBase + URL;
    }

    @Override
    protected void doGet(final HttpServletRequest aRequest, final HttpServletResponse aResponse) throws ServletException, IOException {
        fillinSearchResult(aRequest, aResponse);
    }

    @Override
    protected void doPost(final HttpServletRequest aRequest, final HttpServletResponse aResponse) throws ServletException, IOException {
        fillinSearchResult(aRequest, aResponse);
    }

    private void fillinSearchResult(final HttpServletRequest aRequest, final HttpServletResponse aResponse)
            throws ServletException, IOException {

        final var theURLCodec = new URLCodec();

        var theQueryString = aRequest.getParameter("querystring");
        var theBasePath = basePath;
        if (!StringUtils.isEmpty(theQueryString)) {
            try {
                theBasePath = theBasePath + "/" + theURLCodec.encode(theQueryString);
            } catch (final EncoderException e) {
                log.error("Error encoding query string {}", theQueryString, e);
            }
        }
        final Map<String, String> theDrilldownDimensions = new HashMap<>();

        final var thePathInfo = aRequest.getPathInfo();
        if (!StringUtils.isEmpty(thePathInfo)) {
            var theWorkingPathInfo = thePathInfo;

            // First component is the query string
            if (theWorkingPathInfo.startsWith("/")) {
                theWorkingPathInfo = theWorkingPathInfo.substring(1);
            }
            final var thePaths = StringUtils.split(theWorkingPathInfo,"/");
            for (var i = 0; i<thePaths.length; i++) {
                try {
                    final var theDecodedValue = thePaths[i].replace('+',' ');
                    final var theEncodedValue = theURLCodec.encode(theDecodedValue);
                    theBasePath = theBasePath + "/" + theEncodedValue;
                    if (i == 0) {
                        theQueryString = theDecodedValue;
                    } else {
                        FacetSearchUtils.addToMap(theDecodedValue, theDrilldownDimensions);
                    }
                } catch (final EncoderException e) {
                    log.error("Error while decoding drilldown params for {}", aRequest.getPathInfo(), e);
                }
            }
        }

        if (!StringUtils.isEmpty(theQueryString)) {
            aRequest.setAttribute("querystring", theQueryString);
            try {
                aRequest.setAttribute("queryResult", backend.performQuery(theQueryString, theBasePath, theDrilldownDimensions));
            } catch (final Exception e) {
                log.error("Error running query {}", theQueryString, e);
            }
        } else {
            aRequest.setAttribute("querystring", "");
        }

        aRequest.setAttribute("serverBase", serverBase);

        aRequest.getRequestDispatcher("/index.ftl").forward(aRequest, aResponse);
    }
}