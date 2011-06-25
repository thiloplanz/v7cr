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

import java.util.Locale;

import v7cr.v7db.LocalizedString;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;

@SuppressWarnings("serial")
public class LocalizedStringColumnGenerator implements ColumnGenerator {

	private final Locale l;

	public LocalizedStringColumnGenerator(Locale l) {
		this.l = l;
	}

	public Component generateCell(Table source, Object itemId, Object columnId) {
		Object value = source.getItem(itemId).getItemProperty(columnId)
				.getValue();
		if (value instanceof LocalizedString) {
			return new Label(((LocalizedString) value).toString(l));
		}
		return null;
	}

}
