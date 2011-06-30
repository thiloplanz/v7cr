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
import java.util.Date;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;

import v7cr.v7db.Versioning;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

public class notestest {

	public static void main(String[] argv) throws UnknownHostException,
			MongoException {

		DB db = new Mongo().getDB("v7cr");
		DBCollection review = db.getCollection("reviews");
		DBObject r = review.findOne(new ObjectId("4dfeca8eae37cf6da8d7e41e"));
		BSONObject findbugs = new BasicBSONObject("warnings", new String[] {
				"aaa", "bbb" });
		BSONObject notes = new BasicBSONObject("t", "Findbugs warnings")
				.append("v", "-").append("c", "Found 3 warnings").append("d",
						new Date()).append("x", findbugs);
		r.put("notes", new Object[] { notes });
		System.out.println(r);
		Versioning.update(review, Versioning.getVersion(r), r);
	}

}