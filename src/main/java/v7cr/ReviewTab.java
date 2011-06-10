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

import java.util.Arrays;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.tmatesoft.svn.core.SVNLogEntry;

import v7cr.v7db.BSONBackedObject;
import v7cr.vaadin.ItemFactory;

import com.vaadin.terminal.ExternalResource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Form;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

@SuppressWarnings("serial")
public class ReviewTab extends CustomComponent implements ClickListener {

	final ObjectId reviewId;

	private Review review;

	private Form commentForm;

	ReviewTab(ObjectId id) {
		setIcon(new ThemeResource("../runo/icons/16/document-txt.png"));

		reviewId = id;
		Review r = new Review(V7CR.getInstance().load("reviews", reviewId));
		Project p = new Project(V7CR.getInstance().load("projects",
				r.getProjectName()));
		SVNLogEntry svn = r.getSVNLogEntry();
		String url;
		if (svn != null) {
			url = r.getProjectName() + "-" + svn.getRevision();
			setCaption(url);
		} else {
			url = reviewId.toString();
			setCaption(StringUtils.abbreviate(r.getTitle(), 20));
		}
		VerticalLayout vl = new VerticalLayout();

		{
			Link link = new Link(r.getTitle(), new ExternalResource(url));
			link.setTargetName("_blank");
			vl.addComponent(link);
		}

		if (svn != null) {
			vl.addComponent(new Label("Subversion revision "
					+ svn.getRevision()));
			Link link = new Link("View changeset", new ExternalResource(p
					.getChangesetViewUrl(svn.getRevision())));
			link.setTargetName("_blank");
			vl.addComponent(link);
		}

		{
			Form form = new Form();
			form.setWidth("100%");
			form.setFormFieldFactory(ItemFactory.getFormFieldFactory(r
					.getSchemaDefinition()));
			form.addItemProperty("t", ItemFactory.getTextField(r, "t"));
			form.addItemProperty("s", ItemFactory.getTextField(r, "s"));

			form.addItemProperty("m", new TextArea("Commit message", r
					.getSVNLogEntry().getMessage()));

			form.addItemProperty("b", new TextArea("Raw", Arrays.toString(r
					.getObjectFieldAsArray("v"))));

			form.setReadOnly(true);
			vl.addComponent(form);
		}

		BSONBackedObject[] votes = r.getObjectFieldAsArray("v");
		if (votes != null) {
			for (BSONBackedObject vote : votes) {
				try {
					vl.addComponent(new Label(vote.getStringField("by.n")));
				} catch (NullPointerException e) {
					e.printStackTrace();
				}
				TextArea t = new TextArea("Comment", vote.getStringField("c"));
				t.setReadOnly(true);
				vl.addComponent(t);

			}
		}

		{
			commentForm = new Form();
			OptionGroup voteOptions = new OptionGroup("Vote");
			voteOptions.addItem("+");
			voteOptions.addItem("0");
			voteOptions.addItem("-");
			commentForm.addField("vote", voteOptions);
			commentForm.addField("comment", new TextArea("Comment"));
			vl.addComponent(commentForm);
			Button submitButton = new Button("submit");
			submitButton.addListener(this);
			vl.addComponent(submitButton);
		}

		setCompositionRoot(vl);
	}

	public void buttonClick(ClickEvent event) {
		String comment = commentForm.getItemProperty("comment").getValue()
				.toString();
		String vote = commentForm.getItemProperty("vote").getValue().toString();
		V7CR v7cr = V7CR.getInstance();
		Review r = new Review(v7cr.load("reviews", reviewId)).addVote(v7cr
				.getSessionUser(), new Date(), comment, vote);
		v7cr.save("reviews", r);
		((TabSheet) getParent()).removeComponent(this);
	}
}
