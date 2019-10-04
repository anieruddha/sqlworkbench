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
package workbench.sql.wbcommands;


import java.sql.SQLException;

import workbench.resource.ResourceMgr;

import workbench.db.DbSwitcher;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ExceptionUtil;

/**
 * A command to switch the current database by creating a new connection.
 *
 * @see DbSwitcher#switchDatabase(WbConnection, String)
 * @author  Thomas Kellerer
 */
public class WbSwitchDB
	extends SqlCommand
{
	public static final String VERB = "WbSwitchDB";

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(sql);
		try
		{
			// everything after the WbSwitchDB command is the database name
			String dbName = getCommandLine(sql);

      DbSwitcher switcher = DbSwitcher.Factory.createDatabaseSwitcher(currentConnection);
      if (switcher != null)
      {
        switcher.switchDatabase(currentConnection, dbName);

        String msg = ResourceMgr.getFormattedString("MsgCatalogChanged", ResourceMgr.getString("TxtDatabase"), dbName);
        result.addMessage(msg);
        result.setSuccess();
      }
			else
      {
        result.setFailure();
      }
		}
		catch (Exception e)
		{
			result.addMessageByKey("MsgExecuteError");
			result.addErrorMessage(ExceptionUtil.getAllExceptions(e).toString());
		}
		finally
		{
			this.done();
		}

		return result;
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

  @Override
  public boolean isWbCommand()
  {
    return true;
  }
}
