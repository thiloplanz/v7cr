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

import v7cr.v7db.BSONBackedObject;
import v7cr.vaadin.ItemFactory;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Form;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;

@SuppressWarnings("serial")
class ProjectEditor extends CustomComponent implements ItemClickListener,
		Button.ClickListener {

	private final Table projTable = new Table();

	private final Panel rightSide = new Panel();

	ProjectEditor(V7CR v7) {
		setCaption("Manage Projects");
		setIcon(new ThemeResource("../runo/icons/16/settings.png"));

		reload(v7);
		projTable.setSelectable(true);
		projTable.addListener(this);

		rightSide.setWidth("500");

		HorizontalLayout hl = new HorizontalLayout();
		hl.setWidth("100%");
		VerticalLayout leftSide = new VerticalLayout();
		leftSide.addComponent(projTable);
		Button add = new Button("add new project");
		add.addListener(this);
		leftSide.addComponent(add);
		hl.addComponent(leftSide);
		hl.addComponent(rightSide);

		setCompositionRoot(hl);

	}

	private void reload(V7CR v7) {
		BeanContainer<String, Project> beans = new BeanContainer<String, Project>(
				Project.class);
		beans.setBeanIdProperty("id");

		for (BSONBackedObject b : v7.find("projects")) {
			beans.addBean(new Project(b));
		}

		projTable.setContainerDataSource(beans);
		projTable.setVisibleColumns(new Object[] { "id", "name" });

	}

	public void itemClick(ItemClickEvent event) {
		String projectId = (String) event.getItemId();
		if (projectId == null)
			return;

		final Project p = new Project(V7CR.getInstance().load("projects",
				projectId));

		rightSide.removeAllComponents();
		rightSide.addComponent(new Label(projectId));

		final Form form = new Form();
		form.setWidth("100%");
		form.addField("name", ItemFactory.getTextField(p, "name"));
		form.addField("repo", ItemFactory.getTextField(p, "repo"));
		form
				.addField("viewChanges", ItemFactory.getTextField(p,
						"viewChanges"));

		rightSide.addComponent(form);

		Button submit = new Button("submit");
		rightSide.addComponent(submit);
		submit.addListener(new Button.ClickListener() {

			public void buttonClick(ClickEvent event) {

				DBObject b = p.getDBObject();
				b.put("name", form.getItemProperty("name").getValue());
				b.put("repo", form.getItemProperty("repo").getValue());
				b.put("viewChanges", form.getItemProperty("viewChanges")
						.getValue());

				V7CR v7 = V7CR.getInstance();
				v7.save("projects", b);
				rightSide.removeAllComponents();
				reload(v7);

			}
		});

	}

	public void buttonClick(ClickEvent event) {
		rightSide.removeAllComponents();
		Project p = new Project(new BSONBackedObject());
		final Form form = new Form();
		form.setWidth("100%");

		form.addField("id", new TextField("Project Id"));
		form.addField("name", ItemFactory.getTextField(p, "name"));
		form.addField("repo", ItemFactory.getTextField(p, "repo"));
		form
				.addField("viewChanges", ItemFactory.getTextField(p,
						"viewChanges"));

		rightSide.addComponent(form);

		Button submit = new Button("create");
		rightSide.addComponent(submit);

		submit.addListener(new Button.ClickListener() {

			public void buttonClick(ClickEvent event) {

				DBObject b = new BasicDBObjectBuilder().add("_id",
						form.getItemProperty("id").getValue()).add("name",
						form.getItemProperty("name").getValue()).add("repo",
						form.getItemProperty("repo").getValue()).add(
						"viewChanges",
						form.getItemProperty("viewChanges").getValue()).get();

				V7CR v7 = V7CR.getInstance();
				v7.save("projects", b);
				// also create the member role
				DBObject r = new BasicDBObjectBuilder().add("_id",
						"project:" + b.get("_id")).add("name",
						"Reviewers of project '" + b.get("_id") + "'").get();

				v7.save("roles", r);
				rightSide.removeAllComponents();
				reload(v7);

			}
		});

	}
}
