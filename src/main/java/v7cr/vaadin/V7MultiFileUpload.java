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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vaadin.easyuploads.DirectoryFileFactory;
import org.vaadin.easyuploads.FileBuffer;
import org.vaadin.easyuploads.FileFactory;
import org.vaadin.easyuploads.MultiUploadHandler;
import org.vaadin.easyuploads.MultiUpload.FileDetail;
import org.vaadin.easyuploads.UploadField.FieldType;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptAll;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.terminal.StreamVariable;
import com.vaadin.terminal.StreamVariable.StreamingEndEvent;
import com.vaadin.terminal.StreamVariable.StreamingErrorEvent;
import com.vaadin.terminal.StreamVariable.StreamingProgressEvent;
import com.vaadin.terminal.StreamVariable.StreamingStartEvent;
import com.vaadin.terminal.gwt.server.AbstractWebApplicationContext;
import com.vaadin.terminal.gwt.server.WebBrowser;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.Html5File;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.DragAndDropWrapper.WrapperTransferable;

/**
 * Hacked version of the Vaadin MultiFileUpload, to make it use v7files in proxy
 * mode: the component posts to v7files, v7files proxies to the streamvariable
 * 
 */
@SuppressWarnings("serial")
public abstract class V7MultiFileUpload extends CssLayout implements
		DropHandler {

	private final CssLayout progressBars = new CssLayout();
	private final CssLayout uploads = new CssLayout();
	private String uploadButtonCaption = "...";

	public V7MultiFileUpload() {
		setWidth("200px");
		addComponent(progressBars);
		uploads.setStyleName("v-multifileupload-uploads");
		addComponent(uploads);
		prepareUpload();
	}

	private void prepareUpload() {
		final FileBuffer receiver = createReceiver();

		final V7MultiUpload upload = new V7MultiUpload();
		MultiUploadHandler handler = new MultiUploadHandler() {
			private LinkedList<ProgressIndicator> indicators;

			public void streamingStarted(StreamingStartEvent event) {
			}

			public void streamingFinished(StreamingEndEvent event) {
				if (!indicators.isEmpty()) {
					progressBars.removeComponent(indicators.remove(0));
				}
				File file = receiver.getFile();
				handleFile(file, event.getFileName(), event.getMimeType(),
						event.getBytesReceived());
				receiver.setValue(null);
			}

			public void streamingFailed(StreamingErrorEvent event) {
				Logger.getLogger(getClass().getName()).log(Level.FINE,
						"Streaming failed", event.getException());

				for (ProgressIndicator progressIndicator : indicators) {
					progressBars.removeComponent(progressIndicator);
				}

			}

			public void onProgress(StreamingProgressEvent event) {
				long readBytes = event.getBytesReceived();
				long contentLength = event.getContentLength();
				float f = (float) readBytes / (float) contentLength;
				indicators.get(0).setValue(f);
			}

			public OutputStream getOutputStream() {
				FileDetail next = upload.getPendingFileNames().iterator()
						.next();
				return receiver.receiveUpload(next.getFileName(), next
						.getMimeType());
			}

			public void filesQueued(Collection<FileDetail> pendingFileNames) {
				if (indicators == null) {
					indicators = new LinkedList<ProgressIndicator>();
				}
				for (FileDetail f : pendingFileNames) {
					ProgressIndicator pi = createProgressIndicator();
					progressBars.addComponent(pi);
					pi.setCaption(f.getFileName());
					pi.setVisible(true);
					indicators.add(pi);
				}
			}
		};
		upload.setHandler(handler);
		upload.setButtonCaption(getUploadButtonCaption());
		uploads.addComponent(upload);

	}

	private ProgressIndicator createProgressIndicator() {
		ProgressIndicator progressIndicator = new ProgressIndicator();
		progressIndicator.setPollingInterval(300);
		progressIndicator.setValue(0);
		return progressIndicator;
	}

	public String getUploadButtonCaption() {
		return uploadButtonCaption;
	}

	public void setUploadButtonCaption(String uploadButtonCaption) {
		this.uploadButtonCaption = uploadButtonCaption;
		Iterator<Component> componentIterator = uploads.getComponentIterator();
		while (componentIterator.hasNext()) {
			Component next = componentIterator.next();
			if (next instanceof V7MultiUpload) {
				V7MultiUpload upload = (V7MultiUpload) next;
				if (upload.isVisible()) {
					upload.setButtonCaption(getUploadButtonCaption());
				}
			}
		}
	}

	private FileFactory fileFactory;

	public FileFactory getFileFactory() {
		if (fileFactory == null) {
			fileFactory = new TempFileFactory();
		}
		return fileFactory;
	}

	static class TempFileFactory implements FileFactory {

		public File createFile(String fileName, String mimeType) {
			final String tempFileName = "upload_tmpfile_"
					+ System.currentTimeMillis();
			try {
				return File.createTempFile(tempFileName, null);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

	}

	public void setFileFactory(FileFactory fileFactory) {
		this.fileFactory = fileFactory;
	}

	protected FileBuffer createReceiver() {
		FileBuffer receiver = new FileBuffer(FieldType.FILE) {
			@Override
			public FileFactory getFileFactory() {
				return V7MultiFileUpload.this.getFileFactory();
			}
		};
		return receiver;
	}

	protected int getPollinInterval() {
		return 500;
	}

	@Override
	public void attach() {
		super.attach();
		if (supportsFileDrops()) {
			prepareDropZone();
		}
	}

	private DragAndDropWrapper dropZone;

	/**
	 * Sets up DragAndDropWrapper to accept multi file drops.
	 */
	private void prepareDropZone() {
		if (dropZone == null) {
			Component label = new Label(getAreaText(), Label.CONTENT_XHTML);
			label.setSizeUndefined();
			dropZone = new DragAndDropWrapper(label);
			dropZone.setStyleName("v-multifileupload-dropzone");
			dropZone.setSizeUndefined();
			addComponent(dropZone, 1);
			dropZone.setDropHandler(this);
			addStyleName("no-horizontal-drag-hints");
			addStyleName("no-vertical-drag-hints");
		}
	}

	protected String getAreaText() {
		return "<small>DROP<br/>FILES</small>";
	}

	protected boolean supportsFileDrops() {
		AbstractWebApplicationContext context = (AbstractWebApplicationContext) getApplication()
				.getContext();
		WebBrowser browser = context.getBrowser();
		if (browser.isChrome()) {
			return true;
		} else if (browser.isFirefox()) {
			return true;
		} else if (browser.isSafari()) {
			return true;
		}
		return false;
	}

	abstract protected void handleFile(File file, String fileName,
			String mimeType, long length);

	/**
	 * A helper method to set DirectoryFileFactory with given pathname as
	 * directory.
	 * 
	 * @param file
	 */
	public void setRootDirectory(String directoryWhereToUpload) {
		setFileFactory(new DirectoryFileFactory(
				new File(directoryWhereToUpload)));
	}

	public AcceptCriterion getAcceptCriterion() {
		// TODO accept only files
		// return new And(new TargetDetailIs("verticalLocation","MIDDLE"), new
		// TargetDetailIs("horizontalLoction", "MIDDLE"));
		return AcceptAll.get();
	}

	public void drop(DragAndDropEvent event) {
		DragAndDropWrapper.WrapperTransferable transferable = (WrapperTransferable) event
				.getTransferable();
		Html5File[] files = transferable.getFiles();
		for (final Html5File html5File : files) {
			final ProgressIndicator pi = new ProgressIndicator();
			pi.setCaption(html5File.getFileName());
			progressBars.addComponent(pi);
			final FileBuffer receiver = createReceiver();
			html5File.setStreamVariable(new StreamVariable() {

				private String name;
				private String mime;

				public OutputStream getOutputStream() {
					return receiver.receiveUpload(name, mime);
				}

				public boolean listenProgress() {
					return true;
				}

				public void onProgress(StreamingProgressEvent event) {
					float p = (float) event.getBytesReceived()
							/ (float) event.getContentLength();
					pi.setValue(p);
				}

				public void streamingStarted(StreamingStartEvent event) {
					name = event.getFileName();
					mime = event.getMimeType();

				}

				public void streamingFinished(StreamingEndEvent event) {
					progressBars.removeComponent(pi);
					handleFile(receiver.getFile(), html5File.getFileName(),
							html5File.getType(), html5File.getFileSize());
					receiver.setValue(null);
				}

				public void streamingFailed(StreamingErrorEvent event) {
					progressBars.removeComponent(pi);
				}

				public boolean isInterrupted() {
					return false;
				}
			});
		}

	}
}
