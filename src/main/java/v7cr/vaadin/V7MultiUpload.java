/**
 * Copyright (c) 2011-2012, Thilo Planz. All rights reserved.
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

package v7cr.vaadin;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.vaadin.easyuploads.MultiUploadHandler;
import org.vaadin.easyuploads.MultiUpload.FileDetail;

import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.terminal.StreamVariable;
import com.vaadin.ui.AbstractComponent;

/**
 * Server side component for the VMultiUpload widget. Pretty much hacked up
 * together to test new Receiver support in the GWT terminal.
 */
@SuppressWarnings("serial")
// @com.vaadin.ui.ClientWidget(org.vaadin.easyuploads.client.ui.VMultiUpload.class)
public class V7MultiUpload extends AbstractComponent {

	List<FileDetail> pendingFiles = new ArrayList<FileDetail>();

	private MultiUploadHandler receiver;

	StreamVariable streamVariable = new StreamVariable() {

		public void streamingStarted(StreamingStartEvent event) {
			final FileDetail next = getPendingFileNames().iterator().next();
			receiver.streamingStarted(new StreamingStartEvent() {

				public String getMimeType() {
					return next.getMimeType();
				}

				public String getFileName() {
					return next.getFileName();
				}

				public long getContentLength() {
					return next.getContentLength();
				}

				public long getBytesReceived() {
					return 0;
				}

				public void disposeStreamVariable() {

				}
			});
		}

		public void streamingFinished(final StreamingEndEvent event) {

			final FileDetail next = getPendingFileNames().iterator().next();

			receiver.streamingFinished(new StreamingEndEvent() {

				public String getMimeType() {
					return next.getMimeType();
				}

				public String getFileName() {
					return next.getFileName();
				}

				public long getContentLength() {
					return next.getContentLength();
				}

				public long getBytesReceived() {
					return event.getBytesReceived();
				}

			});
			pendingFiles.remove(0);
		}

		public void streamingFailed(StreamingErrorEvent event) {
			receiver.streamingFailed(event);
		}

		public void onProgress(StreamingProgressEvent event) {
			receiver.onProgress(event);
		}

		public boolean listenProgress() {
			return true;
		}

		public boolean isInterrupted() {
			return false;
		}

		public OutputStream getOutputStream() {
			return receiver.getOutputStream();
		}
	};

	private boolean ready;

	public void setHandler(MultiUploadHandler receiver) {
		this.receiver = receiver;
	}

	@Override
	public void paintContent(PaintTarget target) throws PaintException {
		super.paintContent(target);
		target.addVariable(this, "realTarget", streamVariable);
		target
				.addVariable(this, "target",
						"http://0.0.0.0:80888/upload/vaadin");
		if (ready) {
			target.addAttribute("ready", true);
			ready = false;
		}
		target.addAttribute("buttoncaption", getButtonCaption());
	}

	@Override
	public void changeVariables(Object source, Map<String, Object> variables) {
		super.changeVariables(source, variables);
		if (variables.containsKey("filequeue")) {
			String[] filequeue = (String[]) variables.get("filequeue");
			List<FileDetail> newFiles = new ArrayList<FileDetail>(
					filequeue.length);
			for (String string : filequeue) {
				newFiles.add(new FileDetail(string));
			}
			receiver.filesQueued(newFiles);
			pendingFiles.addAll(newFiles);
			requestRepaint();
			ready = true;
		}
	}

	public Collection<FileDetail> getPendingFileNames() {
		return Collections.unmodifiableCollection(pendingFiles);
	}

	public void setButtonCaption(String buttonCaption) {
		this.buttonCaption = buttonCaption;
	}

	public String getButtonCaption() {
		return buttonCaption;
	}

	private String buttonCaption = "...";

}
