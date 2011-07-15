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

import static org.apache.commons.lang.ArrayUtils.EMPTY_STRING_ARRAY;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.bson.types.ObjectId;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.vaadin.easyuploads.MultiFileUpload;

import v7cr.v7db.BSONBackedObject;
import v7cr.v7db.LocalizedString;
import v7cr.v7db.SchemaDefinition;

import com.mongodb.gridfs.GridFSFile;
import com.vaadin.terminal.ExternalResource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Window.Notification;

@SuppressWarnings("serial")
public class ReviewTab extends CustomComponent implements ClickListener {

	final ObjectId reviewId;

	private Review r;

	private TextArea newComment;

	private OptionGroup voteOptions;

	private ComponentContainer fileArea;

	ReviewTab(ObjectId id) {
		setIcon(new ThemeResource("../runo/icons/16/document-txt.png"));

		reviewId = id;

		reload();
	}

	private void reload() {
		final V7CR v7 = V7CR.getInstance();
		r = new Review(v7.load("reviews", reviewId));
		Project p = new Project(v7.load("projects", r.getProjectName()));
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

		vl.addComponent(getBasicInfo(v7, r, p, url));
		Panel s = getSVNPanel(v7, r.getSchemaDefinition(), svn, p);
		if (s != null)
			vl.addComponent(s);

		final BSONBackedObject[] notes = r.getObjectFieldAsArray("notes");
		if (notes != null) {
			for (BSONBackedObject note : notes) {
				vl.addComponent(getNotesPanel(note));
			}
		}

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
			fileArea = new VerticalLayout();
			vl.addComponent(fileArea);
			MultiFileUpload uploader = new MultiFileUpload() {

				@Override
				protected void handleFile(File file, String fileName,
						String mimeType, long length) {
					try {
						GridFSFile gf = v7.storeFile(file, fileName);
						TemporaryFile tf = new TemporaryFile(v7, gf);
						fileArea.addComponent(tf);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			};

			vl.addComponent(uploader);

			Button submitButton = new Button(v7.getMessage("button.submit"));
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
			V7CR v7 = V7CR.getInstance();

			BSONBackedObject[] files = data.getObjectFieldAsArray("files");
			if (files != null) {
				for (BSONBackedObject f : files) {
					String fn = f.getStringField("filename");
					ObjectId fileId = f.getObjectIdField("_id");
					if (fileId != null)
						grid.addComponent(new Link(fn, new GridFSResource(v7,
								fileId, fn)), 0, grid.getCursorY(), 2, grid
								.getCursorY());
				}
			}

			// the last comment can still be edited for some time
			long timeLeft = created.getTime() + 30 * 60 * 1000
					- System.currentTimeMillis();
			if (timeLeft > 0) {

				p.addComponent(new Button(v7.getMessage("button.edit"),
						new Button.ClickListener() {

							public void buttonClick(ClickEvent event) {
								makeEditable();
							}
						}

				));
				p.addComponent(new Button(v7.getMessage("button.delete"),
						new Button.ClickListener() {

							public void buttonClick(ClickEvent event) {
								V7CR v7cr = V7CR.getInstance();
								v7cr.update("reviews", r.deleteVote(data));
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
			V7CR v7 = V7CR.getInstance();

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
			p.addComponent(new Button(v7.getMessage("button.submit"),
					new Button.ClickListener() {

						public void buttonClick(ClickEvent event) {
							V7CR v7cr = V7CR.getInstance();
							v7cr.update("reviews", r.updateVote(data, textArea
									.getValue().toString(), voteOptions
									.getValue().toString()));
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
			p.addComponent(new Button(v7.getMessage("button.cancel"),
					new Button.ClickListener() {

						public void buttonClick(ClickEvent event) {
							makeNotEditable();
						}
					}));
			p.addComponent(new Button(v7.getMessage("button.delete"),
					new Button.ClickListener() {

						public void buttonClick(ClickEvent event) {
							V7CR v7cr = V7CR.getInstance();
							v7cr.update("reviews", r.deleteVote(data));
							reload();

						}
					}));

		}
	}

	private Panel getBasicInfo(V7CR v7, Review r, Project proj, String linkUrl) {

		Panel p = new Panel(v7.getMessage("reviewTab.review"));
		p.setWidth("600px");
		GridLayout grid = new GridLayout(3, 4);
		grid.setSizeFull();
		p.setContent(grid);
		grid.setSpacing(true);
		Locale l = v7.getLocale();
		SchemaDefinition sd = r.getSchemaDefinition();
		grid.addComponent(new Label(sd.getFieldCaption("s", l)), 0, 0, 1, 0);
		p.addComponent(new Label("<b>"
				+ LocalizedString.get(sd.getFieldDefinition("s")
						.getPossibleValueMetaData(r.getStatus()), "caption", l)
				+ "</b>", Label.CONTENT_XHTML));
		p.addComponent(new Label(sd.getFieldCaption("p", l)));
		p.addComponent(new Label("[" + proj.getId() + "]"));
		grid.addComponent(new Label(proj.getName()));
		p.addComponent(new Label(sd.getFieldCaption("reviewee", l)));
		p.addComponent(new Label(r.getReviewee().getId()));
		grid.addComponent(new Label(r.getReviewee().getName()));
		p.addComponent(new Label(sd.getFieldCaption("t", l)));
		grid.addComponent(new Label(r.getTitle()), 1, 3, 2, 3);
		p.addComponent(new Label(v7.getMessage("reviewTab.directLink")));
		Link link = new Link(linkUrl, new ExternalResource(linkUrl));
		link.setTargetName("_blank");
		link.setIcon(new ThemeResource("../runo/icons/16/arrow-right.png"));
		grid.addComponent(link);
		return p;

	}

	private Panel getNotesPanel(BSONBackedObject note) {
		Panel p = new Panel(note.getStringField("t"));
		p.setWidth("600px");
		Label c = new Label(note.getStringField("c"));
		p.addComponent(c);

		String v = note.getStringField("v");
		if ("+".equals(v)) {
			c.setIcon(new ThemeResource("../runo/icons/16/ok.png"));
		}
		if ("-".equals(v)) {
			c.setIcon(new ThemeResource("../runo/icons/16/attention.png"));
		}
		return p;
	}

	private Panel getSVNPanel(V7CR v7, SchemaDefinition sd, SVNLogEntry svn,
			Project proj) {
		if (svn == null)
			return null;
		Locale l = v7.getLocale();
		Panel p = new Panel(v7.getMessage("reviewTab.subversion"));
		p.setWidth("600px");
		GridLayout grid = new GridLayout(4, 4);
		grid.setSizeFull();
		p.setContent(grid);
		grid.setSpacing(true);
		p.addComponent(new Label(sd.getFieldCaption("svn.rev", l)));
		p.addComponent(new Label("" + svn.getRevision()));
		p.addComponent(new Label(DateFormat.getDateTimeInstance().format(
				svn.getDate())));
		p.addComponent(new Label(svn.getAuthor()));
		Link link = new Link(v7.getMessage("reviewTab.viewChanges"),
				new ExternalResource(proj
						.getChangesetViewUrl(svn.getRevision())));
		link.setTargetName("_blank");
		link.setIcon(new ThemeResource("../runo/icons/16/arrow-right.png"));
		p.addComponent(link);
		grid.addComponent(new Label(svn.getMessage()), 1, 1, 3, 1);

		Map<String, SVNLogEntryPath> changed = svn.getChangedPaths();

		if (changed != null) {
			Tree changeTree = new Tree(sd.getFieldCaption("svn.changed", l)
					+ "(" + changed.size() + ")");
			Set<String> paths = changed.keySet();
			for (String s : changed.keySet()) {
				changeTree.addItem(s);
				changeTree.setChildrenAllowed(s, false);
				changeTree
						.setItemCaption(s, changed.get(s).getType() + " " + s);
			}
			if (paths.size() > 5) {
				compressTree(changeTree, paths);
			}

			grid.addComponent(changeTree, 0, 2, 3, 2);
		}
		return p;
	}

	private String connectPath(Tree tree, String node) {
		tree.addItem(node);
		tree.setChildrenAllowed(node, true);
		String pp = StringUtils.substringBeforeLast(StringUtils
				.substringBeforeLast(node, "/"), "/")
				+ "/";
		while (true) {
			if (tree.containsId(pp)) {
				tree.setParent(node, pp);
				return pp;
			}
			pp = StringUtils.substringBeforeLast(StringUtils
					.substringBeforeLast(pp, "/"), "/")
					+ "/";
			if ("/".equals(pp))
				return pp;
		}
	}

	// introduce intermediate nodes (for common prefixes) to make the tree more
	// even
	private void compressTree(Tree tree, Collection<String> nodes) {

		List<String> orgPaths = new ArrayList<String>(nodes);

		int end = orgPaths.size();

		for (int start = 0; start < end; start++) {
			String prefix = StringUtils.getCommonPrefix(orgPaths.subList(start,
					end).toArray(EMPTY_STRING_ARRAY));
			if (!prefix.endsWith("/")) {
				prefix = StringUtils.substringBeforeLast(prefix, "/") + "/";
			}
			String first = orgPaths.get(start);
			String extendedPrefix = prefix
					+ StringUtils.substringBefore(first.substring(prefix
							.length()), "/");
			if (!extendedPrefix.equals(first)) {
				extendedPrefix += "/";
				if (tree.containsId(prefix)) {
					tree.addItem(extendedPrefix);
					tree.setParent(extendedPrefix, prefix);
				} else {
					tree.setParent(extendedPrefix, connectPath(tree,
							extendedPrefix));
				}
				tree.setChildrenAllowed(extendedPrefix, true);
				int last;
				for (last = start; last < end; last++) {
					String l = orgPaths.get(last);
					if (l.startsWith(extendedPrefix)) {
						tree.setParent(l, extendedPrefix);
					} else {
						last++;
						break;
					}
				}
				tree.setItemCaption(extendedPrefix, extendedPrefix + "("
						+ (last - 1 - start) + ")");

				if (last - start > 5) {
					compressTree(tree, orgPaths.subList(start, last - 1));
				}
				start = last - 1;

			} else {
				if (!tree.containsId(prefix)) {
					prefix = connectPath(tree, prefix);
				}

				if (!prefix.equals(first)) {
					tree.setParent(first, prefix);
				}

			}
		}

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
		v7cr.update("reviews", r.addVote(v7cr.getSessionUser(), new Date(),
				comment, vote, fileArea));

		reload();
	}
}
