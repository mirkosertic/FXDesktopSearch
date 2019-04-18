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
package de.mirkosertic.desktopsearch.officeopen;

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
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
public class OfficeOpenPreviewGenerator implements PreviewGenerator, PreviewConstants {

    private final Set<SupportedDocumentType> suppportedDocumentTypes;

    public OfficeOpenPreviewGenerator() {
        suppportedDocumentTypes = new HashSet<>();
        suppportedDocumentTypes.add(SupportedDocumentType.odt);
    }

    @Override
    public boolean supportsFile(final File aFile) {
        for (final var theType : suppportedDocumentTypes) {
            if (theType.matches(aFile)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Preview createPreviewFor(final File aFile) {
        final ZipFile theZipFile;
        try {
            theZipFile = new ZipFile(aFile);
        } catch (final IOException e) {
            log.error("Error opening {}", aFile, e);
            return null;
        }
        try {
            final var theThumbnailEntry = theZipFile.getEntry("Thumbnails/thumbnail.png");
            if (theThumbnailEntry == null) {
                log.error("Cannot find thumbnail in {}", aFile);
                return null;
            }

            final var theImage = ImageIO.read(new BufferedInputStream(theZipFile.getInputStream(theThumbnailEntry)));
            return new Preview(ImageUtils.rescale(theImage, THUMB_WIDTH, THUMB_HEIGHT, ImageUtils.RescaleMethod.RESIZE_FIT_ONE_DIMENSION));
        } catch (final IOException e) {
            log.error("Error reading thumbnail from {}", aFile, e);
            return null;
        } finally {
            try {
                theZipFile.close();
            } catch (final Exception e) {
                // Do nothing
            }
        }
    }
}