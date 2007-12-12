/*******************************************************************************
 * Copyright (c) 2004 Composent, Inc. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.filetransfer.retrieve;

import java.io.IOException;
import java.net.*;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.core.util.Proxy;
import org.eclipse.ecf.filetransfer.*;
import org.eclipse.ecf.filetransfer.identity.IFileID;
import org.eclipse.ecf.internal.provider.filetransfer.Messages;
import org.eclipse.ecf.provider.filetransfer.util.JREProxyHelper;
import org.eclipse.osgi.util.NLS;

public class UrlConnectionRetrieveFileTransfer extends AbstractRetrieveFileTransfer {

	private static final int HTTP_RANGE_RESPONSE = 206;

	private static final int OK_RESPONSE_CODE = 200;

	protected URLConnection urlConnection;

	// XXX currently unused
	protected IConnectContext connectContext;

	protected long lastModifiedTime = 0L;

	protected int httpVersion = 1;

	protected int responseCode = -1;

	protected String responseMessage = null;

	protected IFileID fileid = null;

	private JREProxyHelper proxyHelper = null;

	public UrlConnectionRetrieveFileTransfer() {
		super();
		proxyHelper = new JREProxyHelper();
	}

	protected void connect() throws IOException {
		urlConnection = getRemoteFileURL().openConnection();
	}

	protected boolean isConnected() {
		return (urlConnection != null);
	}

	protected void setResumeRequestHeaderValues() throws IOException {
		if (this.bytesReceived <= 0 || this.fileLength <= this.bytesReceived)
			throw new IOException(Messages.UrlConnectionRetrieveFileTransfer_RESUME_START_ERROR);
		setRangeHeader("bytes=" + this.bytesReceived + "-"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected void setRequestHeaderValues() throws InvalidFileRangeSpecificationException {
		final IFileRangeSpecification rangeSpec = getFileRangeSpecification();
		if (rangeSpec != null && isHTTP()) {
			final long startPosition = rangeSpec.getStartPosition();
			final long endPosition = rangeSpec.getEndPosition();
			if (startPosition < 0)
				throw new InvalidFileRangeSpecificationException(Messages.UrlConnectionRetrieveFileTransfer_RESUME_START_POSITION_LESS_THAN_ZERO, rangeSpec);
			if (endPosition != -1L && endPosition <= startPosition)
				throw new InvalidFileRangeSpecificationException(Messages.UrlConnectionRetrieveFileTransfer_RESUME_ERROR_END_POSITION_LESS_THAN_START, rangeSpec);
			setRangeHeader("bytes=" + startPosition + "-" + ((endPosition == -1L) ? "" : ("" + endPosition))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
	}

	private void setRangeHeader(String value) {
		urlConnection.setRequestProperty("Range", value); //$NON-NLS-1$
	}

	public int getResponseCode() {
		if (responseCode != -1)
			return responseCode;
		if (isHTTP()) {
			final String response = urlConnection.getHeaderField(0);
			if (response == null) {
				responseCode = -1;
				httpVersion = 1;
				return responseCode;
			}
			if (response == null || !response.startsWith("HTTP/")) //$NON-NLS-1$
				return -1;
			response.trim();
			final int mark = response.indexOf(" ") + 1; //$NON-NLS-1$
			if (mark == 0)
				return -1;
			if (response.charAt(mark - 2) != '1')
				httpVersion = 0;
			int last = mark + 3;
			if (last > response.length())
				last = response.length();
			responseCode = Integer.parseInt(response.substring(mark, last));
			if (last + 1 <= response.length())
				responseMessage = response.substring(last + 1);
		} else {
			responseCode = OK_RESPONSE_CODE;
			responseMessage = "OK"; //$NON-NLS-1$
		}

		return responseCode;

	}

	private boolean isHTTP() {
		final String protocol = getRemoteFileURL().getProtocol();
		if (protocol.equalsIgnoreCase("http") || protocol.equalsIgnoreCase("https")) //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		return false;
	}

	private boolean isHTTP11() {
		return (isHTTP() && httpVersion >= 1);
	}

	protected void getResponseHeaderValues() throws IOException {
		if (!isConnected())
			throw new ConnectException(Messages.UrlConnectionRetrieveFileTransfer_CONNECT_EXCEPTION_NOT_CONNECTED);
		if (getResponseCode() == -1)
			throw new IOException(Messages.UrlConnectionRetrieveFileTransfer_EXCEPTION_INVALID_SERVER_RESPONSE);
		lastModifiedTime = urlConnection.getLastModified();
		setFileLength(urlConnection.getContentLength());
	}

	protected void getResumeResponseHeaderValues() throws IOException {
		if (!isConnected())
			throw new ConnectException(Messages.UrlConnectionRetrieveFileTransfer_CONNECT_EXCEPTION_NOT_CONNECTED);
		if (getResponseCode() != HTTP_RANGE_RESPONSE)
			throw new IOException(Messages.UrlConnectionRetrieveFileTransfer_INVALID_SERVER_RESPONSE_TO_PARTIAL_RANGE_REQUEST);
		if (lastModifiedTime != urlConnection.getLastModified())
			throw new IOException(Messages.UrlConnectionRetrieveFileTransfer_EXCEPTION_FILE_MODIFIED_SINCE_LAST_ACCESS);
	}

	/**
	 * @param proxy2 the ECF proxy to setup
	 */
	protected void setupProxy(final Proxy proxy2) {
		proxyHelper.setupProxy(proxy2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.provider.filetransfer.retrieve.AbstractRetrieveFileTransfer#openStreams()
	 */
	protected void openStreams() throws IncomingFileTransferException {
		try {
			connect();
			setRequestHeaderValues();
			// Make actual GET request
			setInputStream(urlConnection.getInputStream());
			getResponseHeaderValues();
			fireReceiveStartEvent();
		} catch (final Exception e) {
			throw new IncomingFileTransferException(NLS.bind(Messages.UrlConnectionRetrieveFileTransfer_EXCEPTION_COULD_NOT_CONNECT, getRemoteFileURL().toString()), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.provider.filetransfer.retrieve.AbstractRetrieveFileTransfer#hardClose()
	 */
	protected void hardClose() {
		super.hardClose();
		urlConnection = null;
		responseCode = -1;
		if (proxyHelper != null) {
			proxyHelper.dispose();
			proxyHelper = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.filetransfer.IRetrieveFileTransferContainerAdapter#setConnectContextForAuthentication(org.eclipse.ecf.core.security.IConnectContext)
	 */
	public void setConnectContextForAuthentication(IConnectContext connectContext) {
		this.connectContext = connectContext;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.provider.filetransfer.retrieve.AbstractRetrieveFileTransfer#doPause()
	 */
	protected boolean doPause() {
		if (isPaused() || !isConnected() || isDone())
			return false;
		this.paused = true;
		return this.paused;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.provider.filetransfer.retrieve.AbstractRetrieveFileTransfer#doResume()
	 */
	protected boolean doResume() {
		if (!isPaused() || isConnected())
			return false;
		return openStreamsForResume();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.provider.filetransfer.retrieve.AbstractRetrieveFileTransfer#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == null)
			return null;
		if (adapter.equals(IFileTransferPausable.class) && isHTTP11())
			return this;
		return super.getAdapter(adapter);
	}

	/**
	 * @return <code>true</code> if streams successfully, <code>false</code> otherwise.
	 */
	private boolean openStreamsForResume() {
		final URL theURL = getRemoteFileURL();
		try {
			remoteFileURL = new URL(theURL.toString());
			connect();
			setResumeRequestHeaderValues();
			// Make actual GET request
			setInputStream(urlConnection.getInputStream());
			getResumeResponseHeaderValues();
			this.paused = false;
			fireReceiveResumedEvent();
			return true;
		} catch (final Exception e) {
			this.exception = e;
			this.done = true;
			hardClose();
			fireTransferReceiveDoneEvent();
			return false;
		}
	}

	public ID getID() {
		return fileid;
	}

}
