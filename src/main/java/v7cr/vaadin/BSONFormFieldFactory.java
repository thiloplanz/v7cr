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

import v7cr.v7db.SchemaDefinition;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormFieldFactory;
import com.vaadin.ui.TextField;

@SuppressWarnings("serial")
public class BSONFormFieldFactory implements FormFieldFactory {

	private final SchemaDefinition schema;

	public BSONFormFieldFactory(SchemaDefinition schema) {
		this.schema = schema;
	}

	public Field createField(Item item, Object propertyId, Component uiContext) {

		Property prop = item.getItemProperty(propertyId);

		if (prop instanceof BSONProperty) {
			String caption = schema.getFieldCaption((String) propertyId,
					uiContext.getLocale());
			TextField text = new TextField(prop);
			if (caption != null) {
				text.setCaption(caption);
			}
			text.setNullRepresentation("");
			SchemaDefinition fSchema = schema
					.getFieldDefinition((String) propertyId);
			if (fSchema != null && fSchema.isRequired())
				text.setRequired(true);
			return text;
		}

		return null;
	}

}
