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

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.bson.BasicBSONObject;

public class SchemaDefinition {

	private final BSONBackedObject bson;

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

	/**
	 * name of the field that holds the required flag
	 */

	public static final String REQUIRED = "required";

	/**
	 * name of the field that holds the possible values
	 */

	public static final String POSSIBLE_VALUES = "possibleValues";

	private final BSONBackedObject fields;

	// to scope the type definitions
	private final SchemaDefinition parent;

	public SchemaDefinition(BSONBackedObject bson) {
		this(bson, null);
	}

	private SchemaDefinition(BSONBackedObject bson, SchemaDefinition parent) {
		this.bson = bson;
		this.parent = parent;
		BSONBackedObject f = bson.getObjectField(FIELDS);
		if (f == null) {
			// support field definitions in a nested dataType
			// but only if there is just one option
			String[] dt = getDataTypes();
			if (dt.length == 1 && dt[0].startsWith(":")) {
				SchemaDefinition type = getType(dt[0].substring(1));
				if (type == null) {
					System.err.println("unresolved nested dataType " + dt[0]);
				} else {
					f = type.fields;
				}
			}
		}
		fields = f;
	}

	SchemaDefinition(BasicBSONObject bson) {
		this(BSONBackedObjectLoader.wrap(bson, null));
	}

	/**
	 * returns an empty set if there are no field definitions
	 * 
	 * @return the names of fields for which definitions exist
	 */
	public Set<String> getFieldNames() {
		if (fields == null)
			return Collections.emptySet();
		return fields.getFieldNames();
	}

	/**
	 * The caption should be used in user interface elements (such as form input
	 * labels or table headers) to name the field
	 * 
	 * @return the caption for the named field (or null, if unspecified)
	 */
	public LocalizedString getFieldCaption(String fieldName) {
		SchemaDefinition sd = getFieldDefinition(fieldName);
		if (sd == null)
			return null;

		return LocalizedString.get(sd.bson, "caption");
	}

	/**
	 * The caption should be used in user interface elements (such as form input
	 * labels or table headers) to name the field
	 * 
	 * @return the caption for the named field (or null, if unspecified)
	 */

	public String getFieldCaption(String fieldName, Locale l) {
		SchemaDefinition sd = getFieldDefinition(fieldName);
		if (sd == null)
			return null;

		return LocalizedString.get(sd.bson, "caption", l);
	}

	/**
	 * Returns the definition for the named field.
	 */

	public SchemaDefinition getFieldDefinition(String fieldName) {
		if (fields == null)
			return null;
		int idx = fieldName.indexOf('.');
		if (idx > -1) {
			String first = fieldName.substring(0, idx);
			SchemaDefinition d = getFieldDefinition(first);
			if (d == null)
				return null;
			return d.getFieldDefinition(fieldName.substring(idx + 1));
		}

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
		String[] d = bson.getStringFieldAsArray(DATA_TYPE);
		if (d == null) {
			return new String[] { "object" };
		}
		return d;
	}

	/**
	 * Returns the possible values for this object. If unrestricted returns
	 * null.
	 * <p>
	 * This returns just the actual possible values, not any other meta-data the
	 * schema may contain. For the meta-data, use getPossibleValueMetaData
	 */

	public Object[] getPossibleValues() {
		Object p = bson.getField(POSSIBLE_VALUES);
		if (p == null)
			return null;
		if (!(p instanceof Object[])) {
			p = new Object[] { p };
		}
		Object[] o = (Object[]) p;
		int i = 0;
		for (Object x : o) {
			if (x instanceof BSONBackedObject) {
				o[i] = ((BSONBackedObject) x).getField("_id");
			}
			i++;
		}
		return o;
	}

	/**
	 * Returns the meta-data associated with a possible value defined for this
	 * object. If values are unrestricted, returns null. If the value is not
	 * possible, returns null. If there is no meta-data, returns `{ _id :
	 * theValue } `.
	 */

	public BSONBackedObject getPossibleValueMetaData(Object value) {
		Object p = bson.getField(POSSIBLE_VALUES);
		if (p == null)
			return null;
		if (!(p instanceof Object[])) {
			p = new Object[] { p };
		}
		Object[] o = (Object[]) p;
		for (Object x : o) {
			if (x instanceof BSONBackedObject) {

				BSONBackedObject b = ((BSONBackedObject) x);
				Object id = b.getField("_id");
				if (value.equals(id))
					return b;
				continue;
			}
			if (value.equals(x))
				return BSONBackedObjectLoader.wrap(
						new BasicBSONObject("_id", x), null);
		}
		return null;
	}

	/**
	 * Returns the cardinality for this object. If unspecified, returns the
	 * implied "1"
	 */
	public Cardinality getCardinality() {
		Cardinality c = Cardinality.getCardinality(bson
				.getStringField(CARDINALITY));
		if (c == null)
			return Cardinality.ONE;
		return c;
	}

	SchemaDefinition withCardinalityN() {
		return new SchemaDefinition(bson.append(CARDINALITY, Cardinality.N
				.toString()), parent);
	}

	SchemaDefinition withCardinality1() {
		return new SchemaDefinition(bson.append(CARDINALITY, Cardinality.ONE
				.toString()), parent);
	}

	public boolean isRequired() {
		return Boolean.TRUE.equals(bson.getBooleanField("required"));
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
		BSONBackedObject t = bson.getObjectField("types");
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
		d = new SchemaDefinition(s, this);
		localTypes.put(name, d);
		return d;
	}

	public static SchemaDefinition parse(String json) {
		return new SchemaDefinition(BSONBackedObjectLoader.parse(json, null));
	}

}
