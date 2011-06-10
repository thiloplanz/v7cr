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

import v7cr.v7db.Role;

import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.WebApplicationContext;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

@SuppressWarnings("serial")
class TopPageWindow extends Window {

	TopPageWindow(V7CR app) {
		super("v7 Code Review");
		setName("top");
		initUI(app);

	}

	private void initUI(V7CR app) {
		VerticalLayout vl = new VerticalLayout();
		vl.setSizeFull();
		vl.addComponent(createToolbar(app));
		TabSheet main = new TabSheet();
		vl.addComponent(main);
		vl.setExpandRatio(main, 1);

		Map<String, Role> roles = app.getRoles();
		if (roles.containsKey("admin")) {
			main.addTab(new RoleEditor(app));
			main.addTab(new UserEditor());
			main.addTab(new ProjectEditor(app));
		}
		for (String r : roles.keySet()) {
			if (r.startsWith("project:")) {
				String p = r.substring(8);
				main.addTab(new ReviewList(p));
			}
		}
		addComponent(vl);
	}

	private HorizontalLayout createToolbar(V7CR app) {
		Label logo = new Label("v7 Code Review");
		Label username = new Label(app.getSessionUser().getName() + " "
				+ app.getSessionUser().getId());
		Button logout = new Button("Logout");
		logout.addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				Application a = V7CR.getInstance();
				a.close();
				((WebApplicationContext) a.getContext()).getHttpSession()
						.invalidate();
			}
		});

		HorizontalLayout hl = new HorizontalLayout();
		hl.addComponent(logo);
		hl.addComponent(logout);
		hl.addComponent(username);
		hl.setSpacing(true);
		return hl;
	}
}
