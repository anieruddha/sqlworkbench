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
package workbench.db.clickhouse;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DefaultViewReader;
import workbench.db.DropType;
import workbench.db.NoConfigException;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;

import workbench.sql.formatter.SqlFormatter;
import workbench.sql.formatter.SqlFormatterFactory;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ClickhouseViewReader
  extends DefaultViewReader
{

  public ClickhouseViewReader(WbConnection con)
  {
    super(con);
  }

  @Override
  protected CharSequence createFullViewSource(TableDefinition view, DropType dropType, boolean includeCommit)
    throws SQLException, NoConfigException
  {
    String source = null;
    Statement stmt = null;
    ResultSet rs = null;

    try
    {
      String viewName = view.getTable().getFullyQualifiedName(connection);
      stmt = connection.createStatementForQuery();
      String sql = "show create table " + viewName;

      if (Settings.getInstance().getDebugMetadataSql())
      {
        LogMgr.logInfo("ClickhouseViewReader.createFullViewSource()", "Retrieving view source using using:\n" + sql);
      }

      rs = stmt.executeQuery(sql);

      if (rs.next())
      {
        source = rs.getString(1);
      }

      if (source != null && connection.getDbSettings().getFormatViewSource())
      {
        SqlFormatter formatter = SqlFormatterFactory.createFormatter(connection);
        source = formatter.getFormattedSql(source);
      }

      if (dropType != DropType.none && source != null)
      {
        source  = "drop table " + viewName + ";\n\n" + source;
      }
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }

    return source;
  }
}
