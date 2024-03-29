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
import java.util.List;

import workbench.resource.ResourceMgr;

import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.TableSourceBuilderFactory;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 * Display the source code of a table.
 *
 * @author Thomas Kellerer
 */
public class WbTableSource
	extends SqlCommand
{
	public static final String VERB = "WbTableSource";

	public WbTableSource()
	{
		super();
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

    SourceTableArgument tableArg = new SourceTableArgument(args, currentConnection);

    List<TableIdentifier> tableList = tableArg.getTables();
    List<String> missingTables = tableArg.getMissingTables();

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

    for (TableIdentifier tbl : tableList)
    {
      TableDefinition tableDef = currentConnection.getMetadata().getTableDefinition(tbl);

      TableSourceBuilder reader = TableSourceBuilderFactory.getBuilder(currentConnection);
      String source = reader.getTableSource(tableDef.getTable(), tableDef.getColumns());
      if (source != null)
      {
        result.addMessage(source);
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
