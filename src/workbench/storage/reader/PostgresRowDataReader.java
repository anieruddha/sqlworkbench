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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;

import workbench.db.WbConnection;

import workbench.storage.ResultInfo;

/**
 *
 * @author Thomas Kellerer
 */
class PostgresRowDataReader
  extends RowDataReader
{
  private boolean useJava8Time;

  PostgresRowDataReader(ResultInfo info, WbConnection conn)
  {
    super(info, conn);
    useJava8Time = TimestampTZHandler.Factory.supportsJava8Time(conn);
    if (useJava8Time)
    {
      useGetObjectForTimestamps = true;
      useGetObjectForTimestampTZ = true;
      useGetObjectForDates = true;
      LogMgr.logInfo(new CallerInfo(){}, "Using ZonedDateTime to read TIMESTAMP WITH TIME ZONE columns");
    }
  }

  @Override
  protected Object readTimeTZValue(ResultHolder rs, int column)
    throws SQLException
  {
    return rs.getTime(column);
  }

  @Override
  protected Object readTimestampTZValue(ResultHolder rs, int column)
    throws SQLException
  {
    if (useJava8Time)
    {
      return readTimeZoneInfo(rs, column);
    }
    return super.readTimestampTZValue(rs, column);
  }

  private ZonedDateTime readTimeZoneInfo(ResultHolder rs, int column)
    throws SQLException
  {
    OffsetDateTime odt = rs.getObject(column, OffsetDateTime.class);
    if (odt == null) return null;
    // This is how the JDBC returns Infinity values
    if (odt.equals(OffsetDateTime.MAX) || odt.equals(OffsetDateTime.MIN))
    {
      //TODO: is returning ZondedDateTime better,  or simply returning the OffsetDateTime directly?
      return odt.atZoneSimilarLocal(ZoneId.of("+0"));
    }
    return odt.atZoneSameInstant(ZoneId.systemDefault());
  }

}
