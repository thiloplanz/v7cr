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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

/**
 * An object whose properties are contained in a BSONObject, optionally using a
 * SchemaDefinition.
 * 
 * The object is immutable, subclasses must adhere to this contract.
 * 
 */

public class BSONBackedObject {

	private final BasicBSONObject bson;

	private final SchemaDefinition schema;

	private final static BasicBSONObject EMPTY = new BasicBSONObject();

	/**
	 * creates an empty object. You can use this as a starting point for
	 * building instances using the "append" methods.
	 */
	public BSONBackedObject() {
		bson = EMPTY;
		schema = null;
	}

	BSONBackedObject(BasicBSONObject b, SchemaDefinition schema) {
		this.bson = b;
		this.schema = schema;
	}

	protected BSONBackedObject(BSONBackedObject b, SchemaDefinition schema) {
		this.bson = b.bson;
		this.schema = schema;
	}

	public boolean isEmpty() {
		return bson.keySet().isEmpty();
	}

	public boolean containsField(String field) {
		return bson.containsField(field);
	}

	public Set<String> getFieldNames() {
		return new TreeSet<String>(bson.keySet());
	}

	public String getStringField(String field) {
		return (String) bson.get(field);
	}

	/**
	 * for a String field that can contain multiple values (cardinality > 1),
	 * return all of them as an array. An empty array will be returned as null.
	 */
	public String[] getStringFieldAsArray(String field) {
		Object o = bson.get(field);
		if (o == null)
			return null;
		if (o instanceof String)
			return new String[] { (String) o };
		if (o instanceof List<?>) {
			List<?> l = (List<?>) o;
			if (l.isEmpty())
				return null;
			return l.toArray(new String[] {});
		}
		if (o instanceof Object[]) {
			Object[] a = (Object[]) o;
			if (a.length == 0)
				return null;
			String[] x = new String[a.length];
			System.arraycopy(a, 0, x, 0, a.length);
			return x;
		}
		throw new RuntimeException("not a string field " + o);
	}

	/**
	 * returns the string representation of the BSON data that backs this
	 * object.
	 */
	public String toString() {
		return bson.toString();
	}

	public SchemaDefinition getSchemaDefinition() {
		return schema;
	}

	/**
	 * @return a copy of the underlying BSON data
	 */
	public BasicBSONObject getBSONObject() {
		return (BasicBSONObject) BSON.decode(BSON.encode(bson));
	}

	public BSONBackedObject getObjectField(String fieldName) {
		Object o = bson.get(fieldName);
		if (o == null)
			return null;
		if (o instanceof BasicBSONObject) {
			return new BSONBackedObject((BasicBSONObject) o, null);
		}
		throw new RuntimeException("not an object field " + o);
	}

	/**
	 * for a field that can contain multiple values (cardinality > 1), return
	 * all of them as an array. An empty array will be returned as null.
	 */
	public BSONBackedObject[] getObjectFieldAsArray(String field) {
		Object o = bson.get(field);
		if (o == null)
			return null;
		if (o instanceof Object[]) {
			o = Arrays.asList((Object[]) o);
			// get rid of the array for the next time
			bson.put(field, o);
		}
		if (o instanceof List<?>) {
			List<?> l = (List<?>) o;
			if (l.isEmpty())
				return null;
			BSONBackedObject[] r = new BSONBackedObject[l.size()];
			int i = 0;
			for (Object b : l) {
				if (b instanceof BasicBSONObject) {
					r[i++] = (new BSONBackedObject((BasicBSONObject) b, null));
				} else {
					throw new RuntimeException("not an object field " + b);
				}
			}
			return r;
		}
		if (o instanceof BasicBSONObject)
			return new BSONBackedObject[] { new BSONBackedObject(
					(BasicBSONObject) o, null) };

		throw new RuntimeException("not an object field " + o);
	}

	/**
	 * If the two objects are of the same class and their BSON data is equal,
	 * then they are considered equal
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BSONBackedObject && obj.getClass() == getClass()) {
			return ((BSONBackedObject) obj).bson.equals(bson);
		}
		return false;
	}

	/**
	 * @return the hashCode of the underlying BSON data
	 */
	@Override
	public int hashCode() {
		return bson.hashCode();
	}

	private BSONBackedObject _append(String key, Object value) {
		Set<String> fields = bson.keySet();
		BasicBSONObject copy = new BasicBSONObject(fields.size() + 1);
		copy.putAll((BSONObject) bson);
		copy.put(key, value);
		return new BSONBackedObject(copy, schema);
	}

	/**
	 * Since the object is immutable, all "append" methods return a modified
	 * copy that contains the new value.
	 */
	public BSONBackedObject append(String key, String value) {
		return _append(key, value);
	}

	/**
	 * Since the object is immutable, all "append" methods return a modified
	 * copy that contains the new value.
	 */
	public BSONBackedObject append(String key, BSONBackedObject value) {
		return _append(key, value.bson);
	}

	private List<Object> getList(String key) {
		Object o = bson.get(key);
		if (o == null)
			return new ArrayList<Object>();
		if (o instanceof List<?>)
			return new ArrayList<Object>((List<?>) o);
		if (o instanceof Object[]) {
			return new ArrayList<Object>(Arrays.asList((Object[]) o));
		}
		List<Object> l = new ArrayList<Object>();
		l.add(o);
		return l;
	}

	/**
	 * Adds the value to the end of the array. A new array will be created if
	 * missing, and a previously single element will be turned into an array
	 * (does not raise an error like MongoDB's $push operator would).
	 * <p>
	 * Since the object is immutable, all "push" methods return a modified copy
	 * that contains the new value.
	 */

	public BSONBackedObject push(String key, String value) {
		List<Object> l = getList(key);
		l.add(value);
		return _append(key, l);
	}

	
	/**
	 * Adds the value to the end of the array. A new array will be created if
	 * missing, and a previously single element will be turned into an array
	 * (does not raise an error like MongoDB's $push operator would).
	 * <p>
	 * Since the object is immutable, all "push" methods return a modified copy
	 * that contains the new value.
	 */

	public BSONBackedObject push(String key, BSONBackedObject value) {
		List<Object> l = getList(key);
		l.add(value.bson);
		return _append(key, l);
	}

	
	/**
	 * Adds the value to the end of the array, but only if the entry is not already there.
	 * A new array will be created if
	 * missing, and a previously single element will be turned into an array
	 * (does not raise an error like MongoDB's $addToSet operator would).
	 * <p>
	 * Since the object is immutable, all "addToSet" methods return a modified copy
	 * that contains the new value.
	 */

	public BSONBackedObject addToSet(String key, String value) {
		List<Object> l = getList(key);
		if (l.contains(value))
			return this;
		l.add(value);
		return _append(key, l);
	}
	
	/**
	 * Adds the value to the end of the array, but only if the entry is not already there.
	 * A new array will be created if
	 * missing, and a previously single element will be turned into an array
	 * (does not raise an error like MongoDB's $addToSet operator would).
	 * <p>
	 * Since the object is immutable, all "addToSet" methods return a modified copy
	 * that contains the new value.
	 */

	public BSONBackedObject addToSet(String key, BSONBackedObject value) {
		List<Object> l = getList(key);
		if (l.contains(value.bson))
			return this;
		l.add(value.bson);
		return _append(key, l);
	}

}
