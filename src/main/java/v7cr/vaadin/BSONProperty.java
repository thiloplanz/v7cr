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

import org.bson.BSONObject;

import com.vaadin.data.util.AbstractProperty;

@SuppressWarnings("serial")
public class BSONProperty extends AbstractProperty {

	private final BSONObject bson;

	private final String fieldName;

	public BSONProperty(BSONObject bson, String fieldName) {
		this.bson = bson;
		this.fieldName = fieldName;
	}

	public Class<?> getType() {
		return getValue().getClass();
	}

	public Object getValue() {
		return bson.get(fieldName);
	}

	public void setValue(Object newValue) throws ReadOnlyException,
			ConversionException {
		bson.put(fieldName, newValue);
	}

	@Override
	public String toString() {
		Object x = getValue();
		if (x == null)
			return null;
		return x.toString();
	}

}
