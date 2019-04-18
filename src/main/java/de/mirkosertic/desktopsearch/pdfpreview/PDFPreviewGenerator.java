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
package de.mirkosertic.desktopsearch.pdfpreview;

import de.mirkosertic.desktopsearch.Preview;
import de.mirkosertic.desktopsearch.PreviewConstants;
import de.mirkosertic.desktopsearch.PreviewGenerator;
import de.mirkosertic.desktopsearch.SupportedDocumentType;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class PDFPreviewGenerator implements PreviewGenerator, PreviewConstants {

    private final Set<SupportedDocumentType> suppportedDocumentTypes;

    public PDFPreviewGenerator() {
        suppportedDocumentTypes = new HashSet<>();
        suppportedDocumentTypes.add(SupportedDocumentType.pdf);
    }

    @Override
    public Preview createPreviewFor(final File aFile) {
        try(final var theDocument = PDDocument.load(aFile))  {
            final var thePages = theDocument.getPages();
            if (thePages.getCount() == 0) {
                return null;
            }
            final var theFirstPage = thePages.get(0);

            final var mBox = theFirstPage.getMediaBox();
            final var theWidthPt = mBox.getWidth();
            final var theWidthPx = THUMB_WIDTH; // Math.round(widthPt * scaling);
            final var theHeightPx = THUMB_HEIGHT; // Math.round(heightPt * scaling);
            final var theScaling = THUMB_WIDTH / theWidthPt; // resolution / 72.0F;

            final var theImage = new BufferedImage(theWidthPx, theHeightPx, BufferedImage.TYPE_INT_RGB);
            final var theGraphics = (Graphics2D) theImage.getGraphics();
            theGraphics.setBackground(new Color(255, 255, 255, 0));
            theGraphics.clearRect(0, 0, theImage.getWidth(), theImage.getHeight());

            final var theRenderer = new PDFRenderer(theDocument);
            theRenderer.renderPageToGraphics(0, theGraphics, theScaling);

            final var rotation = theFirstPage.getRotation();
            if ((rotation == 90) || (rotation == 270)) {
                final var w = theImage.getWidth();
                final var h = theImage.getHeight();
                final var rotatedImg = new BufferedImage(w, h, theImage.getType());
                final var g = rotatedImg.createGraphics();
                g.rotate(Math.toRadians(rotation), w / 2, h / 2);
                g.drawImage(theImage, null, 0, 0);
            }
            theGraphics.dispose();
            return new Preview(theImage);
        } catch (final Exception e) {
            log.error("Error creating preview for {}", aFile, e);
            return null;
        }
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
}