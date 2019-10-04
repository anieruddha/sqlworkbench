/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2019, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.sql-workbench.eu/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.eu
 */
package workbench.gui.tools;

import workbench.resource.ResourceMgr;

import workbench.db.ColumnIdentifier;

/**
 *
 * @author Thomas Kellerer
 */

class ColumnMapRow
{
	private ColumnIdentifier source;
	private ColumnIdentifier target;

	void setTarget(ColumnIdentifier id)
	{
		this.target = id;
	}

	void setSource(ColumnIdentifier o)
	{
		this.source = o;
	}

	ColumnIdentifier getSource()
	{
		return this.source;
	}

	ColumnIdentifier getTarget()
	{
		return this.target;
	}

	@Override
	public String toString()
	{
		return "Mapping " + source + " -> " + target;
	}
}

class SkipColumnIndicator
{
	private final String display = ResourceMgr.getString("LblDPDoNotCopyColumns");

	@Override
	public String toString()
	{
		return display;
	}

}
