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
import de.mirkosertic.desktopsearch.SupportedDocumentType;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JODPreviewGenerator implements PreviewGenerator, PreviewConstants {

    private static final Logger LOGGER = Logger.getLogger(JODPreviewGenerator.class);

    private boolean enabled;
    private final Set<SupportedDocumentType> suppportedDocumentTypes;
    private final DefaultDocumentFormatRegistry documentFormatRegistry;

    private static SocketOpenOfficeConnection createConnection() throws ConnectException {
        final SocketOpenOfficeConnection theConnection = new SocketOpenOfficeConnection(8100);
        theConnection.connect();
        return theConnection;
    }

    public JODPreviewGenerator() {
        suppportedDocumentTypes = new HashSet<>();
        suppportedDocumentTypes.add(SupportedDocumentType.txt);
        suppportedDocumentTypes.add(SupportedDocumentType.doc);
        suppportedDocumentTypes.add(SupportedDocumentType.docx);
        //Presentations disabled due to memory issues
        //suppportedDocumentTypes.add(SupportedDocumentType.ppt);
        //suppportedDocumentTypes.add(SupportedDocumentType.pptx);
        suppportedDocumentTypes.add(SupportedDocumentType.rtf);

        documentFormatRegistry = new DefaultDocumentFormatRegistry();
        // DOCX Stuff is missing in the default registry
        final DocumentFormat theDOCXFormat = new DocumentFormat("Microsoft Word", DocumentFamily.TEXT, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");
        theDOCXFormat.setExportFilter(DocumentFamily.TEXT, "MS Word 2007");
        documentFormatRegistry.addDocumentFormat(theDOCXFormat);
        // PPTX Stuff is missing in the default registry
        final DocumentFormat thePPTXFormat = new DocumentFormat("Microsoft Powerpoint", DocumentFamily.PRESENTATION, "application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx");
        theDOCXFormat.setExportFilter(DocumentFamily.TEXT, "MS Word 2007");
        documentFormatRegistry.addDocumentFormat(thePPTXFormat);

        try {
            final SocketOpenOfficeConnection theConnection = createConnection();
            theConnection.disconnect();
            enabled = true;
        } catch (final Exception e) {
            LOGGER.error("Error connecting to OpenOffice on port 8100");
            enabled = false;
        }
    }

    @Override
    public boolean supportsFile(final File aFile) {
        if (!enabled) {
            return false;
        }
        for (final SupportedDocumentType theType : suppportedDocumentTypes) {
            if (theType.matches(aFile)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Preview createPreviewFor(final File aFile) {
        File theTempFile = null;
        SocketOpenOfficeConnection theConnection = null;
        try {

            theConnection = createConnection();

            final OpenOfficeDocumentConverter theConverter = new OpenOfficeDocumentConverter(theConnection, documentFormatRegistry);
            // Convert to OfficeOpen file
            theTempFile = File.createTempFile("jodtemp", ".odt");
            theConverter.convert(aFile, theTempFile);

            // Now we need to extract the thumbnail from the newly converted file
            final ZipFile theZipFile;
            try {
                theZipFile = new ZipFile(theTempFile);
            } catch (final IOException e) {
                LOGGER.error("Error opening "+theTempFile, e);
                return null;
            }

            final ZipEntry theThumbnailEntry = theZipFile.getEntry("Thumbnails/thumbnail.png");
            if (theThumbnailEntry == null) {
                LOGGER.error("Cannot find thumbnail in "+theTempFile);
                return null;
            }

            final BufferedImage theImage = ImageIO.read(new BufferedInputStream(theZipFile.getInputStream(theThumbnailEntry)));
            return new Preview(ImageUtils.rescale(theImage, THUMB_WIDTH, THUMB_HEIGHT, ImageUtils.RescaleMethod.RESIZE_FIT_ONE_DIMENSION));
        } catch (final Exception e) {
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