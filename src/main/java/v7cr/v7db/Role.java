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
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import v7cr.Review;

public class Role extends BSONBackedObject {

	private static final SchemaDefinition schema;

	static {
		try {
			schema = new SchemaDefinition(BSONBackedObjectLoader.parse(IOUtils
					.toString(Review.class.getResourceAsStream("role.json"),
							"UTF-8"), null));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Role(BSONBackedObject b) {
		super(b, schema);
	}

	Role(BSONObject b) {
		super((BasicBSONObject) b, schema);
	}

	public String getId() {
		return getStringField("_id");
	}

	public LocalizedString getName() {
		return LocalizedString.get(this, "name");
	}

	public AccountInfo getAccountInfo() {
		return new AccountInfo(getId(), getName().toString());
	}

	public Map<String, AccountInfo> getMembers() {
		BSONBackedObject[] members = getObjectFieldAsArray("member");
		if (members == null)
			return Collections.emptyMap();
		Map<String, AccountInfo> users = new HashMap<String, AccountInfo>();
		for (BSONBackedObject m : members) {
			AccountInfo a = new AccountInfo(m);
			users.put(a.getId(), a);
		}
		return users;
	}

	public Role addMember(AccountInfo member) {
		BSONBackedObject a = addToSet("member", member);
		return new Role(a);
	}

	public static SchemaDefinition getRoleSchemaDefinition() {
		return schema;
	}

}
