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
public interface ResultHolder
{
  Array getArray(int columnIndex)
    throws SQLException;

  BigDecimal getBigDecimal(int columnIndex)
    throws SQLException;

  InputStream getBinaryStream(int columnIndex)
    throws SQLException;

  Blob getBlob(int columnIndex)
    throws SQLException;

  boolean getBoolean(int columnIndex)
    throws SQLException;

  byte[] getBytes(int columnIndex)
    throws SQLException;

  Reader getCharacterStream(int columnIndex)
    throws SQLException;

  Clob getClob(int columnIndex)
    throws SQLException;

  Date getDate(int columnIndex)
    throws SQLException;

  Object getObject(int columnIndex)
    throws SQLException;

  <T> T getObject(int columnIndex, Class<T> type)
    throws SQLException;

  String getString(int columnIndex)
    throws SQLException;

  Time getTime(int columnIndex)
    throws SQLException;

  Timestamp getTimestamp(int columnIndex)
    throws SQLException;

  boolean wasNull()
    throws SQLException;

  SQLXML getSQLXML(int columnIndex)
    throws SQLException;


}
