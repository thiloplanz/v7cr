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

import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.bson.types.ObjectId;
import org.tmatesoft.svn.core.SVNLogEntry;

import v7cr.v7db.BSONBackedObject;

import com.vaadin.terminal.ExternalResource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Window.Notification;

@SuppressWarnings("serial")
public class ReviewTab extends CustomComponent implements ClickListener {

	final ObjectId reviewId;

	private TextArea newComment;

	private OptionGroup voteOptions;

	ReviewTab(ObjectId id) {
		setIcon(new ThemeResource("../runo/icons/16/document-txt.png"));

		reviewId = id;

		reload();
	}

	private void reload() {

		final Review r = new Review(V7CR.getInstance()
				.load("reviews", reviewId));
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
		vl.setSizeFull();

		vl.addComponent(getBasicInfo(r, p, url));
		Panel s = getSVNPanel(svn, p);
		if (s != null)
			vl.addComponent(s);

		final BSONBackedObject[] votes = r.getObjectFieldAsArray("v");
		if (votes != null) {
			for (BSONBackedObject vote : votes) {
				vl.addComponent(new CommentPanel(vote));
			}

		}

		{
			HorizontalLayout commentGrid = new HorizontalLayout();

			newComment = new TextArea();
			newComment.setColumns(50);
			newComment.setRows(10);
			commentGrid.addComponent(newComment);
			voteOptions = new OptionGroup();
			voteOptions.addItem("+");
			voteOptions.addItem("0");
			voteOptions.addItem("-");
			voteOptions.setValue("0");
			commentGrid.addComponent(voteOptions);

			vl.addComponent(commentGrid);

			Button submitButton = new Button("submit");
			submitButton.addListener(this);
			vl.addComponent(submitButton);
		}

		setCompositionRoot(vl);

		Component parent = getParent();
		if (parent instanceof TabSheet) {
			TabSheet t = (TabSheet) parent;
			Iterator<Component> i = t.getComponentIterator();
			while (i.hasNext()) {
				Component c = i.next();
				if (c instanceof ReviewList
						&& r.getProjectName().equals(c.getCaption())) {
					((ReviewList) c).reload();
					break;
				}
			}
		}
	}

	class CommentPanel extends CustomComponent {

		private final Panel p;

		private final BSONBackedObject data;

		CommentPanel(BSONBackedObject vote) {
			p = new Panel();
			data = vote;
			p.setWidth("600px");
			makeNotEditable();
		}

		private void makeNotEditable() {
			final GridLayout grid = new GridLayout(3, 4);
			grid.setSizeFull();
			p.setContent(grid);
			grid.setSpacing(true);

			Date created = data.getDateField("d");

			p.addComponent(new Label(data.getStringField("by.n")));
			p.addComponent(new Label(data.getStringField("by._id")));
			p.addComponent(new Label(DateFormat.getDateTimeInstance().format(
					created)));
			grid.addComponent(new Label(data.getStringField("c"),
					Label.CONTENT_PREFORMATTED), 0, 1, 1, 1);
			Label icon = new Label();
			String v = data.getStringField("v");
			if ("+".equals(v)) {
				icon.setIcon(new ThemeResource("../runo/icons/32/ok.png"));
			}
			if ("-".equals(v)) {
				icon
						.setIcon(new ThemeResource(
								"../runo/icons/32/attention.png"));
			}
			p.addComponent(icon);
			setCompositionRoot(p);

			// the last comment can still be edited for some time
			long timeLeft = created.getTime() + 30 * 60 * 1000
					- System.currentTimeMillis();
			if (timeLeft > 0) {

				p.addComponent(new Button("edit", new Button.ClickListener() {

					public void buttonClick(ClickEvent event) {
						makeEditable();
					}
				}

				));
				p.addComponent(new Button("delete", new Button.ClickListener() {

					public void buttonClick(ClickEvent event) {
						V7CR v7cr = V7CR.getInstance();
						Review r = new Review(v7cr.load("reviews", reviewId))
								.deleteVote(data);
						v7cr.save("reviews", r);
						reload();
					}
				}));
				p.addComponent(new Label(DurationFormatUtils
						.formatDurationWords(timeLeft, true, true)
						+ " left to edit"));
			}
			;

		}

		private void makeEditable() {
			final GridLayout grid = new GridLayout(3, 4);
			grid.setSizeFull();
			p.setContent(grid);
			grid.setSpacing(true);

			final Date created = data.getDateField("d");

			p.addComponent(new Label(data.getStringField("by.n")));
			p.addComponent(new Label(data.getStringField("by._id")));
			p.addComponent(new Label(DateFormat.getDateTimeInstance().format(
					created)));
			final TextArea textArea = new TextArea(null, data
					.getStringField("c"));
			textArea.setSizeFull();
			grid.addComponent(textArea, 0, 1, 1, 1);
			final OptionGroup voteOptions = new OptionGroup();
			voteOptions.addItem("+");
			voteOptions.addItem("0");
			voteOptions.addItem("-");
			voteOptions.setValue("0");
			grid.addComponent(voteOptions);
			p.addComponent(new Button("submit", new Button.ClickListener() {

				public void buttonClick(ClickEvent event) {
					V7CR v7cr = V7CR.getInstance();
					Review r = new Review(v7cr.load("reviews", reviewId))
							.updateVote(data, textArea.getValue().toString(),
									voteOptions.getValue().toString());
					v7cr.save("reviews", r);
					reload();

					//					
					// V7CR.getInstance().getDBCollection("reviews").update(
					// new BasicDBObject("_id", reviewId).append("v.d",
					// created),
					// new BasicDBObject("$set", new BasicDBObject(
					// "v.$.c", textArea.getValue()).append(
					// "v.$.v", voteOptions.getValue())));
				}
			}

			));
			p.addComponent(new Button("cancel", new Button.ClickListener() {

				public void buttonClick(ClickEvent event) {
					makeNotEditable();
				}
			}));
			p.addComponent(new Button("delete", new Button.ClickListener() {

				public void buttonClick(ClickEvent event) {
					V7CR v7cr = V7CR.getInstance();
					Review r = new Review(v7cr.load("reviews", reviewId))
							.deleteVote(data);
					v7cr.save("reviews", r);
					reload();

				}
			}));

		}
	}

	private Panel getBasicInfo(Review r, Project proj, String linkUrl) {
		Panel p = new Panel("Review");
		p.setWidth("600px");
		GridLayout grid = new GridLayout(3, 4);
		grid.setSizeFull();
		p.setContent(grid);
		grid.setSpacing(true);
		grid.addComponent(new Label("Status:"), 0, 0, 1, 0);
		p.addComponent(new Label("<b>" + r.getStatus() + "</b>",
				Label.CONTENT_XHTML));
		p.addComponent(new Label("Project:"));
		p.addComponent(new Label("[" + proj.getId() + "]"));
		grid.addComponent(new Label(proj.getName()));
		p.addComponent(new Label("Reviewee:"));
		p.addComponent(new Label(r.getReviewee().getId()));
		grid.addComponent(new Label(r.getReviewee().getName()));
		p.addComponent(new Label("Title:"));
		grid.addComponent(new Label(r.getTitle()), 1, 3, 2, 3);
		p.addComponent(new Label("Link:"));
		Link link = new Link(linkUrl, new ExternalResource(linkUrl));
		link.setTargetName("_blank");
		link.setIcon(new ThemeResource("../runo/icons/16/arrow-right.png"));
		grid.addComponent(link);
		return p;

	}

	private Panel getSVNPanel(SVNLogEntry svn, Project proj) {
		if (svn == null)
			return null;
		Panel p = new Panel("Subversion");
		p.setWidth("600px");
		GridLayout grid = new GridLayout(4, 4);
		grid.setSizeFull();
		p.setContent(grid);
		grid.setSpacing(true);
		p.addComponent(new Label("Revision:"));
		p.addComponent(new Label("" + svn.getRevision()));
		p.addComponent(new Label(DateFormat.getDateTimeInstance().format(
				svn.getDate())));
		p.addComponent(new Label(svn.getAuthor()));
		Link link = new Link("view changes", new ExternalResource(proj
				.getChangesetViewUrl(svn.getRevision())));
		link.setTargetName("_blank");
		link.setIcon(new ThemeResource("../runo/icons/16/arrow-right.png"));
		p.addComponent(link);
		grid.addComponent(new Label(svn.getMessage()), 1, 1, 3, 1);
		return p;
	}

	public void buttonClick(ClickEvent event) {
		String comment = (String) newComment.getValue();
		String vote = (String) voteOptions.getValue();

		if (StringUtils.isBlank(comment)) {
			getWindow().showNotification(null, "Please leave a comment",
					Notification.TYPE_WARNING_MESSAGE);
			return;
		}
		if (StringUtils.isBlank(vote)) {
			getWindow().showNotification(null, "Please vote",
					Notification.TYPE_WARNING_MESSAGE);
			return;
		}

		V7CR v7cr = V7CR.getInstance();
		Review r = new Review(v7cr.load("reviews", reviewId)).addVote(v7cr
				.getSessionUser(), new Date(), comment, vote);
		v7cr.save("reviews", r);

		reload();

	}
}
