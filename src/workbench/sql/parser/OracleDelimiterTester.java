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
package workbench.sql.parser;

import java.util.Set;

import workbench.resource.Settings;

import workbench.db.oracle.OracleUtils;

import workbench.sql.DelimiterDefinition;
import workbench.sql.lexer.SQLToken;

import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleDelimiterTester
	implements DelimiterTester
{
	private DelimiterDefinition alternateDelimiter = DelimiterDefinition.DEFAULT_ORA_DELIMITER;
	private boolean useAlternateDelimiter;
	private final Set<String> blockStart = CollectionUtil.caseInsensitiveSet("BEGIN", "DECLARE");
	private final Set<String> keywords = CollectionUtil.caseInsensitiveSet("CREATE", "CREATE OR REPLACE", "WITH");
	private final Set<String> singleLineCommands = CollectionUtil.caseInsensitiveSet("WHENEVER", "ECHO", "DESC", "DESCRIBE", "SET", "SHOW", "PROMPT");
	private final Set<String> types = CollectionUtil.caseInsensitiveSet("FUNCTION", "LIBRARY", "PACKAGE", "PACKAGE BODY", "PROCEDURE", "TRIGGER", "TYPE", "TYPE BODY");
	private final Set<String> plsqlCTETypes = CollectionUtil.caseInsensitiveSet("FUNCTION", "PROCEDURE");

	private SQLToken lastToken;
	private boolean isPLSQLStatement;
	private DelimiterDefinition defaultDelimiter = DelimiterDefinition.STANDARD_DELIMITER;

	public OracleDelimiterTester()
	{
		boolean typesLikeSqlPlus = Settings.getInstance().getBoolProperty("workbench.oracle.sql.parser.types.altdelimiter", true);
		setRequireAlternateDelimiterForTypes(typesLikeSqlPlus);
	}

	public void setRequireAlternateDelimiterForTypes(boolean flag)
	{
		if (flag)
		{
			types.add("TYPE");
		}
		else
		{
			types.remove("TYPE");
		}
	}

	@Override
	public boolean supportsMixedDelimiters()
	{
		return true;
	}

	@Override
	public void setDelimiter(DelimiterDefinition delimiter)
	{
		defaultDelimiter = delimiter;
	}

	@Override
	public void setAlternateDelimiter(DelimiterDefinition delimiter)
	{
		alternateDelimiter = delimiter.createCopy();
	}

	public DelimiterDefinition getAlternateDelimiter()
	{
		return alternateDelimiter;
	}

  public boolean isPLSQLHint(SQLToken token)
  {
    if (token == null) return false;
    if (!token.isComment()) return false;
    String content = token.getText();
    if (!content.startsWith("/*+") && !content.startsWith("--+")) return false;
    return content.toLowerCase().contains("with_plsql");
  }

  private boolean isCTEWithPLSQL(SQLToken current, SQLToken previous)
  {
    if (current == null || previous == null) return false;

    if (!"WITH".equalsIgnoreCase(previous.getText())) return false;

    return plsqlCTETypes.contains(current.getText());
  }

	@Override
	public void currentToken(SQLToken token, boolean isStartOfStatement)
	{
		if (token == null) return;
    boolean isPLSQLHint = false;
		if (token.isComment())
    {
      isPLSQLHint = isPLSQLHint(token);
      if (!isPLSQLHint) return;
    }

    if (isPLSQLHint || isCTEWithPLSQL(token, lastToken))
    {
      useAlternateDelimiter = true;
    }
    else if (useAlternateDelimiter && lastToken != null)
		{
			if (lastToken.getText().equals(alternateDelimiter.getDelimiter()) && isStartOfStatement)
			{
				useAlternateDelimiter = false;
			}
		}
		else if (blockStart.contains(token.getText()) && lastToken == null)
		{
			useAlternateDelimiter = true;
		}
		else if (lastToken != null && isPLSQLStatement)
		{
			useAlternateDelimiter = (types.contains(token.getText()) && keywords.contains(lastToken.getText()));
		}

		if (!token.isWhiteSpace() && !token.getContents().equalsIgnoreCase(OracleUtils.KEYWORD_EDITIONABLE))
		{
			lastToken = token;
		}

		if (isStartOfStatement)
		{
			this.isPLSQLStatement = keywords.contains(token.getText());
		}
	}

	@Override
	public DelimiterDefinition getCurrentDelimiter()
	{
		if (useAlternateDelimiter)
		{
			return alternateDelimiter;
		}
		return defaultDelimiter;
	}

	@Override
	public void statementFinished()
	{
		useAlternateDelimiter = false;
		lastToken = null;
		isPLSQLStatement = false;
	}

	@Override
	public boolean supportsSingleLineStatements()
	{
		return true;
	}

	@Override
	public boolean isSingleLineStatement(SQLToken token, boolean isStartOfLine)
	{
		if (token == null) return false;

		if (isStartOfLine && !token.isWhiteSpace())
		{
			String text = token.getText();
			if (text.charAt(0) == '@')
			{
				return true;
			}
			if (singleLineCommands.contains(text))
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public void lineEnd()
	{
	}

}
