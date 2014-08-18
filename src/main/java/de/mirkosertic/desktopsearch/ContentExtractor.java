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

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.utils.DateUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContentExtractor {

    private final Set<String> supportedExtensions;
    private final Tika tika;
    private final Pattern metaDataDatePattern;

    public ContentExtractor() {

        // TODO: auch korrekt dieses Muster verarbeitrn :  Mon Feb 18 15:55:10 CET 2013

        metaDataDatePattern = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})Z");
        supportedExtensions = new HashSet<>();
        supportedExtensions.add("txt");
        supportedExtensions.add("msg");
        supportedExtensions.add("pdf");
        supportedExtensions.add("doc");
        supportedExtensions.add("docx");
        supportedExtensions.add("ppt");
        supportedExtensions.add("pptx");
        supportedExtensions.add("rtf");
        supportedExtensions.add("html");

        tika = new Tika();
    }

    private String harmonizeMetaDataName(String aName) {
        if (aName.startsWith("meta:")) {
            aName = aName.substring("meta:".length());
        }
        if (aName.startsWith("dcterms:")) {
            aName = aName.substring("dcterms:".length());
        }
        if (aName.startsWith("xmp:")) {
            aName = aName.substring("xmp:".length());
        }
        if (aName.startsWith("dc:")) {
            aName = aName.substring("dc:".length());
        }
        if (aName.startsWith("cp:")) {
            aName = aName.substring("dc:".length());
        }
        if (aName.startsWith("custom:")) {
            aName = aName.substring("custom:".length());
        }
        if (aName.startsWith("extended-properties:")) {
            aName = aName.substring("extended-properties:".length());
        }

        if ("created".equals(aName)) {
            return "creation-date";
        }
        if ("date".equals(aName)) {
            return "creation-date";
        }
        if ("modified".equals(aName)) {
            return "last-modified";
        }
        if ("last-save-date".equals(aName)) {
            return "last-modified";
        }
        if ("save-date".equals(aName)) {
            return "last-modified";
        }
        if ("creatortool".equals(aName)) {
            return "application-name";
        }
        if ("producer".equals(aName)) {
            return "application-name";
        }
        if ("creator".equals(aName)) {
            return "author";
        }
        if ("last-author".equals(aName)) {
            return "author";
        }
        if ("print-date".equals(aName)) {
            return "last-printed";
        }
        if ("keyword".equals(aName)) {
            return "keywords";
        }
        if ("revision".equals(aName)) {
            return "revision-number";
        }
        if ("appversion".equals(aName)) {
            return "application-version";
        }
        if ("character count".equals(aName)) {
            return "character-count";
        }
        if ("xmptpg:npages".equals(aName)) {
            return "page-count";
        }
        if ("slide-count".equals(aName)) {
            return "page-count";
        }
        return aName;
    }

    public Content extractContentFrom(Path aFile, BasicFileAttributes aBasicFileAttributes) {
        try {
            Metadata theMetaData = new Metadata();

            String theStringData;
            // Files under 10 Meg are read into memory as a whole
            if (aBasicFileAttributes.size() < 1024 * 1024 * 4) {
                byte[] theData = Files.readAllBytes(aFile);
                theStringData = tika.parseToString(new ByteArrayInputStream(theData), theMetaData);
            } else {
                try (InputStream theStream = Files.newInputStream(aFile, StandardOpenOption.READ)) {
                    theStringData = tika.parseToString(new BufferedInputStream(theStream), theMetaData);
                }
            }

            FileTime theFileTime = aBasicFileAttributes.lastModifiedTime();
            Content theContent = new Content(aFile.toString(), theStringData, aBasicFileAttributes.size(), theFileTime.toMillis());
            for (String theName : theMetaData.names()) {

                String theMetaDataValue = theMetaData.get(theName);

                // Try to detect if this is a date
                Matcher theMatcher = metaDataDatePattern.matcher(theMetaDataValue);
                if (theMatcher.find()) {
                    int theYear = Integer.parseInt(theMatcher.group(1));
                    int theMonth = Integer.parseInt(theMatcher.group(2));
                    int theDay = Integer.parseInt(theMatcher.group(3));
                    int theHour = Integer.parseInt(theMatcher.group(4));
                    int theMinute = Integer.parseInt(theMatcher.group(5));
                    int theSecond = Integer.parseInt(theMatcher.group(6));

                    Calendar theCalendar = GregorianCalendar.getInstance(DateUtils.UTC, Locale.US);
                    theCalendar.set(Calendar.YEAR, theYear);
                    theCalendar.set(Calendar.MONTH, theMonth - 1);
                    theCalendar.set(Calendar.DAY_OF_MONTH, theDay);
                    theCalendar.set(Calendar.HOUR_OF_DAY, theHour);
                    theCalendar.set(Calendar.MINUTE, theMinute);
                    theCalendar.set(Calendar.SECOND, theSecond);
                    theCalendar.set(Calendar.MILLISECOND, 0);

                    theContent.addMetaData(harmonizeMetaDataName(theName.toLowerCase()), theCalendar.getTime());
                } else {
                    theContent.addMetaData(harmonizeMetaDataName(theName.toLowerCase()), theMetaData.get(theName));
                }
            }

            String theFileName = aFile.toString();
            int p = theFileName.lastIndexOf(".");
            if (p > 0) {
                String theExtension = theFileName.substring(p + 1);
                theContent.addMetaData(IndexFields.EXTENSION, theExtension.toLowerCase());
            }

            return theContent;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean supportsFile(String aFilename) {
        int p = aFilename.lastIndexOf(".");
        if (p < 0) {
            return false;
        }
        String theExtension = aFilename.substring(p + 1);
        return supportedExtensions.contains(theExtension.toLowerCase());
    }
}