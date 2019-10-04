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

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import workbench.db.DBID;
import workbench.db.DbMetadata;
import workbench.db.JdbcUtils;
import workbench.db.WbConnection;

import workbench.util.WbDateFormatter;

/**
 *
 * @author Thomas Kellerer
 */
public interface TimestampTZHandler
{
  /**
   * Converts the input object to an instance suitable to be passed to a PreparedStatement.
   *
   * This is necessary as every JDBC seems to use a different strategy for timestamps with time zones.
   *
   * @param input the data to be converted
   * @return an instance of a class suitable for a PreparedStatement
   */
  Object convertTimestampTZ(Object input);

  /**
   * A dummy implementation that simply returns the input value.
   */
  public static final TimestampTZHandler DUMMY_HANDLER = (Object input) -> input;

  /**
   * An implementation that converts the input to an OffsetDateTime.
   */
  public static final TimestampTZHandler OFFSET_HANDLER = (Object value) ->
  {
    if (value == null) return value;

    if (value instanceof ZonedDateTime)
    {
      ZonedDateTime zdt = (ZonedDateTime)value;
      return zdt.toOffsetDateTime();
    }

    if (value instanceof Timestamp)
    {
      Timestamp ts = (Timestamp)value;
      return OffsetDateTime.of(ts.toLocalDateTime(), WbDateFormatter.getSystemDefaultOffset());
    }
    return value;
  };

  /**
   * An implementation that converts the input to a ZonedDateTime.
   */
  public static final TimestampTZHandler ZONE_HANDLER = (Object value) ->
  {
    if (value instanceof OffsetDateTime)
    {
      OffsetDateTime odt = (OffsetDateTime)value;
      return odt.toZonedDateTime();
    }

    if (value instanceof Timestamp)
    {
      Timestamp ts = (Timestamp)value;
      return ZonedDateTime.of(ts.toLocalDateTime(), ZoneId.systemDefault());
    }
    return value;
  };

  public static class Factory
  {
    private static final Map<DBID, String> DRIVER_VERSIONS = new HashMap<>();
    static
    {
      DRIVER_VERSIONS.put(DBID.Postgres, "42.0");
      DRIVER_VERSIONS.put(DBID.Oracle, "12.2");
      DRIVER_VERSIONS.put(DBID.SQL_Server, "4.0");
    }

    public static boolean supportsJava8Time(WbConnection conn)
    {
      if (conn == null) return false;

      String minVersion = DRIVER_VERSIONS.get(DBID.fromConnection(conn));
      return JdbcUtils.hasMiniumDriverVersion(conn, minVersion);
    }

    public static TimestampTZHandler getHandler(WbConnection conn)
    {
      if (conn == null) return DUMMY_HANDLER;
      if (!supportsJava8Time(conn)) return DUMMY_HANDLER;

      DbMetadata meta = conn.getMetadata();
      if (meta == null) return DUMMY_HANDLER;

      if (meta.isPostgres())
      {
        return OFFSET_HANDLER;
      }

      if (meta.isOracle())
      {
        return new OracleTZHandler(conn, false);
      }

      if (meta.isSqlServer())
      {
        return new SqlServerTZHandler(conn);
      }
      return DUMMY_HANDLER;
    }
  }

}
