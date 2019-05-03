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

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.controlsfx.control.PropertySheet;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class ConfigurationController {

    private static final String CATEGORY_COMMON = "Common";
    private static final String CATEGORY_SUGGEST = "Suggestion";
    private static final String CATEGORY_LANGUAGE = "Language analyzers";
    private static final String CATEGORY_FILEFORMATS = "File formats";

    @FXML
    ListView indexedDirectories;

    @FXML
    Button buttonAdd;

    @FXML
    Button buttonRemove;

    @FXML
    PropertySheet propertySheet;

    @FXML
    Button buttonOk;

    private ConfigurationManager configurationManager;
    private Stage stage;
    private Configuration currentConfiguration;

    public void initialize(final ConfigurationManager aConfigurationManager, final Stage aStage) {
        Objects.requireNonNull(propertySheet);
        Objects.requireNonNull(buttonOk);

        buttonRemove.setOnAction(actionEvent -> removeSelectedLocation());
        buttonAdd.setOnAction(actionEvent -> addNewLocation());
        buttonOk.setOnAction(actionEvent -> ok());

        stage = aStage;
        configurationManager = aConfigurationManager;
        currentConfiguration = configurationManager.getConfiguration();

        indexedDirectories.getItems().addAll(currentConfiguration.getCrawlLocations());

        propertySheet.getItems().add(new PropertyEditorItem(Integer.class, CATEGORY_COMMON, "Max number of documents in search result", SpinnerPropertyEditor.class) {

            @Override
            public Object getValue() {
                return currentConfiguration.getNumberOfSearchResults();
            }

            @Override
            public void setValue(final Object o) {
                currentConfiguration = currentConfiguration.updateNumberOfSearchResults((Integer) o);
            }
        });
        propertySheet.getItems().add(new PropertyEditorItem(boolean.class, CATEGORY_COMMON, "Crawl on startup", BooleanPropertyEditor.class) {

            @Override
            public Object getValue() {
                return currentConfiguration.isCrawlOnStartup();
            }

            @Override
            public void setValue(final Object o) {
                currentConfiguration = currentConfiguration.updateCrawlOnStartup((Boolean) o);
            }
        });
        propertySheet.getItems().add(new PropertyEditorItem(boolean.class, CATEGORY_COMMON, "Show similar documents", BooleanPropertyEditor.class) {

            @Override
            public Object getValue() {
                return currentConfiguration.isShowSimilarDocuments();
            }

            @Override
            public void setValue(final Object o) {
                currentConfiguration = currentConfiguration.updateIncludeSimilarDocuments((Boolean) o);
            }
        });
        propertySheet.getItems().add(new PropertyEditorItem(boolean.class, CATEGORY_COMMON, "Use title from metadata as filename", BooleanPropertyEditor.class) {

            @Override
            public Object getValue() {
                return currentConfiguration.isUseTitleAsFilename();
            }

            @Override
            public void setValue(final Object o) {
                currentConfiguration = currentConfiguration.updateUseTitleAsFilename((Boolean) o);
            }
        });
        propertySheet.getItems().add(new PropertyEditorItem(boolean.class, CATEGORY_COMMON, "Use Natural Language Processing", BooleanPropertyEditor.class) {

            @Override
            public Object getValue() {
                return currentConfiguration.isNaturalLanguageProcessing();
            }

            @Override
            public void setValue(final Object o) {
                currentConfiguration = currentConfiguration.updateNaturalLanguageProcessing((Boolean) o);
            }
        });
        propertySheet.getItems().add(new PropertyEditorItem(Integer.class, CATEGORY_COMMON, "Max number of facet entries", SpinnerPropertyEditor.class) {

            @Override
            public Object getValue() {
                return currentConfiguration.getFacetCount();
            }

            @Override
            public void setValue(final Object o) {
                currentConfiguration = currentConfiguration.updateFacetCount((Integer) o);
            }
        });
        propertySheet.getItems().add(new PropertyEditorItem(Integer.class, CATEGORY_SUGGEST, "Max number of suggestions", SpinnerPropertyEditor.class) {

            @Override
            public Object getValue() {
                return currentConfiguration.getNumberOfSuggestions();
            }

            @Override
            public void setValue(final Object o) {
                currentConfiguration = currentConfiguration.updateNumberOfSuggestions((Integer) o);
            }
        });
        propertySheet.getItems().add(new PropertyEditorItem(Integer.class, CATEGORY_SUGGEST, "Number of words before suggestion window", SpinnerPropertyEditor.class) {

            @Override
            public Object getValue() {
                return currentConfiguration.getSuggestionWindowBefore();
            }

            @Override
            public void setValue(final Object o) {
                currentConfiguration = currentConfiguration.updateSuggestionWindowBefore((Integer) o);
            }
        });
        propertySheet.getItems().add(new PropertyEditorItem(Integer.class, CATEGORY_SUGGEST, "Number of words after suggestion window", SpinnerPropertyEditor.class) {

            @Override
            public Object getValue() {
                return currentConfiguration.getSuggestionWindowAfter();
            }

            @Override
            public void setValue(final Object o) {
                currentConfiguration = currentConfiguration.updateSuggestionWindowAfter((Integer) o);
            }
        });
        propertySheet.getItems().add(new PropertyEditorItem(Integer.class, CATEGORY_SUGGEST, "Suggestion slop", SpinnerPropertyEditor.class) {

            @Override
            public Object getValue() {
                return currentConfiguration.getSuggestionSlop();
            }

            @Override
            public void setValue(final Object o) {
                currentConfiguration = currentConfiguration.updateSuggestionSlop((Integer) o);
            }
        });
        propertySheet.getItems().add(new PropertyEditorItem(boolean.class, CATEGORY_SUGGEST, "Show suggestions in order", BooleanPropertyEditor.class) {

            @Override
            public Object getValue() {
                return currentConfiguration.isSuggestionInOrder();
            }

            @Override
            public void setValue(final Object o) {
                currentConfiguration = currentConfiguration.updateSuggestionsInOrder((Boolean) o);
            }
        });

        for (final var theLanguage : SupportedLanguage.values()) {

            propertySheet.getItems().add(new PropertyEditorItem(boolean.class, CATEGORY_LANGUAGE, theLanguage.toLocale().getDisplayName(), BooleanPropertyEditor.class) {

                @Override
                public Object getValue() {
                    return currentConfiguration.getEnabledLanguages().contains(theLanguage);
                }

                @Override
                public void setValue(final Object o) {
                    currentConfiguration = currentConfiguration.enableLanguage(theLanguage);
                }
            });
        }

        for (final var theDocumentType : SupportedDocumentType.values()) {

            propertySheet.getItems().add(new PropertyEditorItem(boolean.class, CATEGORY_FILEFORMATS, theDocumentType.getDisplayName(Locale.getDefault()), BooleanPropertyEditor.class) {

                @Override
                public Object getValue() {
                    return currentConfiguration.getEnabledDocumentTypes().contains(theDocumentType);
                }

                @Override
                public void setValue(final Object o) {
                    currentConfiguration = currentConfiguration.enableDocumentType(theDocumentType);
                }
            });
        }

    }

    private void removeSelectedLocation() {

        final var theLocation = (Configuration.CrawlLocation) indexedDirectories.getSelectionModel().getSelectedItem();
        indexedDirectories.getItems().remove(theLocation);

        currentConfiguration = currentConfiguration.removeLocation(theLocation);
    }

    private void addNewLocation() {
        final var theChooser = new DirectoryChooser();
        theChooser.setTitle("Add new crawl location");
        final var theFile = theChooser.showDialog(stage.getOwner());
        if (theFile != null) {
            final var theNewLocation = new Configuration.CrawlLocation(UUID.randomUUID().toString(), theFile);
            indexedDirectories.getItems().add(theNewLocation);

            currentConfiguration = currentConfiguration.addLocation(theNewLocation);
        }
    }

    private void ok() {
        configurationManager.updateConfiguration(currentConfiguration);
        stage.hide();
    }
}