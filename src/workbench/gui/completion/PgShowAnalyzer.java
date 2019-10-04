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

import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PgShowAnalyzer
	extends BaseAnalyzer
{
  public PgShowAnalyzer(WbConnection conn, String statement, int cursorPos)
  {
    super(conn, statement, cursorPos);
  }

  @Override
  protected void checkContext()
  {
    // Postgres' show and set commands have no additional options
    // so the auto-completion should be invoked if the cursor is after the keyword
    if (this.cursorPos > verb.length())
    {
      context = CONTEXT_VALUE_LIST;
    }
    else
    {
      context = NO_CONTEXT;
    }
  }

  @Override
  protected void buildResult()
  {
    if (context == CONTEXT_VALUE_LIST)
    {
      DataStore names = SqlUtil.getResult(dbConnection, "select name from pg_settings order by name", true);
      this.elements = new ArrayList(names.getRowCount());
      for (int row=0; row < names.getRowCount(); row++)
      {
        this.elements.add(names.getValueAsString(row, 0));
      }
    }
  }

  @Override
  public boolean allowMultiSelection()
  {
    return false;
  }

}
