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
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.HandlerMapping;

import javax.imageio.ImageIO;
import javax.swing.filechooser.FileSystemView;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Controller
public class ThumbnailController {

    private static final String TYPE_ICON = "icon";
    private static final String TYPE_PREVIEW = "preview";

    private final Backend backend;
    private final PreviewProcessor previewProcessor;

    public ThumbnailController(final Backend backend, final PreviewProcessor aProcessor) {
        this.backend = backend;
        this.previewProcessor = aProcessor;
    }

    @GetMapping("/thumbnail/**")
    protected void doGet(final HttpServletRequest aRequest, final HttpServletResponse aResponse) throws IOException {

        aResponse.setHeader("Cache-Control", "no-cache"); //HTTP 1.1
        aResponse.setHeader("Pragma", "no-cache"); //HTTP 1.0
        aResponse.setDateHeader("Expires", 0);

        final String path = (String) aRequest.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        final String bestMatchPattern = (String ) aRequest.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        final AntPathMatcher apm = new AntPathMatcher();
        var theFilename = URLDecoder.decode(apm.extractPathWithinPattern(bestMatchPattern, path), StandardCharsets.UTF_8);

        log.info("Was requested for thumbnail of {}", theFilename);

        var theType = "";
        if (theFilename.startsWith("preview/")) {
            theType = "preview";
            theFilename = theFilename.substring("preview/".length() - 1);
        } else if (theFilename.startsWith("icon/")) {
            theType = "icon";
            theFilename = theFilename.substring("icon/".length() - 1);
        }

        final var theDot = theFilename.lastIndexOf('.');
        final var theDocumentID = theFilename.substring(0, theDot);
        final var theFileType = theFilename.substring(theDot + 1);

        final var theFileOnDisk = backend.getFileOnDiskForDocument(theDocumentID);

        if (theFileOnDisk != null && theFileOnDisk.exists()) {

            log.info("Found file on disk {}", theFileOnDisk);

            if (TYPE_ICON.equals(theType)) {
                final var theFileIcon = FileSystemView.getFileSystemView().getSystemIcon(theFileOnDisk);

                final var theImage = new BufferedImage(theFileIcon.getIconWidth(), theFileIcon.getIconHeight(),
                        BufferedImage.TYPE_INT_ARGB);
                theFileIcon.paintIcon(null, theImage.getGraphics(), 0, 0);

                ImageIO.write(theImage, theFileType, aResponse.getOutputStream());
            }

            if (TYPE_PREVIEW.equals(theType)) {
                final var thePreview = previewProcessor.computePreviewFor(theFileOnDisk);
                if (thePreview != null) {

                    ImageIO.write(thePreview.getImage(), theFileType, aResponse.getOutputStream());

                } else {
                    log.info("Nothing was found...");
                    aResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            }
        } else {
            log.info("Nothing was found...");
            aResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
