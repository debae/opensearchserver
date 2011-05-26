/**   
 * License Agreement for Jaeksoft OpenSearchServer
 *
 * Copyright (C) 2008-2009 Emmanuel Keller / Jaeksoft
 * 
 * http://www.open-search-server.com
 * 
 * This file is part of Jaeksoft OpenSearchServer.
 *
 * Jaeksoft OpenSearchServer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Jaeksoft OpenSearchServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Jaeksoft OpenSearchServer. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.jaeksoft.searchlib.web.controller.query;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;

import com.jaeksoft.searchlib.Client;
import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.request.SearchRequest;
import com.jaeksoft.searchlib.schema.Field;
import com.jaeksoft.searchlib.schema.FieldList;
import com.jaeksoft.searchlib.schema.SchemaField;

public class MoreLikeThisController extends AbstractQueryController {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2872605532103762800L;

	private List<String> fieldsLeft;

	private List<String> stopWordsList;

	private String selectedField;

	public MoreLikeThisController() throws SearchLibException {
		super();
	}

	@Override
	protected void reset() throws SearchLibException {
		fieldsLeft = null;
		stopWordsList = null;
		selectedField = null;
	}

	public List<String> getFieldsLeft() throws SearchLibException {
		synchronized (this) {
			Client client = getClient();
			if (client == null)
				return null;
			SearchRequest request = getRequest();
			if (request == null)
				return null;
			if (fieldsLeft != null)
				return fieldsLeft;
			fieldsLeft = new ArrayList<String>();
			FieldList<Field> fields = request.getMoreLikeThisFieldList();
			for (SchemaField field : client.getSchema().getFieldList())
				if (fields.get(field.getName()) == null) {
					if (selectedField == null)
						selectedField = field.getName();
					fieldsLeft.add(field.getName());
				}
			return fieldsLeft;
		}
	}

	/**
	 * @return the selectedField
	 */
	public String getSelectedField() {
		return selectedField;
	}

	/**
	 * @param selectedField
	 *            the selectedField to set
	 */
	public void setSelectedField(String selectedField) {
		this.selectedField = selectedField;
	}

	public void onAddField() throws SearchLibException {
		getRequest().getMoreLikeThisFieldList().add(new Field(selectedField));
		reloadPage();
	}

	public void onRemoveField(Component component) throws SearchLibException {
		Field field = (Field) component.getParent().getAttribute("mltField");
		getRequest().getMoreLikeThisFieldList().remove(field);
		reloadPage();
	}

	public List<String> getStopWordsList() throws SearchLibException {
		synchronized (this) {
			Client client = getClient();
			if (client == null)
				return null;
			if (stopWordsList != null)
				return stopWordsList;
			stopWordsList = new ArrayList<String>();
			stopWordsList.add("");
			String[] list = client.getStopWordsManager().getList();
			if (list != null)
				for (String s : list)
					stopWordsList.add(s);
			return stopWordsList;
		}
	}

	@Override
	protected void eventSchemaChange() throws SearchLibException {
		reset();
		reloadPage();
	}

}