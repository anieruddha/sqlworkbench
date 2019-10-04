/*
 * OracleRowDataReader.java
 *
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2019, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
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

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.TimeZone;

import workbench.log.LogMgr;

import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;

public class OracleTZHandler
  implements TimestampTZHandler
{
  private Connection sqlConnection;
  private boolean useDefaultClassLoader;
  private Class tzClass;
  private Constructor tzConstructor;

  public OracleTZHandler(WbConnection conn, boolean useDefaultClassLoader)
  {
    this.useDefaultClassLoader = useDefaultClassLoader;

    sqlConnection = conn.getSqlConnection();

    try
    {
      tzClass = loadClass(conn, "oracle.sql.TIMESTAMPTZ");
      tzConstructor = tzClass.getConstructor(Connection.class, Timestamp.class, Calendar.class);
    }
    catch (Throwable t)
    {
      LogMgr.logWarning("OracleRowDataReader.initialize()", "Class oracle.sql.TIMESTAMPTZ not available!");
    }
  }

  @Override
  public Object convertTimestampTZ(Object input)
  {
    try
    {
      if (input instanceof ZonedDateTime)
      {
        ZonedDateTime zdt = (ZonedDateTime)input;
        TimeZone tz = TimeZone.getTimeZone(zdt.getZone());
        Calendar cal = Calendar.getInstance(tz);
        Timestamp ts = Timestamp.valueOf(zdt.toLocalDateTime());

        Object oraTZ = tzConstructor.newInstance(sqlConnection, ts, cal);
        return oraTZ;
      }

      if (input instanceof OffsetDateTime)
      {
        OffsetDateTime odt = (OffsetDateTime)input;
        TimeZone tz = TimeZone.getTimeZone(odt.getOffset().normalized());
        Calendar cal = Calendar.getInstance(tz);
        Timestamp ts = Timestamp.valueOf(odt.toLocalDateTime());

        Object oraTZ = tzConstructor.newInstance(sqlConnection, ts, cal);
        return oraTZ;
      }
    }
    catch (Throwable th)
    {
      LogMgr.logError("OracleTZHandler.convertTimestampTZ()", "Error converting input to TIMESTAMPTZ", th);
    }

    return input;
  }

  private Class loadClass(WbConnection conn, String className)
    throws ClassNotFoundException
  {
    if (useDefaultClassLoader)
    {
      return Class.forName(className);
    }
    return ConnectionMgr.getInstance().loadClassFromDriverLib(conn.getProfile(), className);
  }


}
