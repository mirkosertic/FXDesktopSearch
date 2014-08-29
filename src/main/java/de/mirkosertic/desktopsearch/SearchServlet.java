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

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.beans.Encoder;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SearchServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(SearchServlet.class);

    public static final String URL = "/search";

    private final Backend backend;
    private final String basePath;
    private final String serverBase;

    public SearchServlet(Backend aBackend, String aServerBase) {
        serverBase = aServerBase;
        backend = aBackend;
        basePath = serverBase + URL;
    }

    @Override
    protected void doGet(HttpServletRequest aRequest, HttpServletResponse aResponse) throws ServletException, IOException {
        fillinSearchResult(aRequest, aResponse);
    }

    @Override
    protected void doPost(HttpServletRequest aRequest, HttpServletResponse aResponse) throws ServletException, IOException {
        fillinSearchResult(aRequest, aResponse);
    }

    private void fillinSearchResult(HttpServletRequest aRequest, HttpServletResponse aResponse)
            throws ServletException, IOException {

        URLCodec theURLCodec = new URLCodec();

        String theQueryString = aRequest.getParameter("querystring");
        String theBasePath = basePath;
        String theBackLink = basePath;
        if (!StringUtils.isEmpty(theQueryString)) {
            try {
                theBasePath = theBasePath + "/" + theURLCodec.encode(theQueryString);
                theBackLink = theBackLink + "/" + theURLCodec.encode(theQueryString);
            } catch (EncoderException e) {
                e.printStackTrace();
            }
        }
        Map<String, String> theDrilldownDimensions = new HashMap<>();

        String thePathInfo = aRequest.getPathInfo();
        if (!StringUtils.isEmpty(thePathInfo)) {
            String theWorkingPathInfo = thePathInfo;

            // First component is the query string
            if (theWorkingPathInfo.startsWith("/")) {
                theWorkingPathInfo = theWorkingPathInfo.substring(1);
            }
            String[] thePaths = StringUtils.split(theWorkingPathInfo,"/");
            for (int i=0;i<thePaths.length;i++) {
                try {
                    String theDecodedValue = theURLCodec.decode(thePaths[i]);
                    String theEncodedValue = theURLCodec.encode(theDecodedValue);
                    theBasePath = theBasePath + "/" + theEncodedValue;
                    if (i<thePaths.length - 1) {
                        theBackLink = theBackLink + "/" + theEncodedValue;
                    }
                    if (i == 0) {
                        theQueryString = theDecodedValue;
                    } else {
                        FacetSearchUtils.addToMap(theDecodedValue, theDrilldownDimensions);
                    }
                } catch (EncoderException|DecoderException e) {
                    LOGGER.error("Error while decoding drilldown params for " + aRequest.getPathInfo(), e);
                }
            }
            if (basePath.equals(theBackLink)) {
                theBackLink = null;
            }
        } else {
            theBackLink = null;
        }

        if (!StringUtils.isEmpty(theQueryString)) {
            aRequest.setAttribute("querystring", theQueryString);
            try {
                aRequest.setAttribute("queryResult", backend.performQuery(theQueryString, theBackLink, theBasePath, theDrilldownDimensions));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            aRequest.setAttribute("querystring", "");
        }

        aRequest.setAttribute("serverBase", serverBase);

        aRequest.getRequestDispatcher("/index.ftl").forward(aRequest, aResponse);
    }
}