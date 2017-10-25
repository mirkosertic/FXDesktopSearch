/*
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

import org.apache.log4j.Logger;
import org.apache.tika.Tika;
import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.utils.DateUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ContentExtractor {

    private static final Logger LOGGER = Logger.getLogger(ContentExtractor.class);

    private final Tika tika;
    private final Pattern metaDataDatePattern;
    private final Configuration configuration;
    private final LanguageDetector languageDetector;

    public ContentExtractor(Configuration aConfiguration) {

        // TODO: auch korrekt dieses Muster verarbeitrn :  Mon Feb 18 15:55:10 CET 2013

        metaDataDatePattern = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})Z");

        configuration = aConfiguration;
        tika = new Tika();
        tika.setMaxStringLength(1024 * 1024 * 5);

        OptimaizeLangDetector theDetector = new OptimaizeLangDetector();
        try {
            theDetector.loadModels();
            languageDetector = theDetector;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String harmonizeMetaDataName(String aName) {
        int p = aName.indexOf(":");
        if (p>0) {
            aName = aName.substring(p+1);
        }

        String theReplacement = configuration.getMetaDataNameReplacement().get(aName);
        if (theReplacement != null) {
            return theReplacement;
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
                    theStringData = tika.parseToString(new BufferedInputStream(theStream), theMetaData)
                            .replace('\n', ' ')
                            .replace('\r', ' ')
                            .replace('\t',' ');
                }
            }

            LanguageResult theLanguageResult = languageDetector.detect(theStringData);

            FileTime theFileTime = aBasicFileAttributes.lastModifiedTime();
            SupportedLanguage theLanguage = SupportedLanguage.getDefault();
            try {
                theLanguage = SupportedLanguage.valueOf(theLanguageResult.getLanguage());
                if (!configuration.getEnabledLanguages().contains(theLanguage)) {
                    theLanguage = SupportedLanguage.getDefault();
                }
            } catch (Exception e) {
                LOGGER.info("Language " + theLanguageResult.getLanguage() + " was detected, but is not supported");
            }
            Content theContent = new Content(aFile.toString(), theStringData, aBasicFileAttributes.size(), theFileTime.toMillis(), theLanguage);
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
            LOGGER.error("Error extracting content of " + aFile, e);
        }

        return null;
    }

    public boolean supportsFile(String aFilename) {
        for (SupportedDocumentType theType : configuration.getEnabledDocumentTypes()) {
            if (theType.supports(aFilename)) {
                return true;
            }
        }
        return false;
    }
}