/*
 * SpHelpTextRunner.java
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
package workbench.db.mssql;

import java.sql.ResultSet;
import java.sql.Statement;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SpHelpTextRunner
{

  public CharSequence getSource(WbConnection connection, String dbName, String schemaName, String objectName)
  {
    String currentDb = connection.getCurrentCatalog();
    CharSequence sql = null;

    boolean changeCatalog = StringUtil.stringsAreNotEqual(currentDb, dbName) && StringUtil.isNonBlank(dbName);
    Statement stmt = null;
    ResultSet rs = null;

    String query;

    if (StringUtil.isBlank(schemaName))
    {
      query = "sp_helptext [" + objectName + "]";
    }
    else
    {
      query = "sp_helptext [" + schemaName + "." + objectName + "]";
    }

    try
    {
      if (changeCatalog)
      {
        SqlServerUtil.changeDatabase(connection, dbName);
      }
      stmt = connection.createStatement();

      if (Settings.getInstance().getDebugMetadataSql())
      {
        LogMgr.logInfo("SpHelpTextRunner.getSource()", "Retrieving view definition using query: " + query);
      }

      boolean hasResult = stmt.execute(query);

      if (hasResult)
      {
        rs = stmt.getResultSet();
        StringBuilder source = new StringBuilder(1000);
        while (rs.next())
        {
          String line = rs.getString(1);
          if (line != null)
          {
            source.append(rs.getString(1));
          }
        }
        StringUtil.trimTrailingWhitespace(source);
        sql = source;
      }
    }
    catch (Exception ex)
    {
      LogMgr.logError("SpHelpTextRunner.getSource()", "Could not retrieve object source using:\n" + query, ex);
      sql = ex.getMessage();
    }
    finally
    {
      if (changeCatalog)
      {
        SqlServerUtil.changeDatabase(connection, currentDb);
      }
      SqlUtil.closeAll(rs, stmt);
    }
    return sql;
  }

}
