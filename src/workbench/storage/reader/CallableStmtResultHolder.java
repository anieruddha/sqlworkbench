/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2018 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;

/**
 *
 * @author Thomas Kellerer
 */
public class CallableStmtResultHolder
  implements ResultHolder
{
  private final CallableStatement stmt;

  public CallableStmtResultHolder(CallableStatement cstmt)
  {
    this.stmt = cstmt;
  }

  @Override
  public Array getArray(int columnIndex)
    throws SQLException
  {
    return stmt.getArray(columnIndex);
  }

  @Override
  public BigDecimal getBigDecimal(int columnIndex)
    throws SQLException
  {
    return stmt.getBigDecimal(columnIndex);
  }

  @Override
  public InputStream getBinaryStream(int columnIndex)
    throws SQLException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Blob getBlob(int columnIndex)
    throws SQLException
  {
    return stmt.getBlob(columnIndex);
  }

  @Override
  public boolean getBoolean(int columnIndex)
    throws SQLException
  {
    return stmt.getBoolean(columnIndex);
  }

  @Override
  public byte[] getBytes(int columnIndex)
    throws SQLException
  {
    return stmt.getBytes(columnIndex);
  }

  @Override
  public Reader getCharacterStream(int columnIndex)
    throws SQLException
  {
    return stmt.getCharacterStream(columnIndex);
  }

  @Override
  public Clob getClob(int columnIndex)
    throws SQLException
  {
    return stmt.getClob(columnIndex);
  }

  @Override
  public Date getDate(int columnIndex)
    throws SQLException
  {
    return stmt.getDate(columnIndex);
  }

  @Override
  public Object getObject(int columnIndex)
    throws SQLException
  {
    return stmt.getObject(columnIndex);
  }

  @Override
  public <T> T getObject(int columnIndex, Class<T> type)
    throws SQLException
  {
    return stmt.getObject(columnIndex, type);
  }

  @Override
  public String getString(int columnIndex)
    throws SQLException
  {
    return stmt.getString(columnIndex);
  }

  @Override
  public Time getTime(int columnIndex)
    throws SQLException
  {
    return stmt.getTime(columnIndex);
  }

  @Override
  public Timestamp getTimestamp(int columnIndex)
    throws SQLException
  {
    return stmt.getTimestamp(columnIndex);
  }

  @Override
  public boolean wasNull()
    throws SQLException
  {
    return stmt.wasNull();
  }

  @Override
  public SQLXML getSQLXML(int columnIndex)
    throws SQLException
  {
    return stmt.getSQLXML(columnIndex);
  }

}
