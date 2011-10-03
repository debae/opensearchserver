/**   
 * License Agreement for OpenSearchServer
 *
 * Copyright (C) 2010-2011 Emmanuel Keller / Jaeksoft
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

package com.jaeksoft.searchlib.parser;

import java.io.IOException;

import org.apache.poi.POIXMLProperties.CoreProperties;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.analysis.ClassPropertyEnum;

public class DocxParser extends Parser {

	private static ParserFieldEnum[] fl = { ParserFieldEnum.title,
			ParserFieldEnum.creator, ParserFieldEnum.subject,
			ParserFieldEnum.description, ParserFieldEnum.content,
			ParserFieldEnum.lang, ParserFieldEnum.lang_method,
			ParserFieldEnum.filename, ParserFieldEnum.content_type };

	public DocxParser() {
		super(fl);
	}

	@Override
	public void initProperties() throws SearchLibException {
		super.initProperties();
		addProperty(ClassPropertyEnum.SIZE_LIMIT, "0", null);
	}

	@Override
	protected void parseContent(LimitInputStream inputStream)
			throws IOException {

		XWPFDocument document = new XWPFDocument(inputStream);
		XWPFWordExtractor word = new XWPFWordExtractor(document);

		CoreProperties info = word.getCoreProperties();
		if (info != null) {
			addField(ParserFieldEnum.title, info.getTitle());
			addField(ParserFieldEnum.creator, info.getCreator());
			addField(ParserFieldEnum.subject, info.getSubject());
			addField(ParserFieldEnum.description, info.getDescription());
			addField(ParserFieldEnum.keywords, info.getKeywords());
		}

		String content = word.getText();
		addField(ParserFieldEnum.content, content.replaceAll("\\s+", " "));

		langDetection(10000, ParserFieldEnum.content);

	}

	@Override
	protected void parseContent(LimitReader reader) throws IOException {
		throw new IOException("Unsupported");
	}

}