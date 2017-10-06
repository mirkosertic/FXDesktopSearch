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
import javafx.stage.Stage;
import org.controlsfx.control.PropertySheet;

import java.util.*;

public class ConfigurationController {

    private static String CATEGORY_COMMON = "Common";
    private static String CATEGORY_SUGGEST = "Suggestion";

    @FXML
    PropertySheet propertySheet;

    @FXML
    Button buttonOk;

    private ConfigurationManager configurationManager;
    private Stage stage;


    private final Set<Configuration.CrawlLocation> removedLocations = new HashSet<>();
    private final Set<Configuration.CrawlLocation> addedLocations = new HashSet<>();
    private final Map<SupportedDocumentType, CheckBox> supportedDocuments = new HashMap<>();
    private final Map<SupportedLanguage, CheckBox> supportedLanguages = new HashMap<>();

    public void initialize(ConfigurationManager aConfigurationManager, Stage aStage) {
        Objects.requireNonNull(propertySheet);
        Objects.requireNonNull(buttonOk);

        buttonOk.setOnAction(actionEvent -> ok());

        stage = aStage;
        configurationManager = aConfigurationManager;

        propertySheet.getItems().add(new PropertyEditorItem(Integer.class, CATEGORY_COMMON, "Max number of documents in search result", SpinnerPropertyEditor.class) {

            @Override
            public Object getValue() {
                return configurationManager.getConfiguration().getNumberOfSearchResults();
            }

            @Override
            public void setValue(Object o) {
                configurationManager.updateConfiguration(configurationManager.getConfiguration().updateNumberOfSearchResults((Integer) o));
            }
        });
        propertySheet.getItems().add(new PropertyEditorItem(boolean.class, CATEGORY_COMMON, "Show similar documents", BooleanPropertyEditor.class) {

            @Override
            public Object getValue() {
                return configurationManager.getConfiguration().isShowSimilarDocuments();
            }

            @Override
            public void setValue(Object o) {
                configurationManager.updateConfiguration(configurationManager.getConfiguration().updateIncludeSimilarDocuments((Boolean) o));
            }
        });
        propertySheet.getItems().add(new PropertyEditorItem(Integer.class, CATEGORY_SUGGEST, "Max number of suggestions", SpinnerPropertyEditor.class) {

            @Override
            public Object getValue() {
                return configurationManager.getConfiguration().getNumberOfSuggestions();
            }

            @Override
            public void setValue(Object o) {
                configurationManager.updateConfiguration(configurationManager.getConfiguration().updateNumberOfSuggestions((Integer) o));
            }
        });
        propertySheet.getItems().add(new PropertyEditorItem(Integer.class, CATEGORY_SUGGEST, "Number of words before suggestion window", SpinnerPropertyEditor.class) {

            @Override
            public Object getValue() {
                return configurationManager.getConfiguration().getSuggestionWindowBefore();
            }

            @Override
            public void setValue(Object o) {
                configurationManager.updateConfiguration(configurationManager.getConfiguration().updateSuggestionWindowBefore((Integer) o));
            }
        });
        propertySheet.getItems().add(new PropertyEditorItem(Integer.class, CATEGORY_SUGGEST, "Number of words after suggestion window", SpinnerPropertyEditor.class) {

            @Override
            public Object getValue() {
                return configurationManager.getConfiguration().getSuggestionWindowAfter();
            }

            @Override
            public void setValue(Object o) {
                configurationManager.updateConfiguration(configurationManager.getConfiguration().updateSuggestionWindowAfter((Integer) o));
            }
        });
        propertySheet.getItems().add(new PropertyEditorItem(Integer.class, CATEGORY_SUGGEST, "Suggestion slop", SpinnerPropertyEditor.class) {

            @Override
            public Object getValue() {
                return configurationManager.getConfiguration().getSuggestionSlop();
            }

            @Override
            public void setValue(Object o) {
                configurationManager.updateConfiguration(configurationManager.getConfiguration().updateSuggestionSlop((Integer) o));
            }
        });
        propertySheet.getItems().add(new PropertyEditorItem(boolean.class, CATEGORY_SUGGEST, "Show suggestions in order", BooleanPropertyEditor.class) {

            @Override
            public Object getValue() {
                return configurationManager.getConfiguration().isSuggestionInOrder();
            }

            @Override
            public void setValue(Object o) {
                configurationManager.updateConfiguration(configurationManager.getConfiguration().updateSuggestionsInOrder((Boolean) o));
            }
        });
    }

    private void ok() {

        stage.hide();
    }
}