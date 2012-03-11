/**
 * Copyright (c) 2011-2012, Thilo Planz. All rights reserved.
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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.ArrayUtils;
import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * An object whose properties are contained in a BSONObject, optionally using a
 * SchemaDefinition.
 * 
 * The object is immutable, subclasses must adhere to this contract.
 * 
 * It also provides a selection of useful accessor and mutator methods,
 * including support for nested fields. Since the object is immutable, the
 * mutator methods return a modified copy.
 * 
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

	// for nested fields
	// returns { parentObject, localFieldName }

	private static Object[] drillDownToParent(Map<?, ?> data, String field) {
		if (field == null) {
			return null;
		}
		int idx = field.indexOf('.');
		if (idx == -1) {
			return new Object[] { data, field };
		}
		String head = field.substring(0, idx);
		String tail = field.substring(idx + 1);
		Object o = data.get(head);
		if (o instanceof Map<?, ?>) {
			return drillDownToParent((Map<?, ?>) o, tail);
		}
		return null;
	}

	// for nested fields
	private Object drillDown(String field) {
		Object[] d = drillDownToParent(bson, field);
		if (d == null)
			return null;
		return ((Map<?, ?>) d[0]).get(d[1]);
	}

	public boolean containsField(String field) {
		Object[] d = drillDownToParent(bson, field);
		if (d == null)
			return false;

		return ((Map<?, ?>) d[0]).containsKey(field);
	}

	public Set<String> getFieldNames() {
		return new TreeSet<String>(bson.keySet());
	}

	public String getStringField(String field) {
		return (String) getField(field);
	}

	/**
	 * for a String field that can contain multiple values (cardinality > 1),
	 * return all of them as an array. An empty array will be returned as null.
	 */
	public String[] getStringFieldAsArray(String field) {
		Object o = drillDown(field);
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

	public ObjectId getObjectIdField(String field) {
		return (ObjectId) getField(field);
	}

	public Long getLongField(String field) {
		return (Long) getField(field);
	}

	public Date getDateField(String field) {
		return (Date) getField(field);
	}

	public Boolean getBooleanField(String field) {
		return (Boolean) getField(field);
	}

	public Integer getIntegerField(String field) {
		return (Integer) getField(field);
	}

	/**
	 * returns the field value, which can be any type of object. Use this only,
	 * if you do not know the type in advance, otherwise the typed methods (such
	 * as getStringField) are preferred.
	 * 
	 * <p>
	 * If the field has multiple values, an array is returned.
	 * 
	 * <p>
	 * Always returns immutable objects or copies of the original data, so any
	 * changes made to it later do not affect this object.
	 * 
	 */
	public Object getField(String field) {
		Object o = drillDown(field);
		if (o == null)
			return null;
		if (o instanceof String || o instanceof Boolean || o instanceof Long
				|| o instanceof ObjectId || o instanceof Integer)
			return o;
		// Date is mutable...
		if (o instanceof Date)
			return ((Date) o).clone();

		if (o instanceof BasicBSONObject) {
			return new BSONBackedObject((BasicBSONObject) o, null);
		}

		if (o instanceof List<?>) {
			o = ((List<?>) o).toArray();
		}
		if (o instanceof Object[]) {
			Object[] a = (Object[]) o;
			int i = 0;
			for (Object m : a) {
				if (m instanceof Date) {
					a[i] = ((Date) m).clone();
				} else if (m instanceof BasicBSONObject) {
					a[i] = new BSONBackedObject((BasicBSONObject) m, null);
				} else if (m instanceof String || m instanceof Boolean
						|| m instanceof Long || m instanceof ObjectId
						|| m instanceof Integer) {
					// immutable, no need to do anything
				} else
					throw new RuntimeException("unsupported field type "
							+ o.getClass().getName() + " for '" + field
							+ "' : " + o);

				i++;
			}
			return a;
		}
		if (o instanceof byte[]) {
			byte[] b = (byte[]) o;
			return ArrayUtils.clone(b);
		}
		throw new RuntimeException("unsupported field type "
				+ o.getClass().getName() + " for '" + field + "' : " + o);
	}

	/**
	 * returns the string representation of the BSON data that backs this
	 * object.
	 */
	@Override
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

	/**
	 * @return a copy of the underlying BSON data
	 */
	public DBObject getDBObject() {
		return new BasicDBObject(getBSONObject());
	}

	public BSONBackedObject getObjectField(String fieldName) {
		Object o = drillDown(fieldName);
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
		Object o = drillDown(field);
		if (o == null)
			return null;
		if (o instanceof Object[]) {
			o = Arrays.asList((Object[]) o);
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

	// for nested fields
	// returns { root, parent }
	private static BasicBSONObject[] createPath(Map<?, ?> data, String field) {
		if (field == null)
			return null;
		int idx = field.indexOf('.');
		if (idx == -1) {
			BasicBSONObject copy = new BasicBSONObject();
			copy.putAll((BSONObject) data);
			return new BasicBSONObject[] { copy, copy };
		}
		String head = field.substring(0, idx);
		String tail = field.substring(idx + 1);
		Object x = data.get(head);
		if (x instanceof Map<?, ?>) {
			BasicBSONObject copy = new BasicBSONObject();
			copy.putAll((BSONObject) data);
			try {
				BasicBSONObject[] r = createPath((Map<?, ?>) x, tail);
				copy.put(head, r[0]);
				return new BasicBSONObject[] { copy, r[1] };

			} catch (UnsupportedOperationException e) {
				throw new UnsupportedOperationException(field
						+ " is not a valid path", e);
			}
		}
		if (x == null) {
			BasicBSONObject copy = new BasicBSONObject();
			copy.putAll((BSONObject) data);
			BasicBSONObject vivi = new BasicBSONObject();
			copy.put(head, vivi);
			return new BasicBSONObject[] { copy, vivi };
		}
		throw new UnsupportedOperationException(field + " is not a valid path");
	}

	private BSONBackedObject _append(String key, Object value) {
		if (value == null)
			return unset(key);
		if (value instanceof List<?> && ((List<?>) value).isEmpty())
			return unset(key);
		if (value instanceof Object[] && ((Object[]) value).length == 0)
			return unset(key);

		BasicBSONObject[] path = createPath(bson, key);
		if (path == null)
			throw new UnsupportedOperationException(key
					+ " is not a valid path");

		int idx = key.lastIndexOf('.');
		String localKey = idx == -1 ? key : key.substring(idx + 1);
		path[1].put(localKey, value);
		return new BSONBackedObject(path[0], schema);
	}

	/**
	 * Since the object is immutable, all "append" methods return a modified
	 * copy that contains the new value.
	 * <p>
	 * Auto-vivification: In case of a nested field, non-existing objects on the
	 * path will be created if necessary.
	 */
	public BSONBackedObject append(String key, String value) {
		return _append(key, value);
	}

	/**
	 * Since the object is immutable, all "append" methods return a modified
	 * copy that contains the new value.
	 * <p>
	 * Auto-vivification: In case of a nested field, non-existing objects on the
	 * path will be created if necessary.
	 */
	public BSONBackedObject append(String key, BSONBackedObject value) {
		return _append(key, value.bson);
	}

	private List<Object> getList(String key) {
		Object o = drillDown(key);
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

	private BSONBackedObject _push(String key, Object value) {
		List<Object> l = getList(key);
		if (l == null) {
			createPath(bson, key);
			l = new ArrayList<Object>(1);
		}
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
	 * <p>
	 * Auto-vivification: In case of a nested field, non-existing objects on the
	 * path will be created if necessary.
	 */

	public BSONBackedObject push(String key, String value) {
		return _push(key, value);
	}

	/**
	 * Adds the value to the end of the array. A new array will be created if
	 * missing, and a previously single element will be turned into an array
	 * (does not raise an error like MongoDB's $push operator would).
	 * <p>
	 * Since the object is immutable, all "push" methods return a modified copy
	 * that contains the new value.
	 * <p>
	 * Auto-vivification: In case of a nested field, non-existing objects on the
	 * path will be created if necessary.
	 */

	public BSONBackedObject push(String key, BSONBackedObject value) {
		return _push(key, value.bson);
	}

	private BSONBackedObject _pushAll(String key, Object... values) {
		List<Object> l = getList(key);
		if (l == null) {
			createPath(bson, key);
			l = new ArrayList<Object>(values.length);
		}
		for (Object o : values)
			l.add(o);
		return _append(key, l);

	}

	/**
	 * Adds the value to the end of the array. A new array will be created if
	 * missing, and a previously single element will be turned into an array
	 * (does not raise an error like MongoDB's $push operator would).
	 * <p>
	 * Since the object is immutable, all "push" methods return a modified copy
	 * that contains the new value.
	 * <p>
	 * Auto-vivification: In case of a nested field, non-existing objects on the
	 * path will be created if necessary.
	 */

	public BSONBackedObject pushAll(String key, String... values) {
		return _pushAll(key, (Object[]) values);
	}

	/**
	 * Adds the value to the end of the array. A new array will be created if
	 * missing, and a previously single element will be turned into an array
	 * (does not raise an error like MongoDB's $push operator would).
	 * <p>
	 * Since the object is immutable, all "push" methods return a modified copy
	 * that contains the new value.
	 * <p>
	 * Auto-vivification: In case of a nested field, non-existing objects on the
	 * path will be created if necessary.
	 */

	public BSONBackedObject pushAll(String key, BSONBackedObject... values) {
		Object[] bsons = new BSONObject[values.length];
		for (int i = 0; i < bsons.length; i++) {
			bsons[i] = values[i].bson;
		}
		return _pushAll(key, bsons);
	}

	/**
	 * Adds the value to the end of the array, but only if the entry is not
	 * already there. A new array will be created if missing, and a previously
	 * single element will be turned into an array (does not raise an error like
	 * MongoDB's $addToSet operator would).
	 * <p>
	 * Since the object is immutable, all "addToSet" methods return a modified
	 * copy that contains the new value.
	 * <p>
	 * Auto-vivification: In case of a nested field, non-existing objects on the
	 * path will be created if necessary.
	 */

	public BSONBackedObject addToSet(String key, String value) {
		List<Object> l = getList(key);
		if (l.contains(value))
			return this;
		l.add(value);
		return _append(key, l);
	}

	/**
	 * Adds the value to the end of the array, but only if the entry is not
	 * already there. A new array will be created if missing, and a previously
	 * single element will be turned into an array (does not raise an error like
	 * MongoDB's $addToSet operator would).
	 * <p>
	 * Since the object is immutable, all "addToSet" methods return a modified
	 * copy that contains the new value.
	 * <p>
	 * Auto-vivification: In case of a nested field, non-existing objects on the
	 * path will be created if necessary.
	 */

	public BSONBackedObject addToSet(String key, BSONBackedObject value) {
		List<Object> l = getList(key);
		if (l.contains(value.bson))
			return this;
		l.add(value.bson);
		return _append(key, l);
	}

	/**
	 * Removes the last element in an array. If the field is not an array,
	 * deletes the field ("removes the only element"). If the array is left
	 * empty, removes the field completely. If the field is missing, does
	 * nothing.
	 * <p>
	 * Since the object is immutable, all "modifier" methods return the modified
	 * copy.
	 * 
	 * @param keys
	 *            you can specify multiple field names at once
	 */
	public BSONBackedObject popLast(String... keys) {
		if (keys == null || keys.length == 0)
			return this;

		// TODO: this recursion and its temporary copies are very inefficient
		// if we are supporting multiple keys, we might as well implement it
		// in a tighter fashion

		if (keys.length == 1) {
			String k = keys[0];
			List<Object> l = getList(k);
			if (l.isEmpty())
				return this;
			l.remove(l.size() - 1);
			return _append(k, l);
		}

		String[] head = Arrays.copyOf(keys, keys.length - 1);
		return popLast(keys[head.length]).popLast(head);
	}

	/**
	 * Deletes the gives fields (if they exist).
	 * <p>
	 * Since the object is immutable, all "modifier" methods return the modified
	 * copy.
	 * 
	 * @param keys
	 *            you can specify multiple field names at once
	 */
	public BSONBackedObject unset(String... keys) {
		if (keys == null || keys.length == 0)
			return this;

		// TODO: this recursion and its temporary copies are very inefficient
		// if we are supporting multiple keys, we might as well implement it
		// in a tighter fashion

		if (keys.length == 1) {
			String field = keys[0];
			Object[] d = drillDownToParent(bson, field);
			if (d == null)
				return this;

			String localKey = (String) d[1];

			BasicBSONObject[] path = createPath(bson, field);
			if (path == null)
				throw new UnsupportedOperationException(field
						+ " is not a valid path");

			path[1].removeField(localKey);
			return new BSONBackedObject(path[0], schema);
		}

		String[] head = Arrays.copyOf(keys, keys.length - 1);
		return unset(keys[head.length]).unset(head);

	}

	private final static BSONBackedObject BUILDER_START = new BSONBackedObject();

	public static final BSONBackedObject start() {
		return BUILDER_START;
	}

}
