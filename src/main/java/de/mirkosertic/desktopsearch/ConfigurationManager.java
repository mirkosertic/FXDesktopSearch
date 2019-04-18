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

import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Slf4j
public class ConfigurationManager {

    private Configuration configuration;
    private final File configurationFile;
    private final Set<ConfigurationChangeListener> configurationChangeListeners;

    public ConfigurationManager(final File aConfigDirectory) {
        configurationChangeListeners = new HashSet<>();
        configurationFile = new File(aConfigDirectory, "configuration.json");
        if (configurationFile.exists()) {
            try(final var theStream = new FileInputStream(configurationFile)) {
                loadConfigurationFrom(theStream);
            } catch (final Exception e) {
                initializeWithDefault(aConfigDirectory);
            }
        } else {
            initializeWithDefault(aConfigDirectory);
        }
    }

    private void initializeWithDefault(final File aConfigDirectory) {
        try (final var theDefaultConfiguration = getClass().getResourceAsStream("/default-configuration.json")) {
            if (theDefaultConfiguration != null) {
                loadConfigurationFrom(theDefaultConfiguration);
            } else {
                log.error("No default configuration found");
                configuration = new Configuration(aConfigDirectory);
            }
        } catch (final Exception e) {
            log.error("Error loading default configuration, initializing with empty one", e);
            configuration = new Configuration(aConfigDirectory);
        }

        // By default, we enable only the english parser
        // and additionally the parser for the system locale
        configuration = configuration.enableLanguage(SupportedLanguage.en);

        try {
            final var theAdditionalLanguage = SupportedLanguage.valueOf(Locale.getDefault().getLanguage());
            configuration = configuration.enableLanguage(theAdditionalLanguage);
        } catch (final Exception e) {
            // Platform language seems not to be supported
        }

        // By default, we also enable all document parsers
        for (final var theType : SupportedDocumentType.values()) {
            configuration = configuration.enableDocumentType(theType);
        }

        writeConfiguration();
    }

    public void addChangeListener(final ConfigurationChangeListener aChangeListener) {
        configurationChangeListeners.add(aChangeListener);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void updateConfiguration(final Configuration aConfiguration) {
        configuration = aConfiguration;
        writeConfiguration();
        configurationChangeListeners.stream().forEach(l -> {try {
            l.configurationUpdated(aConfiguration);
        } catch (final Exception e) {
            log.error("Error notifying for configuration change", e);
        }});
    }

    private void loadConfigurationFrom(final InputStream aStream) throws IOException {
        final var theMapper = new ObjectMapper();
        configuration = theMapper.readValue(aStream, Configuration.class);
    }

    private void writeConfiguration() {
        try (final var theStream = new FileOutputStream(configurationFile)) {
            final var theMapper = new ObjectMapper();
            theMapper.writeValue(theStream, configuration);
        } catch (final Exception e) {
            log.error("Error writing configuration", e);
        }
    }
}