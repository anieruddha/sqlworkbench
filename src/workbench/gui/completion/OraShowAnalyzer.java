/*
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;
import workbench.sql.wbcommands.WbOraShow;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OraShowAnalyzer
  extends BaseAnalyzer
{
  private boolean showSpParameters;

  public OraShowAnalyzer(WbConnection conn, String statement, int cursorPos)
  {
    super(conn, statement, cursorPos);
  }

  @Override
  protected void checkContext()
  {
    context = CONTEXT_KW_LIST;
    showSpParameters = false;

    int parameterPos = -1;

		SQLLexer lexer = SQLLexerFactory.createLexer(dbConnection, this.sql);
		SQLToken token = lexer.getNextToken(false, false);

    while (token != null)
    {
      String v = token.getContents().toLowerCase();
      if (v.startsWith("param") || v.startsWith("spparam"))
      {
        parameterPos = token.getCharEnd();
        showSpParameters = v.startsWith("spparm");
      }
      token = lexer.getNextToken(false, false);
    }

    if (parameterPos > -1 && this.cursorPos >= parameterPos)
    {
      context = CONTEXT_VALUE_LIST;
    }
  }

  @Override
  protected void buildResult()
  {
    if (context == CONTEXT_VALUE_LIST)
    {
      String nameQuery;
      if (showSpParameters)
      {
        nameQuery = "select name from v$spparameter order by lower(name)";
      }
      else
      {
        nameQuery = "select name from v$parameter order by lower(name)";
      }

      DataStore names = SqlUtil.getResult(dbConnection, nameQuery, false);
      this.elements = new ArrayList(names.getRowCount());
      for (int row = 0; row < names.getRowCount(); row++)
      {
        this.elements.add(names.getValueAsString(row, 0));
      }
    }
    else if (context == CONTEXT_KW_LIST)
    {
      this.elements = new ArrayList(10);
      List<String> options = WbOraShow.getOptions();
      Collections.sort(options);
      elements.addAll(options);
    }
  }

}
