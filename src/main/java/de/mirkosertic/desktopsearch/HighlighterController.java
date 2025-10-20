package de.mirkosertic.desktopsearch;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@Slf4j
public class HighlighterController {

    private final Backend backend;

    public HighlighterController(final Backend backend) {
        this.backend = backend;
    }

    @GetMapping("/highlight")
    public ModelAndView highlight(final HttpServletResponse response, @RequestParam final String query, @RequestParam final int docId) {

        response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noCache().mustRevalidate().getHeaderValue());
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setDateHeader(HttpHeaders.EXPIRES, 0);

        final ModelAndView result = new ModelAndView("highlight.html");
        result.addObject("query", query);
        result.addObject("docId", docId);
        result.addObject("highlight", backend.highlightMatch(query, docId));
        return result;
    }
}
