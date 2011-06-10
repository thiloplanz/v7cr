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

import org.bson.BSONObject;
import org.bson.types.ObjectId;

import v7cr.v7db.BSONBackedObjectLoader;

import com.mongodb.BasicDBObject;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Table;
import com.vaadin.ui.TabSheet.Tab;

@SuppressWarnings("serial")
class ReviewList extends CustomComponent implements ItemClickListener {

	private final String projectName;

	private final Table table;

	ReviewList(String projectName) {
		setIcon(new ThemeResource("../runo/icons/16/note.png"));

		this.projectName = projectName;
		setCaption(projectName);
		table = new Table();
		setCompositionRoot(table);
		reload();
		table.addListener(this);

	}

	public void itemClick(ItemClickEvent event) {
		TabSheet tabs = (TabSheet) getParent();
		Object iid = event.getItemId();
		if (iid instanceof ObjectId) {
			// find existing tab
			int count = tabs.getComponentCount();
			for (int i = 0; i < count; i++) {
				Component x = tabs.getTab(i).getComponent();
				if (x instanceof ReviewTab
						&& ((ReviewTab) x).reviewId.equals(iid)) {
					tabs.setSelectedTab(x);
					return;
				}
			}
			Tab t = tabs.addTab(new ReviewTab((ObjectId) iid));
			t.setClosable(true);

			tabs.setSelectedTab(t.getComponent());
		}
		reload();
	}

	public void reload() {
		BeanContainer<ObjectId, Review> reviews = new BeanContainer<ObjectId, Review>(
				Review.class);
		reviews.setBeanIdProperty("id");
		for (BSONObject o : V7CR.getInstance().getDBCollection("reviews").find(
				new BasicDBObject("p", this.projectName)).sort(
				new BasicDBObject("c", -1))) {
			Review r = new Review(BSONBackedObjectLoader.wrap(o, null));
			reviews.addBean(r);
		}
		table.setContainerDataSource(reviews);
		reviews.addNestedContainerProperty("reviewee.name");
		reviews.addNestedContainerProperty("SVNLogEntry.revision");

		table.setVisibleColumns(new String[] { "status", "reviewee.name",
				"registrationDate", "title", "SVNLogEntry.revision" });
		table.setSortDisabled(true);

	}
}
