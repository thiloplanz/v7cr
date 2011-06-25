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

import java.net.UnknownHostException;
import java.util.NoSuchElementException;

import org.apache.commons.lang.StringUtils;
import org.bson.BSONObject;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNRevision;

import v7cr.v7db.AccountInfo;
import v7cr.v7db.BSONBackedObjectLoader;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

public class svntest {

	/**
	 * @param args
	 * @throws SVNException
	 * @throws MongoException
	 * @throws UnknownHostException
	 */
	public static void main(String[] args) throws SVNException,
			UnknownHostException, MongoException {

		DAVRepositoryFactory.setup();

		DB db = new Mongo().getDB("v7cr");

		final Project project = new Project(BSONBackedObjectLoader.wrap(db
				.getCollection("projects").findOne("sanction-route"), null));

		final DBCollection reviews = db.getCollection("reviews");

		Long latestRev;
		try {
			BSONObject latest = (BSONObject) reviews.find(
					new BasicDBObject("p", project.getId())).sort(
					new BasicDBObject("svn.rev", -1)).limit(1).next()
					.get("svn");
			latestRev = (Long) latest.get("rev");

		} catch (NoSuchElementException e) {
			latestRev = 0l;
		}
		System.out.println(latestRev);

		SVNURL url = SVNURL.parseURIDecoded(project.getRepositoryUrl());
		SVNClientManager svn = SVNClientManager.newInstance();
		// svn.setAuthenticationManager(new
		// BasicAuthenticationManager("username","password"));

		SVNLogClient log = svn.getLogClient();

		log.doLog(url, null, SVNRevision.HEAD, SVNRevision
				.create(latestRev + 1), SVNRevision.HEAD, true, true, 100,
				new ISVNLogEntryHandler() {

					public void handleLogEntry(SVNLogEntry logEntry)
							throws SVNException {
						String message = logEntry.getMessage();
						String title = message;
						if (title.contains("\n")) {
							title = title.substring(0, title.indexOf('\n'));
						}
						if (title.length() > 80) {
							title = StringUtils.abbreviate(title, 80);
						}
						Review r = new Review(project.getId(), title);
						BasicDBObject b = new BasicDBObject(r.getBSONObject());
						BSONObject svn = Review.toBSON(logEntry);
						b.put("reviewee", new AccountInfo(logEntry.getAuthor(),
								logEntry.getAuthor()).getBSONObject());
						b.put("svn", svn);
						b.put("c", logEntry.getDate());
						System.out.println(b);
						reviews.insert(b);
					}
				});

		svn.dispose();

	}
}
