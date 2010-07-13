/**   
 * License Agreement for Jaeksoft OpenSearchServer
 *
 * Copyright (C) 2010 Emmanuel Keller / Jaeksoft
 * 
 * http://www.open-search-server.com
 * 
 * This file is part of Jaeksoft OpenSearchServer.
 *
 * Jaeksoft OpenSearchServer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Jaeksoft OpenSearchServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Jaeksoft OpenSearchServer.  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.jaeksoft.searchlib.web.controller.crawler.web;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.lucene.queryParser.ParseException;

import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.crawler.web.process.WebCrawlThread;
import com.jaeksoft.searchlib.function.expression.SyntaxError;
import com.jaeksoft.searchlib.web.controller.CommonController;

public class ManualWebCrawlController extends CommonController {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4805144758692963031L;

	private String url;

	private WebCrawlThread currentCrawlThread;

	public ManualWebCrawlController() throws SearchLibException {
		super();
		url = null;
		currentCrawlThread = null;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

	/**
	 * @param url
	 *            the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	public WebCrawlThread getCrawlThread() {
		synchronized (this) {
			return currentCrawlThread;
		}
	}

	public void onCrawl() throws SearchLibException, ParseException,
			IOException, SyntaxError, URISyntaxException,
			ClassNotFoundException, InterruptedException,
			InstantiationException, IllegalAccessException {
		synchronized (this) {
			if (currentCrawlThread != null && currentCrawlThread.isRunning())
				throw new SearchLibException("A crawl is already running");
			currentCrawlThread = getClient().getWebCrawlMaster().manualCrawl(
					new URL(url));
			currentCrawlThread.waitForStart(60);
			reloadPage();
		}
	}

	public boolean isCrawlThread() {
		synchronized (this) {
			return currentCrawlThread != null;
		}
	}

	public void onTimer() {
		reloadPage();
	}

	public boolean isRefresh() {
		synchronized (this) {
			if (currentCrawlThread == null)
				return false;
			return currentCrawlThread.isRunning();
		}
	}
}