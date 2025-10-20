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

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.utils.DateUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
public class ContentExtractor {

    private final Tika tika;
    private final Pattern metaDataDatePattern;
    private final Configuration configuration;
    private final LanguageDetector languageDetector;

    public ContentExtractor(final Configuration configuration) {

        // TODO: auch korrekt dieses Muster verarbeiten :  Mon Feb 18 15:55:10 CET 2013

        this.metaDataDatePattern = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})Z");

        this.configuration = configuration;
        this.tika = new Tika();
        this.tika.setMaxStringLength(1024 * 1024 * 5);

        try {
            this.languageDetector = LanguageDetector.getDefaultLanguageDetector();
            this.languageDetector.loadModels();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String harmonizeMetaDataName(String metadataName) {
        final var p = metadataName.indexOf(":");
        if (p>0) {
            metadataName = metadataName.substring(p+1);
        }

        final var theReplacement = configuration.getMetaDataNameReplacement().get(metadataName);
        if (theReplacement != null) {
            return theReplacement;
        }

        return metadataName;
    }

    public Content extractContentFrom(final Path fileToExtractContentFrom, final BasicFileAttributes fileAttributes) {
        try {
            final var theMetaData = new Metadata();

            final String theStringData;
            // Files under 10 Meg are read into memory as a whole
            if (fileAttributes.size() < 1024 * 1024 * 4) {
                final var theData = Files.readAllBytes(fileToExtractContentFrom);
                theStringData = tika.parseToString(new ByteArrayInputStream(theData), theMetaData)
                        .replace('\n', ' ')
                        .replace('\r', ' ')
                        .replace('\t',' ');
            } else {
                try (final var theStream = Files.newInputStream(fileToExtractContentFrom, StandardOpenOption.READ)) {
                    theStringData = tika.parseToString(new BufferedInputStream(theStream), theMetaData)
                            .replace('\n', ' ')
                            .replace('\r', ' ')
                            .replace('\t',' ');
                }
            }

            final var theLanguageResult = languageDetector.detect(theStringData);

            final var theFileTime = fileAttributes.lastModifiedTime();
            var theLanguage = SupportedLanguage.getDefault();
            try {
                theLanguage = SupportedLanguage.valueOf(theLanguageResult.getLanguage());
                if (!configuration.getEnabledLanguages().contains(theLanguage)) {
                    theLanguage = SupportedLanguage.getDefault();
                }
            } catch (final Exception e) {
                log.info("Language {} was detected, but is not supported", theLanguageResult.getLanguage());
            }
            final var theContent = new Content(fileToExtractContentFrom.toString(), theStringData, fileAttributes.size(), theFileTime.toMillis(), theLanguage);
            for (final var theName : theMetaData.names()) {

                final var theMetaDataValue = theMetaData.get(theName);

                // Try to detect if this is a date
                final var theMatcher = metaDataDatePattern.matcher(theMetaDataValue);
                if (theMatcher.find()) {
                    final var theYear = Integer.parseInt(theMatcher.group(1));
                    final var theMonth = Integer.parseInt(theMatcher.group(2));
                    final var theDay = Integer.parseInt(theMatcher.group(3));
                    final var theHour = Integer.parseInt(theMatcher.group(4));
                    final var theMinute = Integer.parseInt(theMatcher.group(5));
                    final var theSecond = Integer.parseInt(theMatcher.group(6));

                    final var theCalendar = GregorianCalendar.getInstance(DateUtils.UTC, Locale.US);
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

            final var theFileName = fileToExtractContentFrom.toString();
            final var p = theFileName.lastIndexOf(".");
            if (p > 0) {
                final var theExtension = theFileName.substring(p + 1);
                theContent.addMetaData(IndexFields.EXTENSION, theExtension.toLowerCase());
            }

            return theContent;
        } catch (final Exception e) {
            log.error("Error extracting content of {}", fileToExtractContentFrom, e);
        }

        return null;
    }

    public boolean supportsFile(final String filenameToCheck) {
        for (final var theType : configuration.getEnabledDocumentTypes()) {
            if (theType.supports(filenameToCheck)) {
                return true;
            }
        }
        return false;
    }
}
