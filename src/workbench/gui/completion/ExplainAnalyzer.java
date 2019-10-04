/*
 * ExplainAnalyzer.java
 *
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2019, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     https://www.sql-workbench.eu/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.eu
 *
 */
package workbench.gui.completion;

import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public abstract class ExplainAnalyzer
	extends BaseAnalyzer
{

	public ExplainAnalyzer(WbConnection conn, String statement, int cursorPos)
	{
		super(conn, statement, cursorPos);
	}

	protected abstract int getStatementStart(String sql);

	protected String getExplainSql()
	{
		int statementStart = getStatementStart(sql);
		if (statementStart != Integer.MAX_VALUE)
		{
			return sql.substring(0, statementStart - 1);
		}
		return sql;
	}

	/**
	 * Return the statement to be explained, regardless of where the
	 * cursor position is.
	 */
	public String getExplainedStatement()
	{
		int statementStart = getStatementStart(this.sql);
		if (statementStart < sql.length())
		{
			return sql.substring(statementStart);
		}
		return null;
	}
}
