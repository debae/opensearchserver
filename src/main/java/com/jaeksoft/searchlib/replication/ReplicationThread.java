/**   
 * License Agreement for OpenSearchServer
 *
 * Copyright (C) 2010-2012 Emmanuel Keller / Jaeksoft
 * 
 * http://www.open-search-server.com
 * 
 * This file is part of OpenSearchServer.
 *
 * OpenSearchServer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * OpenSearchServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OpenSearchServer. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.jaeksoft.searchlib.replication;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.jaeksoft.searchlib.Client;
import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.process.ThreadAbstract;
import com.jaeksoft.searchlib.scheduler.TaskLog;
import com.jaeksoft.searchlib.util.FileUtils;
import com.jaeksoft.searchlib.util.InfoCallback;
import com.jaeksoft.searchlib.util.LastModifiedAndSize;
import com.jaeksoft.searchlib.util.ReadWriteLock;
import com.jaeksoft.searchlib.util.RecursiveDirectoryBrowser;
import com.jaeksoft.searchlib.web.PushServlet;

public class ReplicationThread extends ThreadAbstract<ReplicationThread>
		implements RecursiveDirectoryBrowser.CallBack {

	final private ReadWriteLock rwl = new ReadWriteLock();

	private volatile Client client;

	private volatile double totalSize;

	private volatile int filesSent;

	private volatile long bytesSent;

	private volatile double checkedSize;

	private volatile InfoCallback infoCallback;

	private volatile List<File> filesNotPushed;

	private volatile List<File> dirsNotPushed;

	private volatile File sourceDirectory;

	protected ReplicationThread(Client client,
			ReplicationMaster replicationMaster,
			ReplicationItem replicationItem, InfoCallback infoCallback)
			throws SearchLibException {
		super(client, replicationMaster, replicationItem);
		this.sourceDirectory = replicationItem.getDirectory(client);
		this.client = client;
		totalSize = 0;
		filesSent = 0;
		checkedSize = 0;
		filesNotPushed = null;
		dirsNotPushed = null;
		this.infoCallback = infoCallback;
	}

	public int getProgress() {
		rwl.r.lock();
		try {
			if (checkedSize == 0 || totalSize == 0)
				return 0;
			int p = (int) (checkedSize / totalSize) * 100;
			return p;
		} finally {
			rwl.r.unlock();
		}
	}

	public int getFilesSent() {
		rwl.r.lock();
		try {
			return filesSent;
		} finally {
			rwl.r.unlock();
		}
	}

	public long getBytesSent() {
		rwl.r.lock();
		try {
			return bytesSent;
		} finally {
			rwl.r.unlock();
		}
	}

	private void incFilesSent(long bytesSent) {
		rwl.w.lock();
		try {
			filesSent++;
			this.bytesSent += bytesSent;
		} finally {
			rwl.w.unlock();
		}
	}

	public ReplicationItem getReplicationItem() {
		return (ReplicationItem) getThreadItem();
	}

	private void initNotPushedList() {
		filesNotPushed = new ArrayList<File>(0);
		dirsNotPushed = new ArrayList<File>(0);
		getReplicationItem().getReplicationType().addNotPushedPath(
				sourceDirectory, filesNotPushed, dirsNotPushed);
	}

	@Override
	public void runner() throws Exception {
		setInfo("Running");
		initNotPushedList();
		client.push(this);
	}

	@Override
	public void release() {
		Exception e = getException();
		if (e != null)
			setInfo("Error: " + e.getMessage() != null ? e.getMessage() : e
					.toString());
		else if (isAborted())
			setInfo("Aborted");
		else
			setInfo("Completed");
	}

	public void push() throws SearchLibException, MalformedURLException,
			URISyntaxException {
		ReplicationItem replicationItem = getReplicationItem();
		setTotalSize(new LastModifiedAndSize(sourceDirectory, false).getSize());
		addCheckedSize(sourceDirectory.length());
		PushServlet.call_init(getReplicationItem());
		new RecursiveDirectoryBrowser(sourceDirectory, this);
		PushServlet.call_switch(replicationItem);
	}

	private void setTotalSize(long size) {
		rwl.w.lock();
		try {
			totalSize = size;
		} finally {
			rwl.w.unlock();
		}
	}

	private void addCheckedSize(long length) {
		rwl.w.lock();
		try {
			checkedSize += length;
		} finally {
			rwl.w.unlock();
		}
	}

	private boolean checkFilePush(File file) throws IOException {
		if (!checkDirPush(file))
			return false;
		for (File fileNotPushed : filesNotPushed)
			if (file.equals(fileNotPushed))
				return false;
		return true;
	}

	private boolean checkDirPush(File dir) throws IOException {
		for (File dirNotPushed : dirsNotPushed) {
			if (dir.equals(dirNotPushed))
				return false;
			if (FileUtils.isSubDirectory(dirNotPushed, dir))
				return false;
		}
		return true;
	}

	public String getStatInfo() {
		return getProgress() + "% completed - " + getFilesSent()
				+ " file(s) sent - "
				+ FileUtils.byteCountToDisplaySize(getBytesSent()) + " sent";
	}

	@Override
	public void file(File file) throws SearchLibException {
		try {
			ReplicationItem replicationItem = getReplicationItem();
			long length = file.length();
			if (file.isFile()) {
				if (checkFilePush(file)) {
					if (!PushServlet.call_file_exist(client, replicationItem,
							file)) {
						PushServlet.call_file(client, replicationItem, file);
						incFilesSent(length);
					}
				}
			} else {
				if (checkDirPush(file))
					PushServlet.call_directory(client, replicationItem, file);
			}
			addCheckedSize(length);
			if (infoCallback != null) {
				infoCallback.setInfo(getStatInfo());
				if (infoCallback instanceof TaskLog)
					if (((TaskLog) infoCallback).isAbortRequested())
						throw new SearchLibException.AbortException();
			}
		} catch (IllegalStateException e) {
			throw new SearchLibException(e);
		} catch (IOException e) {
			throw new SearchLibException(e);
		}
	}
}
