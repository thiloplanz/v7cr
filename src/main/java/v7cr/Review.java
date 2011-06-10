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

import static org.tmatesoft.svn.core.SVNRevisionProperty.AUTHOR;
import static org.tmatesoft.svn.core.SVNRevisionProperty.DATE;
import static org.tmatesoft.svn.core.SVNRevisionProperty.LOG;

import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNProperties;

import v7cr.v7db.AccountInfo;
import v7cr.v7db.BSONBackedObject;
import v7cr.v7db.BSONBackedObjectLoader;
import v7cr.v7db.SchemaDefinition;

public class Review extends BSONBackedObject {

	private static final SchemaDefinition schema;

	static {
		try {
			schema = new SchemaDefinition(BSONBackedObjectLoader.parse(IOUtils
					.toString(Review.class.getResourceAsStream("review.json")),
					null));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Review(BSONBackedObject bson) {
		super(bson, schema);
	}

	public Review(String project, String title) {
		this(new BSONBackedObject().append("p", project).append("t", title)
				.append("s", "new"));
	}

	public String getProjectName() {
		return getStringField("p");
	}

	public String getStatus() {
		return getStringField("s");
	}

	public String getTitle() {
		return getStringField("t");
	}

	public AccountInfo getReviewee() {
		BSONBackedObject b = getObjectField("reviewee");
		return b == null ? null : new AccountInfo(b);
	}

	public SVNLogEntry getSVNLogEntry() {
		BSONBackedObject b = getObjectField("svn");
		if (b == null)
			return null;
		SVNProperties props = new SVNProperties();
		props.put(AUTHOR, b.getStringField(AUTHOR));
		props.put(DATE, b.getDateField(DATE).toString());
		props.put(LOG, b.getStringField(LOG));
		SVNLogEntry svn = new SVNLogEntry(null, b.getLongField("rev"), props,
				false);
		return svn;
	}

	public static BSONObject toBSON(SVNLogEntry logEntry) {
		BSONObject svn = new BasicBSONObject();
		svn.put("rev", logEntry.getRevision());
		svn.put(AUTHOR, logEntry.getAuthor());
		svn.put(DATE, logEntry.getDate());
		svn.put(LOG, logEntry.getMessage());
		return svn;
	}

	public ObjectId getId() {
		return getObjectIdField("_id");
	}

	public Date getRegistrationDate() {
		return getDateField("c");
	}

	public Review addVote(AccountInfo user, Date date, String comment,
			String vote) {
		BSONObject v = new BasicBSONObject("c", comment).append("d", date)
				.append("v", vote).append("by", user.getBSONObject());
		Review r = new Review(push("v", BSONBackedObjectLoader.wrap(v, null))
				.append("s", "review"));
		return r;
	}
}
