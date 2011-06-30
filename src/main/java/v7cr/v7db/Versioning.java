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

import java.util.ConcurrentModificationException;

import org.bson.BSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class Versioning {

	/**
	 * The field name for the version number
	 */
	public static final String VERSION = "_version";

	/**
	 * inserts a first version of an object.
	 * 
	 * It must not already have a _version field. _version will be set to 1.
	 * 
	 */
	public static WriteResult insert(DBCollection collection, DBObject object) {
		if (object.containsField(VERSION)) {
			throw new IllegalArgumentException(
					"The object already has a _version " + object);
		}
		object.put(VERSION, 1);
		return collection.insert(object);
	}

	/**
	 * updates an object, but only if the base version has not been changed in
	 * the meantime. The object must have an _id field. The _version field if
	 * present is ignored, and set to baseVersion+1.
	 */

	public static WriteResult update(DBCollection collection,
			final int baseVersion, DBObject object) {
		object.put(VERSION, baseVersion + 1);
		Object id = object.get("_id");
		WriteResult result = collection.update(new BasicDBObject("_id", id)
				.append(VERSION, baseVersion), object);
		if (result.getN() != 1)
			throw new ConcurrentModificationException("baseVersion has changed");
		return result;
	}

	public static Integer getVersion(BSONBackedObject b) {
		return b.getIntegerField(VERSION);
	}

	public static Integer getVersion(BSONObject b) {
		return (Integer) b.get(VERSION);
	}
}
