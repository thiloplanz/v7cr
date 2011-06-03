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

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class Role extends BSONBackedObject {

	public Role(BSONBackedObject b) {
		super(b, null);
	}

	Role(BSONObject b) {
		super((BasicBSONObject) b, null);
	}

	public String getId() {
		return getStringField("_id");
	}

	public String getName() {
		return getStringField("name");
	}

	public AccountInfo getAccountInfo() {
		return new AccountInfo(getId(), getName());
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

}
