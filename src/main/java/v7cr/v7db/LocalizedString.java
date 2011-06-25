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

import java.util.Locale;

import org.bson.BSONObject;

/**
 * A string intended for user interface elements. It can be localized for
 * different languages.
 * 
 * 
 */

public class LocalizedString {

	// TODO: require '_', allow other language fields
	private final static SchemaDefinition schema = SchemaDefinition
			.parse("{'dataType':['string', ':lstring'], 'types' : { 'lstring' : { 'fields' : { '_' : { 'dataType': 'string' } } }}}");

	private final String defaultString;

	private final BSONBackedObject bson;

	/**
	 * create a "localized" String that is actually just fixed (to the same
	 * String for all locales)
	 * 
	 * @param fixed
	 *            the single String to be used for all locales
	 */
	public LocalizedString(String fixed) {
		defaultString = fixed;
		bson = null;
	}

	public LocalizedString(BSONBackedObject bson) {
		defaultString = bson.getStringField("_");
		this.bson = bson;
	}

	/**
	 * 
	 * @param o
	 *            must be String, BSONObject or BSONBackedObject
	 */
	public LocalizedString(Object o) {
		if (o instanceof String) {
			defaultString = (String) o;
			bson = null;
		} else if (o instanceof BSONBackedObject) {
			bson = (BSONBackedObject) o;
			defaultString = bson.getStringField("_");
		} else if (o instanceof BSONObject) {
			bson = BSONBackedObjectLoader.wrap((BSONObject) o, null);
			defaultString = bson.getStringField("_");
		} else {
			throw new IllegalArgumentException(
					"LocalizedString can only be decoded from String, BSONObject, or BSONBackedObject, not "
							+ o.getClass().getName());
		}
	}

	/**
	 * regardless of the environment, always prints the fallback default string.
	 * If that is null for some reason (should not happen), returns an empty
	 * string.
	 */
	@Override
	public String toString() {
		return defaultString == null ? "" : defaultString;
	}

	/**
	 * @return the String defined for this locale, or a fallback String
	 */
	public String toString(Locale l) {
		if (bson == null)
			return toString();
		String d = bson.getStringField(l.getLanguage());
		if (d != null)
			return d;
		return toString();

	}

	public Object encodeToBSON() {
		return defaultString;
	}

	public SchemaDefinition getSchemaDefinition() {
		return schema;
	}

	public static LocalizedString get(BSONBackedObject o, String field) {
		if (o == null)
			return null;
		Object x = o.getField(field);
		if (x == null)
			return null;
		return new LocalizedString(x);
	}

	public static String get(BSONBackedObject o, String field, Locale l) {
		LocalizedString ls = get(o, field);
		if (ls == null)
			return null;
		return ls.toString(l);
	}

}
