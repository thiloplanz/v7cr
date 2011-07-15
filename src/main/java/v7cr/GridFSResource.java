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

import org.bson.types.ObjectId;

import com.mongodb.gridfs.GridFSDBFile;
import com.vaadin.Application;
import com.vaadin.service.FileTypeResolver;
import com.vaadin.terminal.ApplicationResource;
import com.vaadin.terminal.DownloadStream;

class GridFSResource implements ApplicationResource {

	private final Application app;

	private final String filename;

	private final ObjectId fileId;

	public Application getApplication() {
		return app;
	}

	public int getBufferSize() {
		return 0;
	}

	public long getCacheTime() {
		return 0;
	}

	public String getFilename() {
		return filename;
	}

	public DownloadStream getStream() {
		GridFSDBFile file = V7CR.getInstance().getFile(fileId);
		if (file == null) {
			return null;
		}
		final DownloadStream ds = new DownloadStream(file.getInputStream(),
				getMIMEType(), getFilename());
		ds.setBufferSize(getBufferSize());
		ds.setCacheTime(getCacheTime());
		return ds;
	}

	public String getMIMEType() {
		return FileTypeResolver.getMIMEType(filename);
	}

	GridFSResource(Application app, ObjectId fileId, String filename) {
		this.app = app;
		this.filename = filename;
		this.fileId = fileId;
		app.addResource(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof GridFSResource))
			return false;
		return ((GridFSResource) obj).fileId.equals(fileId);
	}

	@Override
	public int hashCode() {
		return fileId.hashCode();
	}
}
