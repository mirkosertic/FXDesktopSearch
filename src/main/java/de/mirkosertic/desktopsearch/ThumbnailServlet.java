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

import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ThumbnailServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ThumbnailServlet.class);

    public static final String URL = "/thumbnail";

    private static final String TYPE_ICON = "icon";
    private static final String TYPE_PREVIEW = "preview";

    private final Backend backend;
    private final PreviewProcessor previewProcessor;

    public ThumbnailServlet(final Backend aBackend, final PreviewProcessor aProcessor) {
        backend = aBackend;
        previewProcessor = aProcessor;
    }

    @Override
    protected void doGet(final HttpServletRequest aRequest, final HttpServletResponse aResponse) throws IOException {

        aResponse.setHeader("Cache-Control", "no-cache"); //HTTP 1.1
        aResponse.setHeader("Pragma", "no-cache"); //HTTP 1.0
        aResponse.setDateHeader("Expires", 0);

        String theFilename = aRequest.getPathInfo();

        LOGGER.info("Was requested for thumbnail of " + theFilename);

        // Strip the first path
        theFilename = theFilename.substring(1);

        final int theSlash = theFilename.indexOf("/");
        final String theType = theFilename.substring(0, theSlash);

        theFilename = theFilename.substring(theSlash + 1);

        final int theDot = theFilename.lastIndexOf('.');
        final String theDocumentID = theFilename.substring(0, theDot);
        final String theFileType = theFilename.substring(theDot + 1);

        final File theFileOnDisk = backend.getFileOnDiskForDocument(theDocumentID);

        if (theFileOnDisk != null && theFileOnDisk.exists()) {

            LOGGER.info("Found file on disk " + theFileOnDisk);

            if (TYPE_ICON.equals(theType)) {
                final Icon theFileIcon = FileSystemView.getFileSystemView().getSystemIcon(theFileOnDisk);

                final BufferedImage theImage = new BufferedImage(theFileIcon.getIconWidth(), theFileIcon.getIconHeight(),
                        BufferedImage.TYPE_INT_ARGB);
                theFileIcon.paintIcon(null, theImage.getGraphics(), 0, 0);

                ImageIO.write(theImage, theFileType, aResponse.getOutputStream());
            }

            if (TYPE_PREVIEW.equals(theType)) {
                final Preview thePreview = previewProcessor.computePreviewFor(theFileOnDisk);
                if (thePreview != null) {

                    ImageIO.write(thePreview.getImage(), theFileType, aResponse.getOutputStream());

                } else {
                    LOGGER.info("Nothing was found...");
                    aResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            }
        } else {
            LOGGER.info("Nothing was found...");
            aResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}