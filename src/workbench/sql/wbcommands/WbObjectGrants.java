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

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import workbench.resource.ResourceMgr;

import workbench.db.TableGrantReader;
import workbench.db.TableIdentifier;
import workbench.db.ViewGrantReader;

import workbench.storage.RowActionMonitor;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.ExceptionUtil;
import workbench.util.FileUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;


/**
 * Display the source code of a table.
 *
 * @author Thomas Kellerer
 */
public class WbObjectGrants
	extends SqlCommand
{
	public static final String VERB = "WbObjectGrants";

	public WbObjectGrants()
	{
		super();
    cmdLine = new ArgumentParser();
    cmdLine.addArgument(CommonArgs.ARG_TABLES);
    cmdLine.addArgument(CommonArgs.ARG_VIEWS);
    cmdLine.addArgument(CommonArgs.ARG_OUTPUT_DIR, ArgumentType.DirName);
    cmdLine.addArgument(CommonArgs.ARG_OUTPUT_FILE, ArgumentType.Filename);
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		String args = getCommandLine(sql);
		cmdLine.parse(args);

    if (displayHelp(result))
    {
      return result;
    }

		if (!cmdLine.hasArguments())
		{
			result.addErrorMessageByKey("ErrObjectGrantsWrongParam");
			return result;
		}

		if (cmdLine.hasUnknownArguments())
		{
			setUnknownMessage(result, cmdLine, ResourceMgr.getString("ErrObjectGrantsWrongParam"));
			return result;
		}

    if (this.rowMonitor != null)
    {
      this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
      this.rowMonitor.setCurrentObject(ResourceMgr.getString("MsgRetrievingTables"), -1, -1);
    }

    SourceTableArgument tableArg = new SourceTableArgument(cmdLine.getValue(CommonArgs.ARG_TABLES), currentConnection);

    List<TableIdentifier> tableList = tableArg.getTables();
    List<String> missingTables = tableArg.getMissingTables();

    String types[] = new String[] { currentConnection.getMetadata().getViewTypeName() };

    SourceTableArgument viewArgs = new SourceTableArgument(cmdLine.getValue(CommonArgs.ARG_VIEWS), null, null, types, currentConnection);
    List<TableIdentifier> viewList  = viewArgs.getTables();

    missingTables.addAll(viewArgs.getMissingTables());

    if (missingTables.size() > 0)
    {
      for (String tablename : missingTables)
      {
        result.addWarning(ResourceMgr.getFormattedString("ErrTableNotFound", tablename));
      }
      result.addMessageNewLine();

      if (tableList.isEmpty())
      {
        result.setFailure();
        return result;
      }
    }

    TableGrantReader reader = TableGrantReader.createReader(currentConnection);
    StringBuilder grantSql = new StringBuilder(tableList.size() * 50);

    if (this.rowMonitor != null)
    {
      this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PROCESS);
    }

    ViewGrantReader viewReader = ViewGrantReader.createViewGrantReader(currentConnection);

    int totalCount = tableList.size() + viewList.size();
    int currentObject = 1;

    for (TableIdentifier tbl : tableList)
    {
      if (this.rowMonitor != null)
      {
        this.rowMonitor.setCurrentObject(tbl.getTableExpression(), currentObject, totalCount);
      }
      StringBuilder source = reader.getTableGrantSource(currentConnection, tbl);

      if (StringUtil.isNonBlank(source))
      {
        grantSql.append(source);
      }
      currentObject ++;
    }

    for (TableIdentifier tbl : viewList)
    {
      if (this.rowMonitor != null)
      {
        this.rowMonitor.setCurrentObject(tbl.getTableExpression(), currentObject, totalCount);
      }
      StringBuilder source = viewReader.getViewGrantSource(currentConnection, tbl);

      if (StringUtil.isNonBlank(source))
      {
        grantSql.append(source);
      }
      currentObject ++;
    }

    if (this.rowMonitor != null)
    {
      this.rowMonitor.jobFinished();
    }

		String file = cmdLine.getValue(CommonArgs.ARG_OUTPUT_FILE, null);
    if (file == null)
    {
      result.addMessage(grantSql);
    }
		else
		{
			WbFile output = new WbFile(file);
			try
			{
				FileUtil.writeString(output, grantSql.toString());
				result.addMessageByKey("MsgScriptWritten", output.getFullPath());
				result.setSuccess();
			}
			catch (IOException io)
			{
				result.addErrorMessageByKey("ErrFileCreate", ExceptionUtil.getDisplay(io));
			}
		}
    result.setSuccess();
		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}
}
