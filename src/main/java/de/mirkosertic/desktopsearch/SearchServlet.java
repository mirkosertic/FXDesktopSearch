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

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Controller
public class SearchServlet {

    private final DesktopSearchMain desktopSearchMain;

    public SearchServlet(final DesktopSearchMain main) {
        desktopSearchMain = main;
    }

    @GetMapping("/search/**")
    protected ModelAndView doGet(final HttpServletRequest request) {
        return fillinSearchResult(request);
    }

    @PostMapping("/search/**")
    protected ModelAndView doPost(final HttpServletRequest request) {
        return fillinSearchResult(request);
    }

    private ModelAndView fillinSearchResult(final HttpServletRequest request) {

        final var theURLCodec = new URLCodec();

        var theQueryString = request.getParameter("querystring");
        final StringBuilder theBasePath = new StringBuilder("/");
        if (!StringUtils.isEmpty(theQueryString)) {
            try {
                theBasePath.append("/").append(theURLCodec.encode(theQueryString));
            } catch (final EncoderException e) {
                log.error("Error encoding query string {}", theQueryString, e);
            }
        }
        final Map<String, Set<String>> theDrilldownDimensions = new HashMap<>();

        final var thePathInfo = request.getPathInfo();
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
                    theBasePath.append("/").append(theEncodedValue);
                    if (i == 0) {
                        theQueryString = theDecodedValue;
                    } else {
                        FacetSearchUtils.addToMap(theDecodedValue, theDrilldownDimensions);
                    }
                } catch (final EncoderException e) {
                    log.error("Error while decoding drilldown params for {}", request.getPathInfo(), e);
                }
            }
        }

        final ModelAndView modelAndView = new ModelAndView("index.html");

        if (!StringUtils.isEmpty(theQueryString)) {
            modelAndView.addObject("querystring", theQueryString);
            try {
                modelAndView.addObject("queryResult", desktopSearchMain.performQuery(theQueryString, theBasePath.toString(), theDrilldownDimensions));
            } catch (final Exception e) {
                log.error("Error running query {}", theQueryString, e);
            }
        } else {
            modelAndView.addObject("querystring", "");
        }

        modelAndView.addObject("serverBase", "/");

        return modelAndView;
    }
}