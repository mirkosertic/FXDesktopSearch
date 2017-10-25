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

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.controlsfx.control.PropertySheet;

import java.io.File;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class ConfigurationController {

    private static String CATEGORY_COMMON = "Common";
    private static String CATEGORY_SUGGEST = "Suggestion";
    private static String CATEGORY_LANGUAGE = "Language analyzers";
    private static String CATEGORY_FILEFORMATS = "File formats";

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

    public void initialize(ConfigurationManager aConfigurationManager, Stage aStage) {
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
            public void setValue(Object o) {
                currentConfiguration = currentConfiguration.updateNumberOfSearchResults((Integer) o);
            }
        });
        propertySheet.getItems().add(new PropertyEditorItem(boolean.class, CATEGORY_COMMON, "Show similar documents", BooleanPropertyEditor.class) {

            @Override
            public Object getValue() {
                return currentConfiguration.isShowSimilarDocuments();
            }

            @Override
            public void setValue(Object o) {
                currentConfiguration = currentConfiguration.updateIncludeSimilarDocuments((Boolean) o);
            }
        });
        propertySheet.getItems().add(new PropertyEditorItem(Integer.class, CATEGORY_SUGGEST, "Max number of suggestions", SpinnerPropertyEditor.class) {

            @Override
            public Object getValue() {
                return currentConfiguration.getNumberOfSuggestions();
            }

            @Override
            public void setValue(Object o) {
                currentConfiguration = currentConfiguration.updateNumberOfSuggestions((Integer) o);
            }
        });
        propertySheet.getItems().add(new PropertyEditorItem(Integer.class, CATEGORY_SUGGEST, "Number of words before suggestion window", SpinnerPropertyEditor.class) {

            @Override
            public Object getValue() {
                return currentConfiguration.getSuggestionWindowBefore();
            }

            @Override
            public void setValue(Object o) {
                currentConfiguration = currentConfiguration.updateSuggestionWindowBefore((Integer) o);
            }
        });
        propertySheet.getItems().add(new PropertyEditorItem(Integer.class, CATEGORY_SUGGEST, "Number of words after suggestion window", SpinnerPropertyEditor.class) {

            @Override
            public Object getValue() {
                return currentConfiguration.getSuggestionWindowAfter();
            }

            @Override
            public void setValue(Object o) {
                currentConfiguration = currentConfiguration.updateSuggestionWindowAfter((Integer) o);
            }
        });
        propertySheet.getItems().add(new PropertyEditorItem(Integer.class, CATEGORY_SUGGEST, "Suggestion slop", SpinnerPropertyEditor.class) {

            @Override
            public Object getValue() {
                return currentConfiguration.getSuggestionSlop();
            }

            @Override
            public void setValue(Object o) {
                currentConfiguration = currentConfiguration.updateSuggestionSlop((Integer) o);
            }
        });
        propertySheet.getItems().add(new PropertyEditorItem(boolean.class, CATEGORY_SUGGEST, "Show suggestions in order", BooleanPropertyEditor.class) {

            @Override
            public Object getValue() {
                return currentConfiguration.isSuggestionInOrder();
            }

            @Override
            public void setValue(Object o) {
                currentConfiguration = currentConfiguration.updateSuggestionsInOrder((Boolean) o);
            }
        });

        for (SupportedLanguage theLanguage : SupportedLanguage.values()) {

            propertySheet.getItems().add(new PropertyEditorItem(boolean.class, CATEGORY_LANGUAGE, theLanguage.toLocale().getDisplayName(), BooleanPropertyEditor.class) {

                @Override
                public Object getValue() {
                    return currentConfiguration.getEnabledLanguages().contains(theLanguage);
                }

                @Override
                public void setValue(Object o) {
                    currentConfiguration = currentConfiguration.enableLanguage(theLanguage);
                }
            });
        }

        for (SupportedDocumentType theDocumentType : SupportedDocumentType.values()) {

            propertySheet.getItems().add(new PropertyEditorItem(boolean.class, CATEGORY_FILEFORMATS, theDocumentType.getDisplayName(Locale.getDefault()), BooleanPropertyEditor.class) {

                @Override
                public Object getValue() {
                    return currentConfiguration.getEnabledDocumentTypes().contains(theDocumentType);
                }

                @Override
                public void setValue(Object o) {
                    currentConfiguration = currentConfiguration.enableDocumentType(theDocumentType);
                }
            });
        }

    }

    private void removeSelectedLocation() {

        Configuration.CrawlLocation theLocation = (Configuration.CrawlLocation) indexedDirectories.getSelectionModel().getSelectedItem();
        indexedDirectories.getItems().remove(theLocation);

        currentConfiguration = currentConfiguration.removeLocation(theLocation);
    }

    private void addNewLocation() {
        DirectoryChooser theChooser = new DirectoryChooser();
        theChooser.setTitle("Add new crawl location");
        File theFile = theChooser.showDialog(stage.getOwner());
        if (theFile != null) {
            Configuration.CrawlLocation theNewLocation = new Configuration.CrawlLocation(UUID.randomUUID().toString(), theFile);
            indexedDirectories.getItems().add(theNewLocation);

            currentConfiguration = currentConfiguration.addLocation(theNewLocation);
        }
    }

    private void ok() {
        configurationManager.updateConfiguration(currentConfiguration);
        stage.hide();
    }
}