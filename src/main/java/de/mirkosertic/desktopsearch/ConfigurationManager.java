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

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ConfigurationManager {

    private static final Logger LOGGER = Logger.getLogger(ConfigurationManager.class);

    private Configuration configuration;
    private final File configurationFile;
    private final Set<ConfigurationChangeListener> configurationChangeListeners;

    public ConfigurationManager(File aConfigDirectory) {
        configurationChangeListeners = new HashSet<>();
        configurationFile = new File(aConfigDirectory, "configuration.json");
        if (configurationFile.exists()) {
            try(FileInputStream theStream = new FileInputStream(configurationFile)) {
                loadConfigurationFrom(theStream);
            } catch (Exception e) {
                initializeWithDefault(aConfigDirectory);
            }
        } else {
            initializeWithDefault(aConfigDirectory);
        }
    }

    private void initializeWithDefault(File aConfigDirectory) {
        try (InputStream theDefaultConfiguration = getClass().getResourceAsStream("/default-configuration.json")) {
            if (theDefaultConfiguration != null) {
                loadConfigurationFrom(theDefaultConfiguration);
            } else {
                LOGGER.error("No default configuration found");
                configuration = new Configuration(aConfigDirectory);
            }
        } catch (Exception e) {
            LOGGER.error("Error loading default configuration, initializing with empty one", e);
            configuration = new Configuration(aConfigDirectory);
        }

        // By default, we enable only the english parser
        // and additionally the parser for the system locale
        configuration = configuration.enableLanguage(SupportedLanguage.en);

        try {
            SupportedLanguage theAdditionalLanguage = SupportedLanguage.valueOf(Locale.getDefault().getLanguage());
            configuration = configuration.enableLanguage(theAdditionalLanguage);
        } catch (Exception e) {
            // Platform language seems not to be supported
        }

        // By default, we also enable all document parsers
        for (SupportedDocumentType theType : SupportedDocumentType.values()) {
            configuration = configuration.enableDocumentType(theType);
        }

        writeConfiguration();
    }

    public void addChangeListener(ConfigurationChangeListener aChangeListener) {
        configurationChangeListeners.add(aChangeListener);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void updateConfiguration(Configuration aConfiguration) {
        configuration = aConfiguration;
        writeConfiguration();
        configurationChangeListeners.stream().forEach(l -> {try {
            l.configurationUpdated(aConfiguration);
        } catch (Exception e) {
            LOGGER.error("Error notifying for configuration change", e);
        }});
    }

    private void loadConfigurationFrom(InputStream aStream) throws IOException {
        ObjectMapper theMapper = new ObjectMapper();
        configuration = theMapper.readValue(aStream, Configuration.class);
    }

    private void writeConfiguration() {
        try (FileOutputStream theStream = new FileOutputStream(configurationFile)) {
            ObjectMapper theMapper = new ObjectMapper();
            theMapper.writeValue(theStream, configuration);
        } catch (Exception e) {
            LOGGER.error("Error writing configuration", e);
        }
    }
}