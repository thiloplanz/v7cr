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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import v7cr.v7db.AccountInfo;
import v7cr.v7db.BSONBackedObject;
import v7cr.v7db.BSONBackedObjectLoader;
import v7cr.v7db.Role;
import v7cr.v7db.SessionInfo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.HttpServletRequestListener;
import com.vaadin.terminal.gwt.server.WebApplicationContext;

/**
 * The Application's "main" class
 */
@SuppressWarnings("serial")
public class V7CR extends Application implements HttpServletRequestListener {

	private static ThreadLocal<V7CR> currentApplication = new ThreadLocal<V7CR>();

	private transient SessionInfo sessionInfo;

	@Override
	public void init() {
		setMainWindow(new TopPageWindow(this));
	}

	public void onRequestEnd(HttpServletRequest request,
			HttpServletResponse response) {
		currentApplication.remove();
		sessionInfo = null;

	}

	public void onRequestStart(HttpServletRequest request,
			HttpServletResponse response) {
		sessionInfo = (SessionInfo) request.getSession().getAttribute(
				"v7cr.sessionInfo");
		currentApplication.set(this);

	}

	static V7CR getInstance() {
		return currentApplication.get();
	}

	AccountInfo getSessionUser() {
		return sessionInfo.accountInfo;
	}

	Map<String, Role> getRoles() {
		return sessionInfo.roles;
	}

	DBCollection getDBCollection(String name) {
		WebApplicationContext context = (WebApplicationContext) getContext();
		return InitDB.getDBCollection(context.getHttpSession()
				.getServletContext(), name);
	}

	BSONBackedObject load(String collection, Object id) {
		return BSONBackedObjectLoader.findOne(getDBCollection(collection), id,
				null);
	}

	void save(String collection, BSONBackedObject object) {
		getDBCollection(collection).save(
				new BasicDBObject(object.getBSONObject()));
	}

}
