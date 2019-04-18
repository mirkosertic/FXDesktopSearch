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
        final var theOriginalWidth = aImage.getWidth();
        final var theOriginalHeight = aImage.getHeight();
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

        final var theScaledWidth = (int) Math.round(theOriginalWidth * theResizeRatio);
        final var theScaledHeight = (int) Math.round(theOriginalHeight * theResizeRatio);
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

        final var outputImage = new BufferedImage(aWidth, aHeight, BufferedImage.TYPE_INT_ARGB);
        final var graphics2D = outputImage.createGraphics();

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