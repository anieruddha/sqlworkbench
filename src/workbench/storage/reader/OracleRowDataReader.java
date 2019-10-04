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

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.TimeZone;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;

import workbench.db.ConnectionMgr;
import workbench.db.JdbcUtils;
import workbench.db.WbConnection;

import workbench.storage.ResultInfo;

/**
 * A class to read the value of TIMESTAMP WITH TIME ZONE and TIMESAMP WITH LOCAL TIME ZONE columns.
 *
 * @author Thomas Kellerer
 */
public class OracleRowDataReader
  extends RowDataReader
{
  private Method stringValueTZ;
  private Method offsetDateTimeValueTZ;
  private Method getTimeZoneTZ;
  private Method localDateTimeValueLTZ;
  private Method timestampValue;
  private Method timestampValueLTZ;

  private Connection sqlConnection;
  private DateTimeFormatter tsParser;
  private boolean useDefaultClassLoader;
  private Calendar sessionTimezone;

  public OracleRowDataReader(ResultInfo info, WbConnection conn)
    throws ClassNotFoundException
  {
    this(info, conn, false);
  }

  public OracleRowDataReader(ResultInfo info, WbConnection conn, boolean useDefaultClassLoader)
    throws ClassNotFoundException
  {
    super(info, conn);
    this.useDefaultClassLoader = useDefaultClassLoader;

    sqlConnection = conn.getSqlConnection();

    final CallerInfo ci = new CallerInfo(){};
    // The tsParser is needed for pre 11.2 drivers
    // In that case the String value returned by TIMESTAMPTZ.stringValue() is parsed
    // Starting with the 11.2 driver, TIMESTAMPTZ can directly be converted
    // into an OffsetDateTime value without parsing
    if (JdbcUtils.hasMiniumDriverVersion(conn, "11.2"))
    {
      DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder().
        appendPattern("yyyy-MM-dd HH:mm:ss").
        appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true).
        optionalStart().
          appendLiteral(' ').
          appendZoneOrOffsetId().
        optionalEnd();
      tsParser = builder.toFormatter().withResolverStyle(ResolverStyle.SMART);
    }
    else
    {
      // The 11.1 driver returns values with one or two digits for nearly all parts.
      // e.g.: "2017-11-1 4.15.0.0 Europe/Berlin" or "2017-4-8 16.5.30.0 Europe/Berlin"
      // A value with one or two digits can't be defined using a pattern
      DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder().
        appendValue(ChronoField.YEAR).
        appendLiteral('-').
        appendValue(ChronoField.MONTH_OF_YEAR,1,2,SignStyle.NEVER).
        appendLiteral('-').
        appendValue(ChronoField.DAY_OF_MONTH,1,2,SignStyle.NEVER).
        appendLiteral(' ').
        appendValue(ChronoField.HOUR_OF_DAY,1,2,SignStyle.NEVER).
        appendLiteral('.').
        appendValue(ChronoField.MINUTE_OF_HOUR,1,2,SignStyle.NEVER).
        appendLiteral('.').
        appendValue(ChronoField.SECOND_OF_MINUTE,1,2,SignStyle.NEVER).
        appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true).
        optionalStart().
          appendLiteral(' ').
          appendZoneOrOffsetId().
        optionalEnd();
      tsParser = builder.toFormatter().withResolverStyle(ResolverStyle.LENIENT);
    }

    // we cannot have any hardcoded references to the Oracle classes
    // because that will throw a ClassNotFoundException as those classes were loaded through a different class loader.
    // Therefor I need to use reflection to access the Oracle specific methods
    try
    {
      Class oraTS = loadClass(conn, "oracle.sql.TIMESTAMP");
      timestampValue = oraTS.getMethod("timestampValue", (Class[])null);
    }
    catch (Throwable t)
    {
      LogMgr.logWarning(ci, "Could not access oracle.sql.TIMESTAMP", t);
    }

    try
    {
      Class tzClass = loadClass(conn, "oracle.sql.TIMESTAMPTZ");
      stringValueTZ = tzClass.getMethod("stringValue", java.sql.Connection.class);
    }
    catch (Throwable t)
    {
      LogMgr.logWarning(ci, "Could not access oracle.sql.TIMESTAMPTZ", t);
    }

    try
    {
      Class tzlClass = loadClass(conn, "oracle.sql.TIMESTAMPLTZ");
      timestampValueLTZ = tzlClass.getMethod("timestampValue", java.sql.Connection.class, Calendar.class);
    }
    catch (Throwable t)
    {
      LogMgr.logWarning(ci, "Class oracle.sql.TIMESTAMPLTZ not available!", t);
    }

    if (JdbcUtils.hasMiniumDriverVersion(conn, "12.2"))
    {
      try
      {
        Class tzClass = loadClass(conn, "oracle.sql.TIMESTAMPTZ");
        offsetDateTimeValueTZ = tzClass.getMethod("offsetDateTimeValue", java.sql.Connection.class);
        getTimeZoneTZ = tzClass.getMethod("getTimeZone", (Class[])null);

        Class tzlClass = loadClass(conn, "oracle.sql.TIMESTAMPLTZ");
        localDateTimeValueLTZ = tzlClass.getMethod("localDateTimeValue", java.sql.Connection.class);
      }
      catch (Throwable t)
      {
        LogMgr.logWarning("OracleRowDataReader.initialize()", "Class oracle.sql.TIMESTAMPTZ not available!", t);
      }
    }
    initSessionTimezone(conn);
  }

  private void initSessionTimezone(WbConnection conn)
  {
    final CallerInfo ci = new CallerInfo(){};
    try
    {
      Connection sqlConn = conn.getSqlConnection();
      Method getSessionTZ = sqlConn.getClass().getMethod("getSessionTimeZone", (Class[])null);
      getSessionTZ.setAccessible(true);
      String tzName = (String)getSessionTZ.invoke(sqlConn);
      TimeZone timeZone = TimeZone.getTimeZone(tzName);
      sessionTimezone = Calendar.getInstance(timeZone);
      LogMgr.logDebug(ci, "Using session time zone: " + timeZone.getID());
    }
    catch (Throwable th)
    {
      LogMgr.logWarning(ci, "Could not initialize session time zone", th);
    }

    if (sessionTimezone == null)
    {
      sessionTimezone = Calendar.getInstance();
      LogMgr.logWarning(ci, "Could not obtain session time zone from the driver. Using system time zone: " + sessionTimezone.getTimeZone().getID());
    }
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

  @Override
  protected Object readTimestampTZValue(ResultHolder rs, int column)
    throws SQLException
  {
    return readTimestampValue(rs, column);
  }

  @Override
  protected Object readTimestampValue(ResultHolder rs, int column)
    throws SQLException
  {
    Object value = rs.getObject(column);

    if (value == null) return null;
    if (rs.wasNull()) return null;

    if (value instanceof java.sql.Timestamp)
    {
      return value;
    }

    Object result = null;

    String clsName = value.getClass().getName();
    if ("oracle.sql.TIMESTAMPTZ".equals(clsName))
    {
      if (offsetDateTimeValueTZ != null)
      {
        result = convertUsingOffsetDateTime(value);
      }
      else if (stringValueTZ != null)
      {
        result = convertTZFromString(value);
      }
    }
    else if ("oracle.sql.TIMESTAMPLTZ".equals(clsName))
    {
      result = convertTIMESTAMPLTZ(value);
    }
    else if ("oracle.sql.TIMESTAMP".equals(clsName) && timestampValue != null)
    {
      result = convertTIMESTAMP(value);
    }

    if (result != null) return result;

    // fallback
    return rs.getTimestamp(column);
  }

  private ZoneId getTimeZone(Object tz)
  {
    try
    {
      TimeZone zone = (TimeZone)getTimeZoneTZ.invoke(tz, (Object []) null);
      return zone.toZoneId();
    }
    catch (Throwable ex)
    {
      LogMgr.logDebug(new CallerInfo(){}, "Could not retrieve time zone", ex);
    }
    return null;
  }

  private Object convertUsingOffsetDateTime(Object tz)
  {
    try
    {
      OffsetDateTime odt = (OffsetDateTime)offsetDateTimeValueTZ.invoke(tz, sqlConnection);
      ZoneId zone = getTimeZone(tz);
      if (zone != null)
      {
        return odt.atZoneSameInstant(zone);
      }
      return odt.toZonedDateTime();
    }
    catch (Throwable ex)
    {
      LogMgr.logDebug(new CallerInfo(){}, "Could not convert timestamp", ex);
    }
    return tz;
  }

  private Object convertTIMESTAMPLTZ(Object tz)
  {
    try
    {
      if (localDateTimeValueLTZ != null)
      {
        return localDateTimeValueLTZ.invoke(tz, sqlConnection);
      }

      if (timestampValueLTZ != null)
      {
        Timestamp ts =  (Timestamp)timestampValueLTZ.invoke(tz, sqlConnection, sessionTimezone);
        return ts;
      }
    }
    catch (Throwable ex)
    {
      LogMgr.logDebug(new CallerInfo(){}, "Could not convert TIMESTAMPLTZ", ex);
    }
    return tz;
  }

  private Object convertTIMESTAMP(Object tz)
  {
    try
    {
      return timestampValue.invoke(tz);
    }
    catch (Throwable ex)
    {
      LogMgr.logDebug(new CallerInfo(){}, "Could not convert convertTIMESTAMP", ex);
    }
    return tz;
  }

  private Object convertTZFromString(Object tz)
  {
    String tsValue = null;
    try
    {
      tsValue = (String)stringValueTZ.invoke(tz, sqlConnection);
      return ZonedDateTime.parse(tsValue, tsParser);
    }
    catch (Throwable ex)
    {
      // if something went wrong, disable parsing of the String value
      stringValueTZ = null;
      LogMgr.logDebug(new CallerInfo(){}, "Could not parse timestamp string: " + tsValue, ex);
    }
    return null;
  }
}
