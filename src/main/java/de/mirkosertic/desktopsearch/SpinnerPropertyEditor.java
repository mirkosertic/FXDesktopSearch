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

import javafx.beans.value.ObservableValue;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.editor.AbstractPropertyEditor;

public class SpinnerPropertyEditor extends AbstractPropertyEditor<Integer, Spinner<Integer>> {

    public SpinnerPropertyEditor(final PropertySheet.Item property) {
        super(property, new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 200)));
    }

    @Override
    protected ObservableValue<Integer> getObservableValue() {
        return getEditor().valueProperty();
    }

    @Override
    public void setValue(final Integer aValue) {
        getEditor().getValueFactory().setValue(aValue);
    }
}