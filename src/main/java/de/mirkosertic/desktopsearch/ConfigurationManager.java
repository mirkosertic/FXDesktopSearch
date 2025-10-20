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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Component
public class ConfigurationManager {

    private Configuration configuration;
    private final File configurationFile;
    private final Set<ConfigurationChangeListener> configurationChangeListeners;

    public ConfigurationManager() {
        this(new File(SystemUtils.getUserHome(), "FXDesktopSearch"));
    }

    public ConfigurationManager(final File configDirectory) {

        if (!configDirectory.mkdirs()) {
            log.info("Directory {} could not be created. Maybe there is already a configuration?", configDirectory);
        }

        configurationChangeListeners = new HashSet<>();
        configurationFile = new File(configDirectory, "configuration.json");
        if (configurationFile.exists()) {
            try(final var theStream = new FileInputStream(configurationFile)) {
                loadConfigurationFrom(theStream);
            } catch (final Exception e) {
                initializeWithDefault(configDirectory);
            }
        } else {
            initializeWithDefault(configDirectory);
        }
    }

    private void initializeWithDefault(final File configDirectory) {
        try (final var theDefaultConfiguration = getClass().getResourceAsStream("/default-configuration.json")) {
            if (theDefaultConfiguration != null) {
                loadConfigurationFrom(theDefaultConfiguration);
            } else {
                log.error("No default configuration found");
                configuration = new Configuration(configDirectory);
            }
        } catch (final Exception e) {
            log.error("Error loading default configuration, initializing with empty one", e);
            configuration = new Configuration(configDirectory);
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

    public void updateConfiguration(final Configuration configuration) {
        this.configuration = configuration;
        writeConfiguration();
        configurationChangeListeners.stream().forEach(l -> {try {
            l.configurationUpdated(configuration);
        } catch (final Exception e) {
            log.error("Error notifying for configuration change", e);
        }});
    }

    private void loadConfigurationFrom(final InputStream streamToLoadFrom) throws IOException {
        final var theMapper = new ObjectMapper();
        configuration = theMapper.readValue(streamToLoadFrom, Configuration.class);
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