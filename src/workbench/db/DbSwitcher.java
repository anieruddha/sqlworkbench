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
package workbench.db;

import java.sql.SQLException;
import java.util.List;

import workbench.db.oracle.OracleDatabaseSwitcher;
import workbench.db.postgres.PostgresDatabaseSwitcher;

/**
 *
 * @author Thomas Kellerer
 */
public interface DbSwitcher
{
  boolean supportsSwitching(WbConnection connection);
  boolean needsReconnect();
  boolean switchDatabase(WbConnection connection, String dbName)
    throws SQLException;

  String getUrlForDatabase(String originalUrl, String dbName);
  List<String> getAvailableDatabases(WbConnection connection);
  String getCurrentDatabase(WbConnection connection);

  public static class Factory
  {
    public static DbSwitcher createDatabaseSwitcher(WbConnection conn)
    {
      if (conn == null) return null;

      DBID db = DBID.fromConnection(conn);
      switch (db)
      {
        case Postgres:
          return new PostgresDatabaseSwitcher();
        case Oracle:
          return new OracleDatabaseSwitcher();
        case SQL_Server:
        case MySQL:
        case MariaDB:
          return new JdbcDbSwitcher();
        default:
          return null;
      }
    }
  }


}
