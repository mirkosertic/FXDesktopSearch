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

import jakarta.servlet.http.HttpServletResponse;
import javafx.application.Platform;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

@Controller
public class BringToFrontController {

    private final DesktopSearchMain desktopSearchMain;

    public BringToFrontController(final DesktopSearchMain main) {
        desktopSearchMain = main;
    }

    @GetMapping("/bringToFront")
    protected void doGet(final HttpServletResponse response) throws IOException {
        Platform.runLater(desktopSearchMain::bringToFront);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain");
        response.getWriter().print("Ok");
    }
}
