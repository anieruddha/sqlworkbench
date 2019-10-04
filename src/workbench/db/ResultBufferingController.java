/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2019 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
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
package workbench.db;

import java.sql.SQLException;
import java.sql.Statement;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;

/**
 * A class to control the ResultSet buffering done by the JDBC driver.
 *
 * Currently this is only done for Postgres.
 *
 * @author Thomas Kellerer
 */
public class ResultBufferingController
{
  private final WbConnection dbConn;
  private boolean autocommitChanged;
  private boolean setFetchSize;
  private final int fetchSize = 100;

  public ResultBufferingController(WbConnection toCheck)
  {
    this.dbConn = toCheck;
  }

  public void restoreDriverBuffering()
  {
    if (autocommitChanged)
    {
      try
      {
        LogMgr.logDebug(new CallerInfo(){}, "Re-enabling on autocommit");
        this.dbConn.setAutoCommit(true);
      }
      catch (Exception ex)
      {
        // ignore
      }
    }
  }

  public void initializeStatement(Statement stmt)
    throws SQLException
  {
    if (stmt != null && this.setFetchSize)
    {
      stmt.setFetchSize(fetchSize);
    }
  }

  public void disableDriverBuffering()
  {
    if (dbConn.getDbSettings().autoDisableDriverBuffering() && JdbcUtils.checkPostgresBuffering(dbConn))
    {
      LogMgr.logInfo(new CallerInfo(){}, "Disabling auto commit and setting fetch size to avoid excessive buffering by the driver");
      try
      {
        // Switching from autocommit on to autocommit off is safe as no transaction can be active at this moment
        // Switching back at the end of the export is also safe as we did not do any updates during the export
        this.dbConn.setAutoCommit(false);
      }
      catch (Exception ex)
      {
        // ignore
      }
      this.autocommitChanged = true;
      this.setFetchSize = true;
    }
    else
    {
      this.autocommitChanged = false;
      this.setFetchSize = false;
    }
  }

}
