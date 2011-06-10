/**
 * Copyright (c) 2011, Thilo Planz. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package v7cr.vaadin;

import v7cr.v7db.BSONBackedObject;
import v7cr.v7db.LocalizedString;
import v7cr.v7db.SchemaDefinition;

import com.vaadin.data.Item;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormFieldFactory;
import com.vaadin.ui.TextField;

public class ItemFactory {

	public static TextField getTextField(BSONBackedObject o, String fieldName) {
		SchemaDefinition schema = o.getSchemaDefinition();
		LocalizedString caption = schema != null ? schema.getCaption(fieldName)
				: null;
		String value = o.getStringField(fieldName);
		TextField t = new TextField(caption != null ? caption.toString()
				: fieldName);
		if (value != null)
			t.setValue(value);
		t.setColumns(100);
		return t;
	}

	public static FormFieldFactory getFormFieldFactory(
			final SchemaDefinition schema) {
		return new FormFieldFactory() {

			public Field createField(Item item, Object propertyId,
					Component uiContext) {
				String fieldName = (String) propertyId;
				LocalizedString caption = schema.getCaption(fieldName);
				if (caption == null)
					return new TextField(fieldName);
				return new TextField(caption.toString());
			}
		};
	}

}
