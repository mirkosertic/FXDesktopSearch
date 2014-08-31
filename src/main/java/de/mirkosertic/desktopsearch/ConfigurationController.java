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
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class ConfigurationController implements Initializable {

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
    TextField indexLocation;

    @FXML
    Button buttonSetIndexLocation;

    @FXML
    Button buttonCancel;

    @FXML
    Button buttonOk;

    private Backend backend;

    private Stage stage;

    private final Set<FilesystemLocation> removedLocations = new HashSet<>();
    private final Set<FilesystemLocation> addedLocations = new HashSet<>();

    public void initialize(URL aUrl, ResourceBundle aResourceBundle) {
        Objects.requireNonNull(showSimilarResults);
        Objects.requireNonNull(numberDocuments);
        Objects.requireNonNull(indexedDirectories);
        Objects.requireNonNull(buttonAdd);
        Objects.requireNonNull(buttonRemove);
        Objects.requireNonNull(indexLocation);
        Objects.requireNonNull(buttonSetIndexLocation);
        Objects.requireNonNull(buttonCancel);
        Objects.requireNonNull(buttonOk);

        buttonRemove.setOnAction(actionEvent -> removeSelectedLocation());
        buttonAdd.setOnAction(actionEvent -> addNewLocation());
        buttonOk.setOnAction(actionEvent -> ok());
        buttonCancel.setOnAction(actionEvent -> cancel());
        buttonSetIndexLocation.setOnAction(actionEvent -> setIndexLocation());
    }

    private void ok() {

        for (FilesystemLocation theLocation : addedLocations) {
            try {
                backend.add(theLocation);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        removedLocations.forEach(backend::remove);

        backend.setIncludeSimilarDocuments(showSimilarResults.isSelected());
        backend.setNumberOfSearchResults((int) numberDocuments.getValue());
        try {
            backend.setIndexLocation(new File(indexLocation.getText()));
        } catch (IOException e) {
            e.printStackTrace();
        }

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
            FilesystemLocation theNewLocation = new FilesystemLocation(UUID.randomUUID().toString(), theFile);
            indexedDirectories.getItems().add(theNewLocation);

            addedLocations.add(theNewLocation);
        }
    }

    private void setIndexLocation() {
        DirectoryChooser theChooser = new DirectoryChooser();
        theChooser.setTitle("Select index data location");
        theChooser.setInitialDirectory(new File(indexLocation.getText()));
        File theFile = theChooser.showDialog(stage.getOwner());
        if (theFile != null) {
            indexLocation.setText(theFile.toString());
        }
    }

    public void initializeWithValues(Backend aBackend, Stage aStage) {
        backend = aBackend;
        stage = aStage;

        showSimilarResults.setSelected(backend.isIncludeSimilarDocuments());
        numberDocuments.setValue(backend.getNumberOfSearchResults());
        indexLocation.setText(backend.getIndexLocation());
        indexedDirectories.getItems().addAll(backend.getFileSystemLocations());
    }
}