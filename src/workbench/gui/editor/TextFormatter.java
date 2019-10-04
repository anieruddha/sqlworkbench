/*
 * TextFormatter.java
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
package workbench.gui.editor;

import workbench.interfaces.SqlTextContainer;
import workbench.log.LogMgr;

import workbench.gui.WbSwingUtilities;

import workbench.sql.DelimiterDefinition;
import workbench.sql.formatter.SqlFormatter;
import workbench.sql.formatter.SqlFormatterFactory;
import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;
import workbench.sql.parser.ParserType;
import workbench.sql.parser.ScriptParser;

/**
 *
 * @author Thomas Kellerer
 */
public class TextFormatter
{
	private String dbId;

	public TextFormatter(String id)
	{
		this.dbId = id;
	}

	public void formatSql(SqlTextContainer editor, DelimiterDefinition alternateDelimiter)
  {
    String sql = editor.getSelectedStatement();

    SqlFormatter f = SqlFormatterFactory.createFormatter(dbId);

    String text = null;

    if (f.supportsMultipleStatements())
    {
      text = f.getFormattedSql(sql);
    }
    else
    {
      text = doFormat(sql, f, alternateDelimiter);
    }

    updateEditor(editor, text);
  }

  private void updateEditor(final SqlTextContainer editor, final String text)
  {
    WbSwingUtilities.invoke(() ->
    {
      if (editor.isTextSelected())
      {
        boolean editable = editor.isEditable();
        try
        {
          // the editor will refuse to execute setSelectedText() if it's not editable
          editor.setEditable(true);
          editor.setSelectedText(text);
        }
        finally
        {
          editor.setEditable(editable);
        }
      }
      else
      {
        // setText() is always allowed, even if the editor is not editable
        editor.setText(text);
      }
    });
  }

  private String doFormat(String sql, SqlFormatter formatter, DelimiterDefinition alternateDelimiter)
	{
		ScriptParser parser = new ScriptParser(ParserType.getTypeFromDBID(dbId));
		parser.setAlternateDelimiter(alternateDelimiter);
		parser.setReturnStartingWhitespace(true);
		parser.setScript(sql);

		int count = parser.getSize();
		if (count < 1) return null;

		StringBuilder newSql = new StringBuilder(sql.length() + 100);

		for (int i=0; i < count; i++)
		{
			String command = parser.getCommand(i);

			DelimiterDefinition delimiter = parser.getDelimiterUsed(i);

      boolean isEmpty = isEmpty(command);

			// no need to format "empty" strings
			if (isEmpty)
			{
				newSql.append(command);
				continue;
			}

			try
			{
				String formattedSql = formatter.getFormattedSql(command);
        if (i > 0)
        {
  				// add a blank line between the statements, but not for the last one
					newSql.append('\n');
        }
				newSql.append(formattedSql.trim());
				if (delimiter != null && !isEmpty)
				{
					if (delimiter.isSingleLine())
					{
						newSql.append('\n');
					}
					newSql.append(delimiter.getDelimiter());
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("EditorPanel.reformatSql()", "Error when formatting SQL", e);
			}
		}

		if (newSql.length() == 0) return null;
    if (newSql.charAt(newSql.length() - 1) != '\n')
    {
      newSql.append('\n');
    }
		return newSql.toString();
	}

	private boolean isEmpty(String sql)
	{
		SQLLexer lexer = SQLLexerFactory.createLexerForDbId(dbId, sql);
		SQLToken token = lexer.getNextToken(false, false);
		return token == null;
	}

}
