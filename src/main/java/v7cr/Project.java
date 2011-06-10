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

package v7cr;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import v7cr.v7db.BSONBackedObject;
import v7cr.v7db.BSONBackedObjectLoader;
import v7cr.v7db.SchemaDefinition;

public class Project extends BSONBackedObject {

	private static final SchemaDefinition schema;

	static {
		try {
			schema = new SchemaDefinition(BSONBackedObjectLoader.parse(
					IOUtils.toString(Review.class
							.getResourceAsStream("project.json")), null));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Project(BSONBackedObject bson) {
		super(bson, schema);
	}

	public String getId() {
		return getStringField("_id");
	}

	public String getName() {
		return getStringField("name");
	}

	public String getRepositoryUrl() {
		return getStringField("repo");
	}

	public String getChangesetViewUrl(long svnRev) {
		return StringUtils.replace(getStringField("viewChanges"), "${svn.rev}",
				String.valueOf(svnRev));
	}

}
