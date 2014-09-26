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
package de.mirkosertic.desktopsearch.pdfpreview;

import de.mirkosertic.desktopsearch.Preview;
import de.mirkosertic.desktopsearch.PreviewGenerator;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import org.apache.pdfbox.pdfviewer.PageDrawer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

public class PDFPreviewGenerator implements PreviewGenerator {

    private static final int THUMB_WIDTH = (int)(210 / 2.5);
    private static final int THUMB_HEIGHT = (int) (297 / 2.5);

    @Override
    public Preview createPreviewFor(File aFile) {
        try {
            PDDocument theDocument = PDDocument.load(aFile);
            List<?> thePages = theDocument.getDocumentCatalog().getAllPages();
            PDPage theFirstPage = (PDPage) thePages.get(0);

            PDRectangle mBox = theFirstPage.findMediaBox();
            float theWidthPt = mBox.getWidth();
            float theHeightPt = mBox.getHeight();
            int theWidthPx = THUMB_WIDTH; // Math.round(widthPt * scaling);
            int theHeightPx = THUMB_HEIGHT; // Math.round(heightPt * scaling);
            float theScaling = THUMB_WIDTH / theWidthPt; // resolution / 72.0F;

            Dimension thePageDimension = new Dimension((int) theWidthPt, (int) theHeightPt);
            BufferedImage theImage = new BufferedImage(theWidthPx, theHeightPx, BufferedImage.TYPE_INT_RGB);
            Graphics2D theGraphics = (Graphics2D) theImage.getGraphics();
            theGraphics.setBackground(new Color(255, 255, 255, 0));

            theGraphics.clearRect(0, 0, theImage.getWidth(), theImage.getHeight());
            theGraphics.scale(theScaling, theScaling);
            PageDrawer theDrawer = new PageDrawer();
            theDrawer.drawPage(theGraphics, theFirstPage, thePageDimension);
            int rotation = theFirstPage.findRotation();
            if ((rotation == 90) || (rotation == 270)) {
                int w = theImage.getWidth();
                int h = theImage.getHeight();
                BufferedImage rotatedImg = new BufferedImage(w, h, theImage.getType());
                Graphics2D g = rotatedImg.createGraphics();
                g.rotate(Math.toRadians(rotation), w / 2, h / 2);
                g.drawImage(theImage, null, 0, 0);
            }

            return new Preview(theImage);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean supportsFile(File aFile) {
        return aFile.getName().toLowerCase().endsWith(".pdf");
    }
}