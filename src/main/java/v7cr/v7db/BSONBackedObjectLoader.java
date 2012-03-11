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

import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.mongodb.DBCollection;
import com.mongodb.util.JSON;

/**
 * Collection of static helper methods to obtain BSONObjects from various
 * sources and wrap them into immutable BSONBackedObjects.
 * 
 * <p>
 * The reason for having this class is that if we control the creation of the
 * BSONObjects ourselves, we can prevent references from leaking and thus
 * guarantee immutability while still avoiding having to clone the BSONObjects.
 * 
 */

public class BSONBackedObjectLoader {

	/**
	 * wraps around a given BSONObject, but makes a defensive copy first
	 */
	public static BSONBackedObject wrap(BSONObject o, SchemaDefinition schema) {
		// TODO: cheaper cloning in Java-land
		return decode(BSON.encode(o), schema);
	}

	public static BSONBackedObject decode(byte[] bson, SchemaDefinition schema) {
		return new BSONBackedObject((BasicBSONObject) BSON.decode(bson), schema);
	}

	public static BSONBackedObject parse(String json, SchemaDefinition schema) {
		return new BSONBackedObject((BasicBSONObject) JSON.parse(json), schema);
	}

	public static BSONBackedObject findOne(DBCollection collection, Object id,
			SchemaDefinition schema) {
		BasicBSONObject o = (BasicBSONObject) collection.findOne(id);
		if (o == null)
			return null;
		return new BSONBackedObject(o, schema);
	}

}
