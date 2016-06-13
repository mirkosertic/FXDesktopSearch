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

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class ConfigurationController {

    @FXML
    CheckBox showSimilarResults;

    @FXML
    Slider numberDocuments;

    @FXML
    ListView indexedDirectories;

    @FXML
    Button buttonAdd;

    @FXML
    Button buttonRemove;

    @FXML
    Button buttonCancel;

    @FXML
    Button buttonOk;

    @FXML
    VBox enabledDocumentFormats;

    @FXML
    VBox enabledLanguages;

    @FXML
    Slider numberSuggestions;

    @FXML
    Slider windowBeforeSpan;

    @FXML
    Slider windowAfterSpan;

    @FXML
    Slider slopForSuggestionSpan;

    @FXML
    CheckBox suggestionsInOrder;

    private ConfigurationManager configurationManager;
    private Stage stage;


    private final Set<Configuration.CrawlLocation> removedLocations = new HashSet<>();
    private final Set<Configuration.CrawlLocation> addedLocations = new HashSet<>();
    private final Map<SupportedDocumentType, CheckBox> supportedDocuments = new HashMap<>();
    private final Map<SupportedLanguage, CheckBox> supportedLanguages = new HashMap<>();

    public void initialize(ConfigurationManager aConfigurationManager, Stage aStage) {
        Objects.requireNonNull(showSimilarResults);
        Objects.requireNonNull(numberDocuments);
        Objects.requireNonNull(indexedDirectories);
        Objects.requireNonNull(buttonAdd);
        Objects.requireNonNull(buttonRemove);
        Objects.requireNonNull(buttonCancel);
        Objects.requireNonNull(buttonOk);
        Objects.requireNonNull(enabledDocumentFormats);
        Objects.requireNonNull(enabledLanguages);
        Objects.requireNonNull(numberSuggestions);
        Objects.requireNonNull(windowBeforeSpan);
        Objects.requireNonNull(windowAfterSpan);
        Objects.requireNonNull(slopForSuggestionSpan);
        Objects.requireNonNull(suggestionsInOrder);

        buttonRemove.setOnAction(actionEvent -> removeSelectedLocation());
        buttonAdd.setOnAction(actionEvent -> addNewLocation());
        buttonOk.setOnAction(actionEvent -> ok());
        buttonCancel.setOnAction(actionEvent -> cancel());

        stage = aStage;
        configurationManager = aConfigurationManager;

        Configuration theConfiguration = configurationManager.getConfiguration();

        showSimilarResults.setSelected(theConfiguration.isShowSimilarDocuments());
        numberDocuments.setValue(theConfiguration.getNumberOfSearchResults());
        numberSuggestions.setValue(theConfiguration.getNumberOfSuggestions());
        windowBeforeSpan.setValue(theConfiguration.getSuggestionWindowBefore());
        windowAfterSpan.setValue(theConfiguration.getSuggestionWindowAfter());
        slopForSuggestionSpan.setValue(theConfiguration.getSuggestionSlop());
        indexedDirectories.getItems().addAll(theConfiguration.getCrawlLocations());
        suggestionsInOrder.setSelected(theConfiguration.isSuggestionInOrder());

        // Ok, we build the document type selections
        for (SupportedDocumentType theType : SupportedDocumentType.values()) {
            CheckBox theCheckBox = new CheckBox(theType.getDisplayName(Locale.ENGLISH));
            theCheckBox.setSelected(theConfiguration.getEnabledDocumentTypes().contains(theType));
            enabledDocumentFormats.getChildren().add(theCheckBox);

            supportedDocuments.put(theType, theCheckBox);
        }

        // And also the languages
        for (SupportedLanguage theLanguage : SupportedLanguage.values()) {
            CheckBox theCheckbox = new CheckBox(theLanguage.toLocale().getDisplayName(Locale.ENGLISH));
            theCheckbox.setSelected(theConfiguration.getEnabledLanguages().contains(theLanguage));
            enabledLanguages.getChildren().add(theCheckbox);

            supportedLanguages.put(theLanguage, theCheckbox);
        }
    }

    private void ok() {

        Configuration theConfiguration = configurationManager.getConfiguration();

        for (Configuration.CrawlLocation theLocation : addedLocations) {
            theConfiguration = theConfiguration.addLocation(theLocation);
        }
        for (Configuration.CrawlLocation theRemovedLocation : removedLocations) {
            theConfiguration = theConfiguration.removeLocation(theRemovedLocation);
        }
        theConfiguration = theConfiguration.updateIncludeSimilarDocuments(showSimilarResults.isSelected());
        theConfiguration = theConfiguration.updateNumberOfSearchResults((int) numberDocuments.getValue());
        theConfiguration = theConfiguration.updateNumberOfSuggestions((int) numberSuggestions.getValue());
        theConfiguration = theConfiguration.updateSuggestionWindowBefore((int) windowBeforeSpan.getValue());
        theConfiguration = theConfiguration.updateSuggestionWindowAfter((int) windowAfterSpan.getValue());
        theConfiguration = theConfiguration.updateSuggestionSlop((int) slopForSuggestionSpan.getValue());
        theConfiguration = theConfiguration.updateSuggestionsInOrder(suggestionsInOrder.isSelected());

        for (Map.Entry<SupportedDocumentType, CheckBox> theEntry : supportedDocuments.entrySet()) {
            if (theEntry.getValue().isSelected()) {
                theConfiguration = theConfiguration.enableDocumentType(theEntry.getKey());
            } else {
                theConfiguration = theConfiguration.disableDocumentType(theEntry.getKey());
            }
        }

        for (Map.Entry<SupportedLanguage, CheckBox> theEntry : supportedLanguages.entrySet()) {
            if (theEntry.getValue().isSelected()) {
                theConfiguration = theConfiguration.enableLanguage(theEntry.getKey());
            } else {
                theConfiguration = theConfiguration.disableLanguage(theEntry.getKey());
            }
        }


        configurationManager.updateConfiguration(theConfiguration);
        stage.hide();
    }

    private void cancel() {
        stage.hide();
    }

    private void removeSelectedLocation() {
        removedLocations.addAll(indexedDirectories.getSelectionModel().getSelectedItems());
        indexedDirectories.getItems().removeAll(indexedDirectories.getSelectionModel().getSelectedItems());
    }

    private void addNewLocation() {
        DirectoryChooser theChooser = new DirectoryChooser();
        theChooser.setTitle("Add new crawl location");
        File theFile = theChooser.showDialog(stage.getOwner());
        if (theFile != null) {
            Configuration.CrawlLocation theNewLocation = new Configuration.CrawlLocation(UUID.randomUUID().toString(), theFile);
            indexedDirectories.getItems().add(theNewLocation);

            addedLocations.add(theNewLocation);
        }
    }
}