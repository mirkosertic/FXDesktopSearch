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
package de.mirkosertic.desktopsearch.officeopen;

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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class OfficeOpenPreviewGenerator implements PreviewGenerator, PreviewConstants {

    private static final Logger LOGGER = Logger.getLogger(OfficeOpenPreviewGenerator.class);

    @Override
    public boolean supportsFile(File aFile) {
        if (aFile.getName().toLowerCase().endsWith(".odt")) {
            return true;
        }
        return false;
    }

    @Override
    public Preview createPreviewFor(File aFile) {
        ZipFile theZipFile;
        try {
            theZipFile = new ZipFile(aFile);
        } catch (IOException e) {
            LOGGER.error("Error opening "+aFile, e);
            return null;
        }
        try {
            ZipEntry theThumbnailEntry = theZipFile.getEntry("Thumbnails/thumbnail.png");
            if (theThumbnailEntry == null) {
                LOGGER.error("Cannot find thumbnail in "+aFile);
                return null;
            }

            BufferedImage theImage = ImageIO.read(new BufferedInputStream(theZipFile.getInputStream(theThumbnailEntry)));
            return new Preview(ImageUtils.rescale(theImage, THUMB_WIDTH, THUMB_HEIGHT, ImageUtils.RescaleMethod.RESIZE_FIT_ONE_DIMENSION));
        } catch (IOException e) {
            LOGGER.error("Error reading thumbnail from "+aFile, e);
            return null;
        } finally {
            try {
                theZipFile.close();
            } catch (Exception e) {
                // Do nothing
            }
        }
    }
}