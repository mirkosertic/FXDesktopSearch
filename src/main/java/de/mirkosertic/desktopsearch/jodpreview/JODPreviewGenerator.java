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
import lombok.extern.slf4j.Slf4j;

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

@Slf4j
public class JODPreviewGenerator implements PreviewGenerator, PreviewConstants {

    private boolean enabled;
    private final Set<SupportedDocumentType> suppportedDocumentTypes;
    private final DefaultDocumentFormatRegistry documentFormatRegistry;

    private static SocketOpenOfficeConnection createConnection() throws ConnectException {
        final var theConnection = new SocketOpenOfficeConnection(8100);
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
        final var theDOCXFormat = new DocumentFormat("Microsoft Word", DocumentFamily.TEXT, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");
        theDOCXFormat.setExportFilter(DocumentFamily.TEXT, "MS Word 2007");
        documentFormatRegistry.addDocumentFormat(theDOCXFormat);
        // PPTX Stuff is missing in the default registry
        final var thePPTXFormat = new DocumentFormat("Microsoft Powerpoint", DocumentFamily.PRESENTATION, "application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx");
        theDOCXFormat.setExportFilter(DocumentFamily.TEXT, "MS Word 2007");
        documentFormatRegistry.addDocumentFormat(thePPTXFormat);

        try {
            final var theConnection = createConnection();
            theConnection.disconnect();
            enabled = true;
        } catch (final Exception e) {
            log.error("Error connecting to OpenOffice on port 8100");
            enabled = false;
        }
    }

    @Override
    public boolean supportsFile(final File aFile) {
        if (!enabled) {
            return false;
        }
        for (final var theType : suppportedDocumentTypes) {
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

            final var theConverter = new OpenOfficeDocumentConverter(theConnection, documentFormatRegistry);
            // Convert to OfficeOpen file
            theTempFile = File.createTempFile("jodtemp", ".odt");
            theConverter.convert(aFile, theTempFile);

            // Now we need to extract the thumbnail from the newly converted file
            final ZipFile theZipFile;
            try {
                theZipFile = new ZipFile(theTempFile);
            } catch (final IOException e) {
                log.error("Error opening {}", theTempFile, e);
                return null;
            }

            final var theThumbnailEntry = theZipFile.getEntry("Thumbnails/thumbnail.png");
            if (theThumbnailEntry == null) {
                log.error("Cannot find thumbnail in {}", theTempFile);
                return null;
            }

            final var theImage = ImageIO.read(new BufferedInputStream(theZipFile.getInputStream(theThumbnailEntry)));
            return new Preview(ImageUtils.rescale(theImage, THUMB_WIDTH, THUMB_HEIGHT, ImageUtils.RescaleMethod.RESIZE_FIT_ONE_DIMENSION));
        } catch (final Exception e) {
            log.error("Error converting file {}", aFile, e);
            return null;
        } finally {
            theTempFile.delete();
            if (theConnection != null) {
                theConnection.disconnect();
            }
        }
    }
}