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

import com.mongodb.gridfs.GridFSFile;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

class TemporaryFile extends CustomComponent implements ClickListener {

	TemporaryFile(V7CR v7, GridFSFile file) {
		this.file = file;
		HorizontalLayout hl = new HorizontalLayout();
		setCompositionRoot(hl);
		hl.addComponent(new Label(file.getFilename()));
		hl.addComponent(new Button(v7.getMessage("button.delete"), this));
	}

	final GridFSFile file;

	public void buttonClick(ClickEvent event) {
		((ComponentContainer) getParent()).removeComponent(this);
	}

}
