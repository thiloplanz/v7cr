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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.bson.types.ObjectId;

import v7cr.v7db.AccountInfo;
import v7cr.v7db.BSONBackedObject;
import v7cr.v7db.BSONBackedObjectLoader;
import v7cr.v7db.Role;
import v7cr.v7db.SessionInfo;
import v7cr.v7db.Versioning;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import com.vaadin.Application;
import com.vaadin.terminal.ExternalResource;
import com.vaadin.terminal.ParameterHandler;
import com.vaadin.terminal.StreamResource;
import com.vaadin.terminal.Terminal;
import com.vaadin.terminal.URIHandler;
import com.vaadin.terminal.VariableOwner;
import com.vaadin.terminal.StreamResource.StreamSource;
import com.vaadin.terminal.gwt.server.ChangeVariablesErrorEvent;
import com.vaadin.terminal.gwt.server.HttpServletRequestListener;
import com.vaadin.terminal.gwt.server.WebApplicationContext;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Link;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.Notification;

/**
 * The Application's "main" class
 */
@SuppressWarnings("serial")
public class V7CR extends Application implements HttpServletRequestListener {

	private static ThreadLocal<V7CR> currentApplication = new ThreadLocal<V7CR>();

	private transient SessionInfo sessionInfo;

	private transient ResourceBundle messages;

	@Override
	public void init() {
		setLocale(Locale.JAPANESE);
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

	Object findId(String collection, DBObject criteria) {
		DBObject o = getDBCollection(collection).findOne(criteria,
				new BasicDBObject());
		if (o == null)
			return null;
		return o.get("_id");
	}

	List<BSONBackedObject> find(String collection) {
		List<BSONBackedObject> result = new ArrayList<BSONBackedObject>();
		for (DBObject d : getDBCollection(collection).find()) {
			result.add(BSONBackedObjectLoader.wrap(d, null));
		}
		return result;
	}

	WriteResult update(String collection, BSONBackedObject object) {
		return Versioning.update(getDBCollection(collection), Versioning
				.getVersion(object), object.getDBObject());
	}

	WriteResult update(String collection, DBObject object) {
		return Versioning.update(getDBCollection(collection), Versioning
				.getVersion(object), object);
	}

	WriteResult insert(String collection, BSONBackedObject object) {
		return Versioning.insert(getDBCollection(collection), object
				.getDBObject());
	}

	WriteResult insert(String collection, DBObject object) {
		return Versioning.insert(getDBCollection(collection), object);
	}

	BSONBackedObject storeFile(File file, String fileName, String mimeType)
			throws IOException {
		HttpClient http = new DefaultHttpClient();
		HttpPost post = new HttpPost("http://0.0.0.0:8088/upload/v7cr");
		FileBody body = new FileBody(file, fileName, mimeType, null);
		MultipartEntity multipart = new MultipartEntity();
		multipart.addPart("file", body);
		post.setEntity(multipart);
		HttpResponse response = http.execute(post);
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new IOException("failed to save " + fileName + ": "
					+ response.getStatusLine());
		}

		byte[] bson = IOUtils.toByteArray(response.getEntity().getContent());
		return BSONBackedObjectLoader.decode(bson, null);
	}

	Link getFile(BSONBackedObject file) {
		String fn = file.getStringField("files.file.filename");
		if (fn == null)
			return null;
		final byte[] inlineData = (byte[]) file.getField("files.file.in");
		if (inlineData != null)
			return new Link(fn, new StreamResource(new StreamSource() {

				public InputStream getStream() {
					return new ByteArrayInputStream(inlineData);
				}
			}, fn, this));

		try {
			fn = URLEncoder.encode(fn, "UTF-8");
		} catch (Exception e) {
			fn = "";
		}
		URL url = getURL();
		return new Link(fn,
				new ExternalResource(url.getProtocol()
						+ "://"
						+ url.getHost()
						+ ":8088/upload/v7cr?filename="
						+ fn
						+ "&sha="
						+ Hex.encodeHexString((byte[]) file
								.getField("files.file.sha"))));
	}

	@Override
	public Window getWindow(String name) {
		Window x = super.getWindow(name);
		if (x != null)
			return x;

		if (name.contains("-")) {
			String[] pjt_rev = StringUtils.split(name, '-');
			// project name could contain spaces
			if (pjt_rev.length > 2) {
				pjt_rev = new String[] {
						StringUtils.join(pjt_rev, '-', 0, pjt_rev.length - 1),
						pjt_rev[pjt_rev.length - 1] };
			}
			// check permission to access the project
			if (!getRoles().containsKey("project:" + pjt_rev[0])) {
				throw new SecurityException(
						"permission denied to access project " + pjt_rev[0]);
			}

			Object reviewId = findId("reviews", new BasicDBObject("p",
					pjt_rev[0]).append("svn.rev", Long.parseLong(pjt_rev[1])));
			if (reviewId instanceof ObjectId) {
				Window reviewWindow = new Window(name);
				reviewWindow.addComponent(new ReviewTab((ObjectId) reviewId));
				addWindow(reviewWindow);
				return reviewWindow;
			}
			throw new IllegalArgumentException(name);
		}

		return null;
	}

	String getMessage(String key) {
		if (messages == null)
			messages = ResourceBundle.getBundle("v7cr.messages", getLocale());
		return messages.getString(key);
	}

	@Override
	public void terminalError(Terminal.ErrorEvent event) {
		if (event.getThrowable().getCause() instanceof ConcurrentModificationException) {
			// Finds the original source of the error/exception
			Object owner = null;
			if (event instanceof VariableOwner.ErrorEvent) {
				owner = ((VariableOwner.ErrorEvent) event).getVariableOwner();
			} else if (event instanceof URIHandler.ErrorEvent) {
				owner = ((URIHandler.ErrorEvent) event).getURIHandler();
			} else if (event instanceof ParameterHandler.ErrorEvent) {
				owner = ((ParameterHandler.ErrorEvent) event)
						.getParameterHandler();
			} else if (event instanceof ChangeVariablesErrorEvent) {
				owner = ((ChangeVariablesErrorEvent) event).getComponent();
			}

			// Shows the error in AbstractComponent
			if (owner instanceof AbstractComponent) {
				((AbstractComponent) owner).getWindow().showNotification(
						getMessage("error.concurrentModification"),
						getMessage("error.concurrentModification.message"),
						Notification.TYPE_ERROR_MESSAGE);
				return;
			}

		}

		// Call the default implementation.
		super.terminalError(event);

	}
}
