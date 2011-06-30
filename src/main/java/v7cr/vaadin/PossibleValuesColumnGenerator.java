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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import v7cr.v7db.LocalizedString;
import v7cr.v7db.SchemaDefinition;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;

@SuppressWarnings("serial")
public class PossibleValuesColumnGenerator implements ColumnGenerator {

	private final String field;

	private final Map<Object, String> names = new HashMap<Object, String>();

	public PossibleValuesColumnGenerator(SchemaDefinition d, String field,
			Locale l) {
		this.field = field;
		SchemaDefinition fieldSchema = d.getFieldDefinition(field);
		if (fieldSchema != null) {
			Object[] pv = fieldSchema.getPossibleValues();
			if (pv != null) {
				for (Object v : pv) {
					LocalizedString s = LocalizedString.get(fieldSchema
							.getPossibleValueMetaData(v), "caption");
					if (s != null) {
						names.put(v, s.toString(l));
					}
				}
			}
		}
	}

	public Component generateCell(Table source, Object itemId, Object columnId) {
		Object value = source.getItem(itemId).getItemProperty(field).getValue();
		String name = names.get(value);
		if (names != null)
			return new Label(name);
		return new Label(value.toString());
	}

}
