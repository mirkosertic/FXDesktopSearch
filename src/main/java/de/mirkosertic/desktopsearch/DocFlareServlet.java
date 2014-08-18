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

import org.apache.commons.lang3.StringEscapeUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DocFlareServlet extends HttpServlet {

    private final Backend backend;

    public DocFlareServlet(Backend aBackend) {
        backend = aBackend;
    }

    private void writeJSon(PrintWriter aWriter, DocFlareElement aFlareElement) {
        aWriter.println("{");
        if (aFlareElement.getSize() != null) {
            aWriter.print("   \"size\" : ");
            aWriter.print(aFlareElement.getSize());
            aWriter.println(",");
        }
        aWriter.print("   \"name\" : \"");
        aWriter.print(StringEscapeUtils.escapeEcmaScript(aFlareElement.getName()));
        if (aFlareElement.getChildren().size() > 0) {
            aWriter.println("\",");
            aWriter.println("   \"children\" : [");

            List<DocFlareElement> theChildren = new ArrayList<>(aFlareElement.getChildren());
            Collections.sort(theChildren, (o1, o2) -> o1.getName().compareTo(o2.getName()));

            for (int i = 0; i < theChildren.size(); i++) {
                writeJSon(aWriter, theChildren.get(i));
                if (i < theChildren.size() - 1) {
                    aWriter.println(",");
                }
            }
            aWriter.println("   ]");
        } else {
            aWriter.println("\"");
        }
        aWriter.println("}");

    }

    @Override
    protected void doGet(HttpServletRequest aRequest, HttpServletResponse aRespose) throws ServletException,
            IOException {

        aRespose.setContentType("application/json");
        writeJSon(aRespose.getWriter(), backend.getDocFlare());
    }
}