/*
 * CreateAnalyzer.java
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

import java.util.Set;

import workbench.log.LogMgr;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;

import workbench.util.CollectionUtil;

/**
 * Analyze a CREATE INDEX statement to provide completion for tables and columns.
 *
 * @author Thomas Kellerer
 * @see DdlAnalyzer
 * @see AlterTableAnalyzer
 */
public class CreateAnalyzer
  extends BaseAnalyzer
{

  private final Set<String> nameTerminals = CollectionUtil.caseInsensitiveSet("USING", "CLUSTER");
  private final Set<String> createTerminal = CollectionUtil.caseInsensitiveSet("INCLUDE", "WITH", "WHERE");

  public CreateAnalyzer(WbConnection conn, String statement, int cursorPos)
  {
    super(conn, statement, cursorPos);
  }

  @Override
  protected void checkContext()
  {
    SQLLexer lexer = SQLLexerFactory.createLexer(dbConnection, this.sql);

    boolean isCreateIndex = false;
    boolean showColumns = false;
    boolean showTables = false;
    int tableStartPos = -1;
    int tableEndPos = -1;
    int tokenCount = 0;
    boolean afterCreate = true;
    int bracketCount = 0;

    try
    {
      SQLToken token = lexer.getNextToken(false, false);
      while (token != null)
      {
        final String t = token.getContents();
        tokenCount++;
        if (tokenCount == 2)
        {
          afterCreate = (token.getCharBegin() > this.cursorPos);
        }

        if (isCreateIndex)
        {
          if ("ON".equalsIgnoreCase(t))
          {
            if (this.cursorPos > token.getCharEnd())
            {
              showTables = true;
              showColumns = false;
            }
            tableStartPos = token.getCharEnd();
          }
          else if (nameTerminals.contains(t))
          {
            // If an index type is specified in Postgres, the table name ends
            // right before the USING keyword
            // TODO: maybe show the available index types for Postgres in auto-completion
            tableEndPos = token.getCharBegin();
          }
          else if ("(".equals(t))
          {
            bracketCount++;
            if (bracketCount == 1)
            {
              if (tableEndPos == -1)
              {
                tableEndPos = token.getCharBegin();
              }
              if (this.cursorPos >= token.getCharBegin())
              {
                showTables = false;
                showColumns = true;
              }
            }
          }
          else if (")".equals(t))
          {
            bracketCount--;
            if (bracketCount == 0 && this.cursorPos > token.getCharBegin())
            {
              showTables = false;
              showColumns = false;
            }
          }
        }
        else if ("INDEX".equalsIgnoreCase(t))
        {
          isCreateIndex = true;
        }

        token = lexer.getNextToken(false, false);
      }
    }
    catch (Exception e)
    {
      LogMgr.logError("CreateAnalyzer", "Error parsing SQL", e);
    }

    if (showTables)
    {
      context = CONTEXT_TABLE_LIST;
      this.schemaForTableList = getSchemaFromCurrentWord();
    }
    else if (showColumns)
    {
      context = CONTEXT_COLUMN_LIST;
      if (tableEndPos == -1) tableEndPos = this.sql.length() - 1;
      String table = this.sql.substring(tableStartPos, tableEndPos).trim();
      this.tableForColumnList = new TableIdentifier(table, dbConnection);
    }
    else if (afterCreate)
    {
      context = CONTEXT_KW_LIST;
      this.keywordFile = DdlAnalyzer.DDL_TYPES_FILE;
    }
  }

}
