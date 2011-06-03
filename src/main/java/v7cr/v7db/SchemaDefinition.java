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

package v7cr.v7db;

import java.util.HashMap;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class SchemaDefinition extends BSONBackedObject {

	/**
	 * name of the field that defines the possible data types for the object.
	 */
	public static final String DATA_TYPE = "dataType";

	/**
	 * name of the field that defines the fields for the object
	 */
	public static final String FIELDS = "fields";

	/**
	 * name of the field that defines the cardinality
	 */

	public static final String CARDINALITY = "cardinality";

	private final BSONBackedObject fields;

	// to scope the type definitions
	private final SchemaDefinition parent;

	
	public SchemaDefinition(BSONBackedObject bson) {
		super(bson, null);
		fields = getObjectField(FIELDS);
		parent = null;
	}

	private SchemaDefinition(BSONBackedObject bson, SchemaDefinition parent) {
		super(bson, null);
		fields = getObjectField(FIELDS);
		this.parent = parent;
	}

	SchemaDefinition(BasicBSONObject bson) {
		super(bson, null);
		fields = getObjectField(FIELDS);
		parent = null;
	}

	public SchemaDefinition getFieldDefinition(String fieldName) {
		if (fields == null)
			return null;
		BSONBackedObject field = fields.getObjectField(fieldName);
		if (field == null)
			return null;
		return new SchemaDefinition(field, this);
	}

	/**
	 * Returns the allowable data types. If unspecified, returns the implied
	 * <code>{ "object" }</code>.
	 * 
	 * @return an array of allowable data types, usually just one
	 */
	public String[] getDataTypes() {
		String[] d = getStringFieldAsArray(DATA_TYPE);
		if (d == null) {
			return new String[] { "object" };
		}
		return d;
	}

	/**
	 * Returns the cardinality for this object. If unspecified, returns the
	 * implied "1"
	 */
	public Cardinality getCardinality() {
		Cardinality c = Cardinality.getCardinality(getStringField(CARDINALITY));
		if (c == null)
			return Cardinality.ONE;
		return c;
	}

	SchemaDefinition withCardinalityN() {
		BasicBSONObject clone = getBSONObject();
		clone.put(CARDINALITY, Cardinality.N.toString());
		return new SchemaDefinition(clone);
	}

	SchemaDefinition withCardinality1() {
		BasicBSONObject clone = getBSONObject();
		clone.put(CARDINALITY, Cardinality.ONE.toString());
		return new SchemaDefinition(clone);
	}

	private Map<String, SchemaDefinition> localTypes;

	/**
	 * Returns the SchemaDefinition for a complex type, as per the current scope
	 * (i.e. the types defined in this schema take precedence)
	 * 
	 * @param name
	 *            without the leading colon (:)
	 */
	public SchemaDefinition getType(String name) {
		if (localTypes == null)
			localTypes = new HashMap<String, SchemaDefinition>();
		SchemaDefinition d = localTypes.get(name);
		if (d != null)
			return d;
		BSONBackedObject t = getObjectField("types");
		if (t == null) {
			if (parent != null)
				return parent.getType(name);
			return null;
		}
		BSONBackedObject s = t.getObjectField(name);
		if (s == null) {
			if (parent != null)
				return parent.getType(name);

			return null;
		}
		d = new SchemaDefinition(s);
		localTypes.put(name, d);
		return d;
	}

}
