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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bson.BSONObject;

import v7cr.v7db.AccountInfo;
import v7cr.v7db.BSONBackedObject;
import v7cr.v7db.Role;
import v7cr.v7db.Roles;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.TwinColSelect;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Table.TableDragMode;

@SuppressWarnings("serial")
class RoleEditor extends CustomComponent implements ItemClickListener,
		ClickListener {

	private final Table rolesTab = new Table();

	private final TwinColSelect memberSelect = new TwinColSelect();

	private final BeanContainer<String, Role> roles = new BeanContainer<String, Role>(
			Role.class);

	RoleEditor(V7CR v7) {
		setCaption("Manage Roles");
		setIcon(new ThemeResource("../runo/icons/16/users.png"));

		this.roles.setBeanIdProperty("id");
		for (BSONBackedObject b : v7.find("roles")) {
			roles.addBean(new Role(b));
		}

		rolesTab.setDragMode(TableDragMode.ROW);
		rolesTab.setContainerDataSource(this.roles);
		rolesTab.setVisibleColumns(new Object[] { "id", "name" });
		rolesTab.setSelectable(true);
		rolesTab.setImmediate(true);
		rolesTab.addListener(this);

		Panel rightSide = new Panel();

		memberSelect.setRightColumnCaption("Members");
		memberSelect.setLeftColumnCaption("Non-members");
		rightSide.addComponent(memberSelect);

		Button commitButton = new Button("update");
		commitButton.addListener(this);
		rightSide.addComponent(commitButton);

		rightSide.setWidth("500");

		HorizontalLayout hl = new HorizontalLayout();
		hl.addComponent(rolesTab);
		hl.addComponent(rightSide);

		setCompositionRoot(hl);
	}

	public void itemClick(ItemClickEvent event) {

		String roleId = (String) event.getItemId();

		Role r = Roles.load(((V7CR) getApplication()).getDBCollection("roles"),
				roleId);
		if (r == null)
			return;

		BeanItem<Role> role = new BeanItem<Role>(r);
		BeanContainer<String, AccountInfo> members = new BeanContainer<String, AccountInfo>(
				AccountInfo.class);
		members.setBeanIdProperty("id");

		members.addAll(r.getMembers().values());

		for (String rid : roles.getItemIds()) {
			Role rr = roles.getItem(rid).getBean();
			members.addAll(rr.getMembers().values());
			members.addBean(rr.getAccountInfo());
		}
		memberSelect.setContainerDataSource(members);
		for (String rid : role.getBean().getMembers().keySet()) {
			memberSelect.select(rid);
		}

	}

	public void buttonClick(ClickEvent event) {
		String roleId = (String) rolesTab.getValue();
		BeanItem<Role> role = roles.getItem(roleId);
		if (role == null)
			return;

		DBCollection db = V7CR.getInstance().getDBCollection("roles");
		Role r = Roles.load(db, roleId);

		Collection<?> selected = (Collection<?>) memberSelect.getValue();
		DBObject o = new BasicDBObject(r.getBSONObject());
		List<BSONObject> members = new ArrayList<BSONObject>();
		for (Object s : selected) {
			BeanItem<AccountInfo> biai = (BeanItem<AccountInfo>) memberSelect
					.getContainerDataSource().getItem(s);
			members.add(biai.getBean().getBSONObject());
		}
		o.put("member", members);
		db.save(o);

	}

}
