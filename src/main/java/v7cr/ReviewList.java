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

import java.util.Locale;

import org.bson.types.ObjectId;

import v7cr.v7db.BSONBackedObject;
import v7cr.v7db.LocalizedString;
import v7cr.v7db.SchemaDefinition;
import v7cr.vaadin.DBCollectionContainer;
import v7cr.vaadin.PossibleValuesColumnGenerator;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.TabSheet.Tab;

@SuppressWarnings("serial")
class ReviewList extends CustomComponent implements ItemClickListener,
		ValueChangeListener {

	private final String projectName;

	private final Table table;

	private transient Object filterStatus;

	ReviewList(String projectName) {
		setIcon(new ThemeResource("../runo/icons/16/note.png"));

		VerticalLayout vl = new VerticalLayout();
		vl.setSizeFull();
		ComboBox c = new ComboBox();
		V7CR v7 = V7CR.getInstance();
		Locale l = v7.getLocale();
		SchemaDefinition sd = new Review("x", "x").getSchemaDefinition()
				.getFieldDefinition("s");
		for (Object x : sd.getPossibleValues()) {
			c.addItem(x);
			c.setItemCaption(x, LocalizedString.get(sd
					.getPossibleValueMetaData(x), "caption", l));
		}
		c.setImmediate(true);
		c.addListener(this);
		vl.addComponent(c);

		this.projectName = projectName;
		BSONBackedObject p = v7.load("projects", projectName);
		setCaption(p.getStringField("name"));
		table = new Table();
		table.addGeneratedColumn("status", new PossibleValuesColumnGenerator(
				Review.getReviewSchemaDefinition(), "s", l));

		vl.addComponent(table);
		setCompositionRoot(vl);
		reload();
		table.addListener((ItemClickListener) this);

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
	}

	public void reload() {
		DBCollection coll = V7CR.getInstance().getDBCollection("reviews");

		BasicDBObject filter = new BasicDBObject("p", projectName);
		if (filterStatus != null)
			filter.append("s", filterStatus);

		SchemaDefinition sd = Review.getReviewSchemaDefinition();

		DBCollectionContainer reviews = new DBCollectionContainer(sd, coll,
				filter, "c", false);

		// BeanContainer<ObjectId, Review> reviews = new BeanContainer<ObjectId,
		// Review>(
		// Review.class);
		// reviews.setBeanIdProperty("id");
		// for (BSONObject o :
		// V7CR.getInstance().getDBCollection("reviews").find(
		// new BasicDBObject("p", this.projectName)).sort(
		// new BasicDBObject("c", -1))) {
		// Review r = new Review(BSONBackedObjectLoader.wrap(o, null));
		// reviews.addBean(r);
		// }

		table.setContainerDataSource(reviews);

		reviews.addContainerProperty("reviewee.n", String.class, null);
		reviews.addContainerProperty("svn.rev", String.class, null);

		Locale l = V7CR.getInstance().getLocale();

		// table.setVisibleColumns(new String[] { "status", "reviewee.name",
		// "registrationDate", "title", "SVNLogEntry.revision" });

		table.setVisibleColumns(new String[] { "status", "reviewee.n", "c",
				"t", "svn.rev" });

		table.setColumnHeaders(new String[] { sd.getFieldCaption("s", l),
				sd.getFieldCaption("reviewee", l), sd.getFieldCaption("c", l),
				sd.getFieldCaption("t", l),
				sd.getFieldCaption("svn.fields.rev", l) });

		table.setSortDisabled(true);

	}

	public void valueChange(ValueChangeEvent event) {
		filterStatus = event.getProperty().getValue();
		reload();
	}
}
