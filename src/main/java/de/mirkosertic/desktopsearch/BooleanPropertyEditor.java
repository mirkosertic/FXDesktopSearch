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
import javafx.scene.control.CheckBox;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.editor.AbstractPropertyEditor;

public class BooleanPropertyEditor extends AbstractPropertyEditor<Boolean, CheckBox> {

    public BooleanPropertyEditor(final PropertySheet.Item property) {
        super(property, new CheckBox());
    }

    @Override
    protected ObservableValue<Boolean> getObservableValue() {
        return getEditor().selectedProperty();
    }

    @Override
    public void setValue(final Boolean aBoolean) {
        getEditor().selectedProperty().setValue(aBoolean);
    }
}