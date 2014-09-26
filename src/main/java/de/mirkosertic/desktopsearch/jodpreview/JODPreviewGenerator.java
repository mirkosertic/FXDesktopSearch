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
package de.mirkosertic.desktopsearch.jodpreview;

import com.artofsolving.jodconverter.DefaultDocumentFormatRegistry;
import com.artofsolving.jodconverter.DocumentFamily;
import com.artofsolving.jodconverter.DocumentFormat;
import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.converter.OpenOfficeDocumentConverter;
import de.mirkosertic.desktopsearch.ImageUtils;
import de.mirkosertic.desktopsearch.Preview;
import de.mirkosertic.desktopsearch.PreviewConstants;
import de.mirkosertic.desktopsearch.PreviewGenerator;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JODPreviewGenerator implements PreviewGenerator, PreviewConstants {

    private static final Logger LOGGER = Logger.getLogger(JODPreviewGenerator.class);

    private boolean enabled;

    private static SocketOpenOfficeConnection createConnection() throws ConnectException {
        SocketOpenOfficeConnection theConnection = new SocketOpenOfficeConnection(8100);
        theConnection.connect();
        return theConnection;
    }

    public JODPreviewGenerator() {
        try {
            SocketOpenOfficeConnection theConnection = createConnection();
            theConnection.disconnect();
            enabled = true;
        } catch (Exception e) {
            LOGGER.error("Error connecting to OpenOffice on port 8100");
            enabled = false;
        }
    }

    @Override
    public boolean supportsFile(File aFile) {
        if (!enabled) {
            return false;
        }
        if (aFile.getName().toLowerCase().endsWith(".doc") || aFile.getName().toLowerCase().endsWith(".docx")) {
            return true;
        }
        return false;
    }

    @Override
    public Preview createPreviewFor(File aFile) {
        File theTempFile = null;
        SocketOpenOfficeConnection theConnection = null;
        try {

            theConnection = createConnection();

            DefaultDocumentFormatRegistry theRegistry = new DefaultDocumentFormatRegistry();

            final DocumentFormat doc = new DocumentFormat("Microsoft Word", DocumentFamily.TEXT, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");
            doc.setExportFilter(DocumentFamily.TEXT, "MS Word 2007");
            theRegistry.addDocumentFormat(doc);

            OpenOfficeDocumentConverter theConverter = new OpenOfficeDocumentConverter(theConnection, theRegistry);
            // Convert to OfficeOpen file
            theTempFile = File.createTempFile("jodtemp", ".odt");
            theConverter.convert(aFile, theTempFile);

            // Now we need to extract the thumbnail from the newly converted file
            ZipFile theZipFile;
            try {
                theZipFile = new ZipFile(theTempFile);
            } catch (IOException e) {
                LOGGER.error("Error opening "+theTempFile, e);
                return null;
            }

            ZipEntry theThumbnailEntry = theZipFile.getEntry("Thumbnails/thumbnail.png");
            if (theThumbnailEntry == null) {
                LOGGER.error("Cannot find thumbnail in "+theTempFile);
                return null;
            }

            BufferedImage theImage = ImageIO.read(new BufferedInputStream(theZipFile.getInputStream(theThumbnailEntry)));
            return new Preview(ImageUtils.rescale(theImage, THUMB_WIDTH, THUMB_HEIGHT, ImageUtils.RescaleMethod.RESIZE_FIT_ONE_DIMENSION));
        } catch (Exception e) {
            LOGGER.error("Error converting file " + aFile, e);
            return null;
        } finally {
            theTempFile.delete();
            if (theConnection != null) {
                theConnection.disconnect();
            }
        }
    }
}
