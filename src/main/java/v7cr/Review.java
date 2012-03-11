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

package v7cr;

import static org.tmatesoft.svn.core.SVNRevisionProperty.AUTHOR;
import static org.tmatesoft.svn.core.SVNRevisionProperty.DATE;
import static org.tmatesoft.svn.core.SVNRevisionProperty.LOG;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.internal.util.SVNDate;

import v7cr.v7db.AccountInfo;
import v7cr.v7db.BSONBackedObject;
import v7cr.v7db.BSONBackedObjectLoader;
import v7cr.v7db.SchemaDefinition;

import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;

public class Review extends BSONBackedObject {

	private static final SchemaDefinition schema;

	static {
		try {
			schema = new SchemaDefinition(BSONBackedObjectLoader.parse(IOUtils
					.toString(Review.class.getResourceAsStream("review.json"),
							"UTF-8"), null));
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
		props.put(DATE, SVNDate.formatDate(b.getDateField(DATE)));
		props.put(LOG, b.getStringField(LOG));
		Map<String, SVNLogEntryPath> changedPaths = null;
		BSONBackedObject[] changed = b.getObjectFieldAsArray("changed");
		if (changed != null) {
			changedPaths = new TreeMap<String, SVNLogEntryPath>();
			for (BSONBackedObject c : changed) {
				String p = c.getStringField("p");
				String t = c.getStringField("t");
				changedPaths.put(p, new SVNLogEntryPath(p, t.charAt(0), null,
						-1));
			}
		}

		SVNLogEntry svn = new SVNLogEntry(changedPaths, b.getLongField("rev"),
				props, false);
		return svn;
	}

	public static BSONObject toBSON(SVNLogEntry logEntry) {
		BSONObject svn = new BasicBSONObject();
		svn.put("rev", logEntry.getRevision());
		svn.put(AUTHOR, logEntry.getAuthor());
		svn.put(DATE, logEntry.getDate());
		svn.put(LOG, logEntry.getMessage());
		Map<String, SVNLogEntryPath> changedPaths = logEntry.getChangedPaths();
		if (changedPaths != null && !changedPaths.isEmpty()) {
			List<BSONObject> c = new ArrayList<BSONObject>(changedPaths.size());
			for (SVNLogEntryPath p : changedPaths.values()) {
				BSONObject cp = new BasicBSONObject();
				cp.put("p", p.getPath());
				cp.put("t", String.valueOf(p.getType()));
				if (p.getCopyPath() != null) {
					cp.put("cp", p.getCopyPath());
					cp.put("crev", p.getCopyRevision());
				}
				c.add(cp);
			}
			svn.put("changed", c);
		}
		return svn;
	}

	public ObjectId getId() {
		return getObjectIdField("_id");
	}

	public Date getRegistrationDate() {
		return getDateField("c");
	}

	public Review addVote(AccountInfo user, Date date, String comment,
			String vote, ComponentContainer files) {
		BSONObject v = new BasicBSONObject("c", comment).append("d", date)
				.append("v", vote).append("by", user.getBSONObject());
		if (files != null) {
			Iterator<Component> fi = files.getComponentIterator();
			List<BSONObject> fileData = new ArrayList<BSONObject>();
			while (fi.hasNext()) {
				TemporaryFile f = (TemporaryFile) fi.next();
				fileData.add(f.file.getBSONObject());
			}
			if (!fileData.isEmpty()) {
				v.put("files", fileData);
			}
		}
		Review r = new Review(push("v", BSONBackedObjectLoader.wrap(v, null)))
				.updateStatus();
		return r;
	}

	public Review deleteVote(BSONBackedObject vote) {
		BSONBackedObject[] votes = getObjectFieldAsArray("v");
		votes = (BSONBackedObject[]) ArrayUtils.removeElement(votes, vote);
		return new Review(unset("v").pushAll("v", votes)).updateStatus();
	}

	public Review updateVote(BSONBackedObject vote, String newMessage,
			String rating) {
		BSONBackedObject[] votes = getObjectFieldAsArray("v");
		int idx = ArrayUtils.indexOf(votes, vote);
		if (idx == -1)
			return this;
		votes[idx] = votes[idx].append("c", newMessage).append("v", rating);
		return new Review(unset("v").pushAll("v", votes)).updateStatus();
	}

	private String calculateStatus() {
		String status = "new";
		BSONBackedObject[] votes = getObjectFieldAsArray("v");
		if (votes == null || votes.length == 0)
			return status;
		status = "review";
		int good = 0;
		// TODO: very user gets to vote just once
		for (BSONBackedObject v : votes) {
			String vv = v.getStringField("v");
			if ("-".equals(vv))
				return "not good";
			if ("+".equals(vv))
				good++;
		}
		if (good >= 2)
			return "okay";
		return status;

	}

	Review updateStatus() {
		String s = calculateStatus();
		if (s.equals(getStatus()))
			return this;
		return new Review(append("s", s));
	}

	public static SchemaDefinition getReviewSchemaDefinition() {
		return schema;
	}
}
