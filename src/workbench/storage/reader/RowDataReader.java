/*
 * RowDataReader.java
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
package workbench.storage.reader;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Struct;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.BlobAccessType;
import workbench.db.ClobAccessType;
import workbench.db.DbMetadata;
import workbench.db.DbSettings;
import workbench.db.WbConnection;
import workbench.db.mssql.SqlServerDataConverter;
import workbench.db.oracle.OracleDataConverter;
import workbench.db.postgres.PostgresDataConverter;

import workbench.storage.ArrayConverter;
import workbench.storage.DataConverter;
import workbench.storage.RefCursorConsumer;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.StructConverter;

import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to read the data from a ResultSet.
 *
 * Different data types are handled correctly and different strategies for "extended" types (BLOB, CLOB XML, ...) can
 * be chosen.
 * <br/>
 * Errors during the retrieval of one row are re-thrown to be shown in the frontend. This behaviour can
 * be disabled using the config property <tt>workbench.db.ignore.readerror</tt>.
 *
 * @author Thomas Kellerer
 *
 * @see ResultInfo#treatLongVarcharAsClob()
 * @see ResultInfo#useGetBytesForBlobs()
 * @see ResultInfo#useGetStringForClobs()
 * @see ResultInfo#useGetStringForBit()
 * @see ResultInfo#useGetXML()
 * @see ResultInfo#getConvertArrays()
 */
public class RowDataReader
{
  private final List<Closeable> streams;
  protected DataConverter converter;
  protected boolean ignoreReadErrors;
  protected boolean useStreamsForBlobs;
  protected	boolean useStreamsForClobs;
  protected boolean longVarcharAsClob;
  protected BlobAccessType blobMethod = BlobAccessType.binaryStream;
  protected ClobAccessType clobMethod = ClobAccessType.string;
  protected boolean useGetStringForBit;
  protected boolean useGetObjectForDates;
  protected boolean useGetObjectForTimestamps;
  protected boolean useGetObjectForTimestampTZ;
  protected boolean useGetObjectForTime;
  protected boolean useGetXML;
  protected boolean adjustArrayDisplay;
  protected boolean showArrayType;
  protected boolean fixStupidMySQLZeroDate;
  protected boolean isOracle;
  protected ResultInfo resultInfo;
  protected RefCursorConsumer refCursorConsumer;

  public RowDataReader(ResultInfo info, WbConnection conn)
  {
    ignoreReadErrors = Settings.getInstance().getBoolProperty("workbench.db.ignore.readerror", false);
    converter = getConverterInstance(conn);
    isOracle = conn == null ? false : conn.getMetadata().isOracle();
    resultInfo = info;
    DbSettings dbs = conn == null ? null : conn.getDbSettings();
    if (dbs != null)
    {
      longVarcharAsClob = dbs.longVarcharIsClob();
      blobMethod = dbs.getBlobReadMethod();
      clobMethod = dbs.getClobReadMethod();
      useGetStringForBit = dbs.useGetStringForBit();
      useGetObjectForDates = dbs.useGetObjectForDates();
      useGetObjectForTimestamps = dbs.useGetObjectForTimestamps();
      useGetObjectForTimestampTZ = dbs.useGetObjectForTimestampTZ();
      useGetObjectForTime = dbs.useLocalTimeForTime();
      showArrayType = dbs.showArrayType();
      adjustArrayDisplay = dbs.handleArrayDisplay();
      useGetXML = dbs.useGetXML();
      fixStupidMySQLZeroDate = dbs.fixStupidMySQLZeroDate();
    }
    streams = new ArrayList<>(countLobColumns());
  }

  public void setRefCursorConsumer(RefCursorConsumer consumer)
  {
    this.refCursorConsumer = consumer;
  }

  private int countLobColumns()
  {
    if (resultInfo == null) return 0;
    int lobCount = 0;
    for (int i=0; i < resultInfo.getColumnCount(); i++)
    {
      int type = resultInfo.getColumnType(i);
      if (SqlUtil.isBlobType(type) || SqlUtil.isClobType(type))
      {
        lobCount ++;
      }
    }
    return lobCount;
  }

  /**
   * Register a DataConverter with this reader.
   *
   * @param conv  the convert to use
   * @see #read(java.sql.ResultSet, boolean)
   */
  public void setConverter(DataConverter conv)
  {
    this.converter = conv;
  }

  /**
   * Controls how BLOB columns are returned.
   * <br/>
   * By default they are converted to a byte[] array. If setUseStreamsForBlobs() is enabled.
   * then they will be returned as InputStreams (as returned by ResultSet.getBinaryStream()).
   * <br/>
   *
   * <b>If this is set to true, the consumer of the RowData instance is responsible for closing
   * all InputStreams returned by this class.</b>
   *
   * @param flag if true, return InputStreams instead of byte[]
   *
   * @see #read(java.sql.ResultSet, boolean)
   */
  public void setUseStreamsForBlobs(boolean flag)
  {
    useStreamsForBlobs = flag;
  }

  /**
   * Controls how CLOB columns are returned.
   * <br/>
   *
   * By default CLOB data is converted to a String.<br/>
   *
   * Setting useStreamsForClobs to true will return a <tt>Reader</tt> instance for the CLOB columns
   * (as returned by ResultSet.getCharacterStream()).
   *
   * <b>If this is set to true, the consumer of the RowData instance is responsible for closing
   * all Readers returned by this class.</b>
   *
   * Setting this to true will be ignored if <tt>getString()</tt> is used for the CLOB columns
   *
   * @param flag if true, return <tt>Reader</tt>s instead of <tt>String</tt>s
   *
   * @see #read(java.sql.ResultSet, boolean)
   * @see ResultInfo#useGetStringForClobs()
   */
  public void setUseStreamsForClobs(boolean flag)
  {
    useStreamsForClobs = flag;
  }

  public RowData read(ResultSet rs)
    throws SQLException
  {
    return read(rs, true);
  }

  /**
   * Read the current row from the ResultSet into a RowData instance
   * <br/>
   * It is assumed that ResultSet.next() has already been called on the ResultSet.
   * <br/>
   * BLOBs (and similar datatypes) will be read into a byte array unless setUseStreamsForBlobs(true) is called.
   * CLOBs (and similar datatypes) will be converted into a String object.
   * <br/>
   * For all "known" types, the corresponding getXXX() method is called on the ResultSet
   * <br/>
   * If the driver returns a java.sql.Struct, this will be converted into a String
   * using {@linkplain StructConverter#getStructDisplay(java.sql.Struct)}
   * <br/>
   * After retrieving the value from the ResultSet it is passed to a registered DataConverter.
   * If a converter is registered, no further processing will be done with the column's value
   * <br/>
   * The status of the returned RowData will be NOT_MODIFIED.
   *
   * @param rs              the ResultSet that is positioned to the correct row
   * @param trimCharData    if true, values for Types.CHAR columns will be trimmed.
   *
   * @see #setConverter(workbench.storage.DataConverter)
   * @see #setUseStreamsForBlobs(boolean)
   * @see #setUseStreamsForClobs(boolean)
   * @see #read(ResultHolder, boolean)
   */
  public RowData read(ResultSet rs, boolean trimCharData)
    throws SQLException
  {
    return read(new ResultSetHolder(rs), trimCharData);
  }

  public RowData read(ResultHolder rs, boolean trimCharData)
    throws SQLException
  {
    int colCount = resultInfo.getColumnCount();
    Object[] colData = new Object[colCount];

    Object value;
    for (int i=0; i < colCount; i++)
    {
      int type = resultInfo.getColumnType(i);

      if (converter != null)
      {
        String dbms = resultInfo.getDbmsTypeName(i);
        if (converter.convertsType(type, dbms))
        {
          value = rs.getObject(i + 1);
          colData[i] = converter.convertValue(type, dbms, value);
          continue;
        }
      }
      colData[i] = readColumnData(rs, type, i+1, trimCharData);
    }
    return new RowData(colData);
  }

  public Object readColumnData(ResultHolder rs, int type, int column, boolean trimCharData)
    throws SQLException
  {
    Object value = null;

    try
    {
      if (type == Types.VARCHAR || type == Types.NVARCHAR)
      {
        value = rs.getString(column);
      }
      else if (type == Types.TIMESTAMP)
      {
        value = readTimestampValue(rs, column);
      }
      else if (type == Types.TIMESTAMP_WITH_TIMEZONE)
      {
        value = readTimestampTZValue(rs, column);
      }
      else if (type == Types.DATE)
      {
        value = readDateValue(rs, column);
      }
      else if (type == Types.TIME)
      {
        value = readTimeValue(rs, column);
      }
      else if (type == Types.TIME_WITH_TIMEZONE)
      {
        value = readTimeTZValue(rs, column);
      }
      else if (useGetStringForBit && type == Types.BIT)
      {
        value = rs.getString(column);
      }
      else if (adjustArrayDisplay && type == java.sql.Types.ARRAY)
      {
        // this is mainly here for Oracle nested tables and VARRAYS, but should basically work
        // for other arrays as well.
        Object o = rs.getObject(column);
        value = ArrayConverter.getArrayDisplay(o, resultInfo.getDbmsTypeName(column-1), showArrayType, isOracle);
      }
      else if (type == java.sql.Types.STRUCT)
      {
        Object o = rs.getObject(column);
        if (o instanceof Struct)
        {
          value = StructConverter.getInstance().getStructDisplay((Struct)o, isOracle);
        }
        else
        {
          value = o;
        }
      }
      else if (SqlUtil.isBlobType(type))
      {
        if (useStreamsForBlobs)
        {
          // this is used by the RowDataConverter in order to avoid
          // reading large blobs into memory
          InputStream in = rs.getBinaryStream(column);
          if (in == null || rs.wasNull())
          {
            value = null;
          }
          else
          {
            addStream(in);
            value = in;
          }
        }
        else
        {
          value = readBlob(rs, column);
        }
      }
      else if (type == Types.SQLXML)
      {
        value = readXML(rs, column, useGetXML);
      }
      else if (SqlUtil.isClobType(type, longVarcharAsClob))
      {
        if (useStreamsForClobs)
        {
          Reader in = rs.getCharacterStream(column);
          if (in == null || rs.wasNull())
          {
            value = null;
          }
          else
          {
            value = in;
            addStream(in);
          }
        }
        else
        {
          value = readClob(rs, column);
        }
      }
      else if (type == Types.CHAR || type == Types.NCHAR)
      {
        value = rs.getString(column);
        if (trimCharData)
        {
          value = StringUtil.rtrim((String)value);
        }
      }
      else if (refCursorConsumer != null && refCursorConsumer.isRefCursor(type, resultInfo.getDbmsTypeName(column - 1)))
      {
        value = refCursorConsumer.readRefCursor(rs, column);
      }
      else
      {
        value = rs.getObject(column);
      }
    }
    catch (SQLException e)
    {
      if (ignoreReadErrors)
      {
        value = null;
        LogMgr.logError(new CallerInfo(){}, "Error retrieving data for column '" + resultInfo.getColumnName(column-1) + "'. Using NULL!", e);
      }
      else
      {
        throw e;
      }
    }
    catch (Throwable e)
    {
      // I'm catching Throwable here just in case.
      // e.g. if Oracle's XML type library is missing, a ClassNotFoundException is thrown
      // that would otherwise not be visible)
      String error = e.getClass().getName();
      if (e.getMessage() != null)
      {
        error += ": " + e.getMessage();
      }
      throw new SQLException(error, e);
    }
    return value;
  }

  protected Object readTimeValue(ResultHolder rs, int column)
    throws SQLException
  {
    if (useGetObjectForTime)
    {
      try
      {
        return rs.getObject(column, LocalTime.class);
      }
      catch (UnsupportedOperationException | NoSuchMethodError | AbstractMethodError th)
      {
        useGetObjectForTime = false;
      }
    }
    return rs.getTime(column);
  }

  protected Object readTimeTZValue(ResultHolder rs, int column)
    throws SQLException
  {
    return readTimeValue(rs, column);
  }

  /**
   * Read a timestamp value from the current row and given column.
   * <br/>
   * <br/>
   * This is made a separate method to allow DBMS specifc implementations of the RowDataReader to  read timestamps differently.<br/>
   * <br/>
   * Currently this is used to adjust the timezone information returned by the Oracle driver
   * (see {@linkplain OracleRowDataReader#readTimestampValue(java.sql.ResultSet, int)})
   *
   * @param rs      the ResultSet to read from
   * @param column  the column index (1-based) to read
   * @return the value retrieved.
   *
   * @throws SQLException
   * @see OracleRowDataReader#readTimestampValue(java.sql.ResultSet, int)
   */
  protected Object readTimestampValue(ResultHolder rs, int column)
    throws SQLException
  {
    try
    {
      if (useGetObjectForTimestamps)
      {
        try
        {
          return rs.getObject(column, LocalDateTime.class);
        }
        catch (UnsupportedOperationException | NoSuchMethodError | AbstractMethodError th)
        {
          useGetObjectForTimestamps = false;
        }
      }
      return rs.getTimestamp(column);
    }
    catch (SQLException ex)
    {
      if (fixStupidMySQLZeroDate && "S1009".equals(ex.getSQLState()))
      {
        return rs.getString(column);
      }
      throw ex;
    }
  }

  protected Object readTimestampTZValue(ResultHolder rs, int column)
    throws SQLException
  {
    if (useGetObjectForTimestamps)
    {
      try
      {
        return rs.getObject(column, OffsetDateTime.class);
      }
      catch (UnsupportedOperationException | NoSuchMethodError | AbstractMethodError th)
      {
        useGetObjectForTimestamps = false;
      }
    }
    return readTimestampValue(rs, column);
  }

  protected Object readDateValue(ResultHolder rs, int column)
    throws SQLException
  {
    if (useGetObjectForDates)
    {
      try
      {
        return rs.getObject(column, LocalDate.class);
      }
      catch (UnsupportedOperationException | NoSuchMethodError | AbstractMethodError th)
      {
        useGetObjectForDates = false;
      }
    }
    return rs.getDate(column);
  }

  private void addStream(Closeable in)
  {
    if (in == null) return;
    synchronized (streams)
    {
      streams.add(in);
    }
  }

  public void closeStreams()
  {
    synchronized (streams)
    {
      if (streams.size() > 0)
      {
        FileUtil.closeStreams(streams);
        streams.clear();
      }
    }
  }

  private Object readXML(ResultHolder rs, int column, boolean useGetXML)
    throws SQLException
  {
    Object value = null;
    if (useGetXML)
    {
      SQLXML xml = null;
      try
      {
        xml = rs.getSQLXML(column);
        if (xml != null && !rs.wasNull())
        {
          value = xml.getString();
        }
      }
      finally
      {
        if (xml != null) xml.free();
      }
    }
    else
    {
      value = readCharacterStream(rs, column);
    }
    return value;
  }

  private Object readClob(ResultHolder rs, int column)
    throws SQLException
  {
    Object value = null;
    switch (clobMethod)
    {
      case characterStream:
        value = readCharacterStream(rs, column);
        break;
      case string:
        value = rs.getString(column);
        break;
      case jdbcClob:
        Clob data = rs.getClob(column);
        int len = (int)data.length();
        if (len >= 0)
        {
          value = data.getSubString(1, len);
        }
    }
    return value;
  }

  private Object readBlob(ResultHolder rs, int column)
    throws SQLException
  {
    Object value = null;
    switch (blobMethod)
    {
      case binaryStream:
        value = readBinaryStream(rs, column);
        break;
      case byteArray:
        value = rs.getBytes(column);
        if (rs.wasNull()) value = null;
        break;
      case jdbcBlob:
        Blob data = rs.getBlob(column);
        int len = (int)data.length();
        if (len >= 0)
        {
          value = data.getBytes(1, len);
        }
    }
    return value;
  }

  private Object readBinaryStream(ResultHolder rs, int column)
    throws SQLException
  {
    Object value;
    InputStream in;
    try
    {
      in = rs.getBinaryStream(column);
      if (in == null || rs.wasNull())
      {
        value = null;
      }
      else
      {
        // readBytes will close the InputStream
        value = FileUtil.readBytes(in);
      }
    }
    catch (IOException e)
    {
      LogMgr.logError(new CallerInfo(){}, "Error retrieving binary data for column " + resultInfo.getColumnName(column), e);
      value = rs.getObject(column);
    }
    return value;
  }

  private Object readCharacterStream(ResultHolder rs, int column)
    throws SQLException
  {
    Object value;
    Reader in;
    try
    {
      in = rs.getCharacterStream(column);
      if (in != null && !rs.wasNull())
      {
        // readCharacters will close the Reader
        value = FileUtil.readCharacters(in);
      }
      else
      {
        value = null;
      }
    }
    catch (IOException e)
    {
      LogMgr.logWarning(new CallerInfo(){}, "Error retrieving clob data for column " + resultInfo.getColumnName(column), e);
      value = rs.getObject(column);
    }
    return value;
  }

  /**
   * Creates instances of necessary DataConverters.
   * <br/>
   * The following datatypes are currently supported:
   * <ul>
   * <li>For Postgres: hstore</li>
   * <li>For SQL Server's timestamp type</li>
   * <li>For Oracle: RAW and ROWID types</li>
   * </ul>
   *
   * @param conn the connection for which to create the DataConverter
   * @return a suitable converter or null if nothing should be converted
   *
   * @see workbench.resource.Settings#getFixSqlServerTimestampDisplay()
   * @see workbench.resource.Settings#getConvertOracleTypes()
   * @see workbench.db.oracle.OracleDataConverter
   * @see workbench.db.mssql.SqlServerDataConverter
   */
  public static DataConverter getConverterInstance(WbConnection conn)
  {
    if (conn == null) return null;

    DbMetadata meta = conn.getMetadata();
    if (meta == null) return null;

    if (meta.isOracle() && Settings.getInstance().getConvertOracleTypes())
    {
      return OracleDataConverter.getInstance();
    }
    if (meta.isSqlServer() && Settings.getInstance().getFixSqlServerTimestampDisplay())
    {
      return SqlServerDataConverter.getInstance();
    }
    if (meta.isPostgres())
    {
      return PostgresDataConverter.getInstance();
    }
    return null;
  }


}
