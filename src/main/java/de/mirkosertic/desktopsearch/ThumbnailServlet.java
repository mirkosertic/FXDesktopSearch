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
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ThumbnailServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ThumbnailServlet.class);

    public static final String URL = "/thumbnail";

    private final Backend backend;

    public ThumbnailServlet(Backend aBackend) {
        backend = aBackend;
    }

    @Override
    protected void doGet(HttpServletRequest aRequest, HttpServletResponse aResponse) throws ServletException, IOException {
        String theFilename = aRequest.getPathInfo();

        LOGGER.info("Was requested for thumbnail of " + theFilename);

        int theDot = theFilename.lastIndexOf('.');
        int theDocumentID = Integer.parseInt(theFilename.substring(1, theDot));
        String theFileType = theFilename.substring(theDot + 1);

        File theFileOnDisk = backend.getFileOnDiskForDocument(theDocumentID);
        if (theFileOnDisk != null && theFileOnDisk.exists()) {
            LOGGER.info("Found file on disk " + theFileOnDisk);

            Icon theFileIcon = FileSystemView.getFileSystemView().getSystemIcon(theFileOnDisk);

            BufferedImage theImage = new BufferedImage(theFileIcon.getIconWidth(), theFileIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            theFileIcon.paintIcon(null, theImage.getGraphics(), 0, 0);

            ImageIO.write(theImage, theFileType, aResponse.getOutputStream());
        } else {
            LOGGER.info("Nothing was found...");
            aResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}