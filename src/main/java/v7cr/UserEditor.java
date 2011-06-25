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

@SuppressWarnings("serial")
class UserEditor extends CustomComponent implements ClickListener {

	private final Form userForm = new Form();

	UserEditor(V7CR v7) {
		setCaption(v7.getMessage("userEditor.name"));
		setIcon(new ThemeResource("../runo/icons/16/user.png"));
		HorizontalLayout hl = new HorizontalLayout();
		setCompositionRoot(hl);

		userForm.setCaption(v7.getMessage("userEditor.form.caption"));
		userForm.setDescription(v7.getMessage("userEditor.form.description"));
		TextField id = new TextField(v7.getMessage("userEditor.form.email"));
		id.setRequired(true);
		id
				.addValidator(new EmailValidator(v7
						.getMessage("error.invalidEmail")));
		userForm.addField("id", id);
		TextField name = new TextField(v7
				.getMessage("userEditor.form.displayName"));
		name.setRequired(true);
		userForm.addField("name", name);

		userForm.getLayout().addComponent(
				new Button(v7.getMessage("button.create")));
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
		} catch (InvalidValueException e) {
		}
	}

}
