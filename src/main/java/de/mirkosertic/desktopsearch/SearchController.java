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

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Controller
public class SearchController {

    private final Backend backend;

    public SearchController(final Backend backend) {
        this.backend = backend;
    }

    @GetMapping("/search")
    protected ModelAndView doGet(final HttpServletResponse respomse, @RequestParam(required = false) final String querystring, @RequestParam final MultiValueMap<String, String> params) {
        return fillinSearchResult(respomse, querystring, params);
    }

    @PostMapping("/search")
    protected ModelAndView doPost(final HttpServletResponse response, @RequestParam(required = false) final String querystring, @RequestParam final MultiValueMap<String, String> params) {
        return fillinSearchResult(response, querystring, params);
    }

    private ModelAndView fillinSearchResult(final HttpServletResponse response, final String querystring, final MultiValueMap<String, String> params) {

        response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noCache().mustRevalidate().getHeaderValue());
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setDateHeader(HttpHeaders.EXPIRES, 0);

        final ModelAndView modelAndView = new ModelAndView("index.html");

        if (!StringUtils.isEmpty(querystring)) {
            modelAndView.addObject("querystring", querystring);
            try {
                modelAndView.addObject("queryResult", backend.performQuery(querystring, params));
            } catch (final Exception e) {
                log.error("Error running query {}", querystring, e);
            }
        } else {
            modelAndView.addObject("querystring", "");
        }

        modelAndView.addObject("serverBase", "/");

        return modelAndView;
    }
}