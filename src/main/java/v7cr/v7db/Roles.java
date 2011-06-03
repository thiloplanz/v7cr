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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.BasicBSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class Roles {

	public static Map<String, Role> loadRoles(DBCollection collection,
			String userId) {
		Map<String, Role> allRoles = new HashMap<String, Role>();
		Set<Role> roles = new HashSet<Role>();

		for (DBObject o : collection.find(new BasicDBObject("member._id",
				userId))) {
			roles.add(new Role((BasicBSONObject) o));
		}

		while (!roles.isEmpty()) {
			Set<Role> newRoles = new HashSet<Role>();
			for (Role r : roles) {
				allRoles.put(r.getId(), r);
				for (Role n : loadRoles(collection, r.getId()).values()) {
					if (!allRoles.containsKey(n.getId())) {
						newRoles.add(n);
					}
				}
			}
			roles = newRoles;
		}

		return allRoles;
	}

	public static Role load(DBCollection collection, String roleId) {
		return new Role(BSONBackedObjectLoader
				.findOne(collection, roleId, null));
	}

}
