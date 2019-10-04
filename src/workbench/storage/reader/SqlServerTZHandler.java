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

import java.lang.reflect.Method;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import workbench.log.LogMgr;

import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerTZHandler
  implements TimestampTZHandler
{
  private static final String MS_DTO_CLASS = "microsoft.sql.DateTimeOffset";
  private boolean useSystemClassloader;

  private Method getOffset;
  private Method getTimestamp;
  private Method valueOf;

  public SqlServerTZHandler(WbConnection conn)
  {
    this(conn, false);
  }

  public SqlServerTZHandler(WbConnection conn, boolean useDefaultClassLoader)
  {
    useSystemClassloader = useDefaultClassLoader;
    init(conn);
  }

  public Object readOffsetDateTime(ResultHolder rs, int column)
    throws SQLException
  {
    Object obj = rs.getObject(column);
    if (obj == null) return null;

    if (getTimestamp == null || getOffset == null) return obj;

    String clsName = obj.getClass().getName();
    if (MS_DTO_CLASS.equals(clsName))
    {
      try
      {
        Timestamp ts = (Timestamp)getTimestamp.invoke(obj);
        int offset = 0;
        Integer offsetValue = (Integer)getOffset.invoke(obj);
        if (offsetValue != null)
        {
          offset = offsetValue.intValue();
        }
        ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(offset * 60);
        OffsetDateTime odt = OffsetDateTime.of(ts.toLocalDateTime(), zoneOffset);
        return odt;
      }
      catch (Throwable th)
      {
        // ignore
      }
    }
    return obj;
  }

  @Override
  public Object convertTimestampTZ(Object input)
  {
    if (input == null) return input;
    if (valueOf == null) return input;

    java.sql.Timestamp ts = null;
    int offset = Integer.MIN_VALUE;

    if (input instanceof OffsetDateTime)
    {
      OffsetDateTime odt = (OffsetDateTime)input;
      offset = odt.getOffset().getTotalSeconds() * 60;
      ts = java.sql.Timestamp.valueOf(odt.toLocalDateTime());
    }

    if (input instanceof ZonedDateTime)
    {
      ZonedDateTime zdt = (ZonedDateTime)input;
      ts = java.sql.Timestamp.valueOf(zdt.toLocalDateTime());
      offset = zdt.getOffset().getTotalSeconds() * 60;
    }

    if (ts != null && offset != Integer.MIN_VALUE)
    {
      try
      {
        return valueOf.invoke(null, ts, offset);
      }
      catch (Throwable ex)
      {
        // ignore, return the original value
      }
    }
    return input;
  }

  private void init(WbConnection conn)
  {
    try
    {
      Class msDTO = loadClass(conn, MS_DTO_CLASS);
      getTimestamp = msDTO.getMethod("getTimestamp", (Class[])null);
      getOffset = msDTO.getMethod("getMinutesOffset", (Class[])null);
      valueOf = msDTO.getMethod("valueOf", java.sql.Timestamp.class, int.class);
    }
    catch (Throwable t)
    {
      LogMgr.logWarning("SqlServerRowDataReader.initialize()", "Class microsoft.sql.DateTimeOffset not available!");
    }
  }

  private Class loadClass(WbConnection conn, String className)
    throws ClassNotFoundException
  {
    if (useSystemClassloader)
    {
      return Class.forName(className);
    }
    return ConnectionMgr.getInstance().loadClassFromDriverLib(conn.getProfile(), className);
  }
}
