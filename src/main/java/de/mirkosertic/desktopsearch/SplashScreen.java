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

import javax.swing.*;
import java.awt.*;
import java.net.URL;

class SplashScreen extends JWindow {

    public SplashScreen(final URL iconUrl) {
        final var icon = new ImageIcon(iconUrl);
        final var imageLabel = new JLabel(icon);
        toFront();
        getContentPane().add(imageLabel, BorderLayout.CENTER);

    }

    @Override
    public void setVisible(final boolean bStatus) {
        pack();
        center();
        super.setVisible(bStatus);
    }

    private boolean isDoubleScreen() {

        final var screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final var ratio = (double) screenSize.width / (double) screenSize.height;
        return ratio > 2.0;
    }

    private void center() {

        if (!isDoubleScreen()) {
            final var screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            final var windowSize = getSize();
            setLocation(calculateLocationWidth(windowSize, screenSize.width), calculateLocationHeigth(
                    windowSize, screenSize));
        } else {
            centerOnLeftScreen();
        }
    }

    private int calculateLocationHeigth(final Dimension windowSize, final Dimension screenSize) {

        if (windowSize.height > screenSize.height) {
            windowSize.height = screenSize.height;
        }
        var height = (screenSize.height - windowSize.height) / 2;
        final var screenBottomOffset = 23;
        if (height < screenBottomOffset) {
            height = 0;
        }
        return height;
    }

    private int calculateLocationWidth(final Dimension windowSize, final int screenSizeWidth) {

        if (windowSize.width > screenSizeWidth) {
            windowSize.width = screenSizeWidth;
        }
        return (screenSizeWidth - windowSize.width) / 2;
    }

    private void centerOnLeftScreen() {

        if (!isDoubleScreen()) {
            center();
            return;
        }
        final var screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final var windowSize = getSize();
        final var halfWidth = screenSize.width / 2;
        setLocation(calculateLocationWidth(windowSize, halfWidth), calculateLocationHeigth(windowSize,
                screenSize));
    }
}
