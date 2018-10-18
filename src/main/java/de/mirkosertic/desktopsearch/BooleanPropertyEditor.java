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