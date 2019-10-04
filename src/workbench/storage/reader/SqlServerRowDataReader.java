/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2017 Thomas Kellerer.
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
package workbench.storage.reader;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import workbench.db.JdbcUtils;
import workbench.db.WbConnection;

import workbench.storage.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerRowDataReader
  extends RowDataReader
{
  private final SqlServerTZHandler handler;

  public SqlServerRowDataReader(ResultInfo info, WbConnection conn)
  {
    this(info, conn, false);
  }

  protected SqlServerRowDataReader(ResultInfo info, WbConnection conn, boolean useSystemCL)
  {
    super(info, conn);

    boolean is71 = JdbcUtils.hasMiniumDriverVersion(conn, "7.1");
    useGetObjectForTimestamps = is71;
    useGetObjectForDates = is71;

    if (!useGetObjectForTimestamps && TimestampTZHandler.Factory.supportsJava8Time(conn))
    {
      handler = new SqlServerTZHandler(conn, useSystemCL);
    }
    else
    {
      handler = null;
    }
  }

  @Override
  protected Object readTimestampValue(ResultHolder rs, int column)
    throws SQLException
  {
    if (useGetObjectForTimestamps)
    {
      return rs.getObject(column, LocalDateTime.class);
    }
    return super.readTimestampValue(rs, column);
  }

  @Override
  protected Object readTimestampTZValue(ResultHolder rs, int column)
    throws SQLException
  {
    if (useGetObjectForTimestamps)
    {
      return rs.getObject(column, OffsetDateTime.class);
    }
    if (handler == null)
    {
      return super.readTimestampTZValue(rs, column);
    }
    return handler.readOffsetDateTime(rs, column);
  }

}
