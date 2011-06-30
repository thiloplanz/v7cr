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

import java.util.Collection;

import org.bson.BSONObject;

import v7cr.v7db.SchemaDefinition;

import com.vaadin.data.Item;
import com.vaadin.data.Property;

@SuppressWarnings("serial")
public class BSONItem implements Item {

	private final BSONObject bson;

	private final SchemaDefinition schema;

	public BSONItem(BSONObject b) {
		bson = b;
		schema = null;
	}

	public BSONItem(BSONObject b, SchemaDefinition s) {
		bson = b;
		schema = s;
	}

	public boolean addItemProperty(Object id, Property property)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public Property getItemProperty(Object id) {
		if (id instanceof String) {
			String fieldName = (String) id;
			return new BSONProperty(bson, fieldName);
		}
		return null;
	}

	public Collection<?> getItemPropertyIds() {
		if (schema != null)
			return schema.getFieldNames();
		return bson.keySet();
	}

	public boolean removeItemProperty(Object id)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public BSONObject getBSONObject() {
		return bson;
	}

	@Override
	public String toString() {
		return bson.toString();
	}

}
