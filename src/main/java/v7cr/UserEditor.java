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

import v7cr.v7db.AccountInfo;
import v7cr.v7db.Role;

import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.validator.EmailValidator;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Form;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

class UserEditor extends CustomComponent implements ClickListener {

	private final Form userForm = new Form();

	UserEditor() {
		setCaption("Register Users");
		setIcon(new ThemeResource("../runo/icons/16/user.png"));
		HorizontalLayout hl = new HorizontalLayout();
		setCompositionRoot(hl);

		userForm.setCaption("Register New User");
		userForm
				.setDescription("You can add a new user by entering an email address. Upon registration, she will be given the Connect role automatically.");
		TextField id = new TextField("Email address");
		id.setRequired(true);
		id.addValidator(new EmailValidator("invalid email address"));
		userForm.addField("id", id);
		TextField name = new TextField("Display name");
		name.setRequired(true);
		userForm.addField("name", name);

		userForm.getLayout().addComponent(new Button("Register", this));
		hl.addComponent(userForm);

	}

	public void buttonClick(ClickEvent event) {
		try {
			userForm.commit();
			String id = userForm.getField("id").getValue().toString();
			String name = userForm.getField("name").getValue().toString();
			AccountInfo ac = new AccountInfo(id, name);

			V7CR v7cr = V7CR.getInstance();
			Role connect = new Role(v7cr.load("roles", "connect"));
			connect = connect.addMember(ac);
			v7cr.save("roles", connect);
			System.out.println(connect);
		} catch (InvalidValueException e) {
		}
	}

}
