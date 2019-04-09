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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class ImageUtils {

    public enum RescaleMethod {
        /**
         * Scale input image so that width and height is equal (or smaller) to the output size.
         * The other dimension will be smaller or equal than the output size.
         */
        RESIZE_FIT_BOTH_DIMENSIONS,
        /**
         * Scale input image so that width or height is equal to the output size.
         * The other dimension will be bigger or equal than the output size.
         */
        RESIZE_FIT_ONE_DIMENSION,
        /**
         * Do not resize the image. Instead, crop the image (if smaller) or center it (if bigger)
         */
        NO_RESIZE_ONLY_CROP,
        /**
         * Do not try to scale the image up, only down. If bigger, center it.
         */
        DO_NOT_SCALE_UP,
        /**
         * If output image is bigger than input image, allow the output to be smaller than expected (the size of the input image)
         */
        ALLOW_SMALLER,
    }

    public static BufferedImage rescale(final BufferedImage aImage, final int aWidth, final int aHeight, final RescaleMethod aRescaleMethod) {
        double theResizeRatio = 1;
        final int theOriginalWidth = aImage.getWidth();
        final int theOriginalHeight = aImage.getHeight();
        switch (aRescaleMethod)
        {
            case RESIZE_FIT_BOTH_DIMENSIONS:
                theResizeRatio = Math.min(((double) aWidth) / theOriginalWidth, ((double) aHeight) / theOriginalHeight);
                break;
            case RESIZE_FIT_ONE_DIMENSION:
                theResizeRatio = Math.max(((double) aWidth) / theOriginalWidth, ((double) aHeight) / theOriginalHeight);
                break;
            case NO_RESIZE_ONLY_CROP:
                theResizeRatio = 1.0;
                break;
        }

        final int theScaledWidth = (int) Math.round(theOriginalWidth * theResizeRatio);
        final int theScaledHeight = (int) Math.round(theOriginalHeight * theResizeRatio);
        final int theOffsetX;
        final int theOffsetY;

        // Center if smaller.
        if (theScaledWidth < aWidth) {
            theOffsetX = (aWidth - theScaledWidth) / 2;
        } else {
            theOffsetX = 0;
        }
        if (theScaledHeight < aHeight) {
            theOffsetY = (aHeight - theScaledHeight) / 2;
        } else {
            theOffsetY = 0;
        }

        final BufferedImage outputImage = new BufferedImage(aWidth, aHeight, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics2D = outputImage.createGraphics();

        // Fill background with white color
        graphics2D.setBackground(Color.WHITE);
        graphics2D.setPaint(Color.WHITE);
        graphics2D.fillRect(0, 0, aWidth, aHeight);

        // Enable smooth, high-quality resampling
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(aImage, theOffsetX, theOffsetY, theScaledWidth, theScaledHeight, null);
        graphics2D.dispose();

        return outputImage;
    }
}