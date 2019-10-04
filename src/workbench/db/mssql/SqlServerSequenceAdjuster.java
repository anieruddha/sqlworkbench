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
package workbench.db.mssql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;

import workbench.db.SequenceAdjuster;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerSequenceAdjuster
  implements SequenceAdjuster
{
  @Override
  public int adjustTableSequences(WbConnection connection, TableIdentifier table, boolean includeCommit)
    throws SQLException
  {

    String idColumn = getIdentityColumn(table, connection);
    if (idColumn == null)
    {
      return 0;
    }
    long maxValue = getMaxValue(table, idColumn, connection);
    if (maxValue < 0) return 0;

    Statement stmt = null;
    try
    {
      stmt = connection.getSqlConnection().createStatement();
      String tname = table.getTableExpression(connection);
      stmt.execute("dbcc checkident ('" + tname + "', RESEED, " + maxValue + ")");
    }
    catch (Exception ex)
    {
      LogMgr.logError(new CallerInfo(){}, "Could not reseed identity column", ex);
      return 0;
    }
    finally
    {
      SqlUtil.close(stmt);
    }
    return 1;
  }

  public long getMaxValue(TableIdentifier tbl, String column, WbConnection conn)
  {
    if (column == null) return -1;

    String sql =
      "select max(" + conn.getMetadata().quoteObjectname(column) + ") from " + tbl.getTableExpression(conn);

    LogMgr.logMetadataSql(new CallerInfo(){}, "max identity value", sql);

    PreparedStatement pstmt = null;
    ResultSet rs = null;
    long result = -1;

    try
    {
      pstmt = conn.getSqlConnection().prepareStatement(sql);
      pstmt.setString(1, tbl.getRawSchema());
      pstmt.setString(2, tbl.getRawTableName());
      rs = pstmt.executeQuery();
      if (rs.next())
      {
        result = rs.getLong(1);
        if (rs.wasNull())
        {
          result = 0;
        }
      }
    }
    catch (Exception ex)
    {
      LogMgr.logMetadataError(new CallerInfo(){}, ex, "max identity value", sql);
    }
    finally
    {
      SqlUtil.close(rs, pstmt);
    }
    return result;
  }

  private String getIdentityColumn(TableIdentifier tbl, WbConnection conn)
  {
    String sql =
      "select COLUMN_NAME \n" +
      "from INFORMATION_SCHEMA.COLUMNS \n" +
      "where COLUMNPROPERTY(object_id(TABLE_SCHEMA+'.'+TABLE_NAME), COLUMN_NAME, 'IsIdentity') = 1 \n" +
      "  and TABLE_SCHEMA = ? \n" +
      "  and TABLE_NAME = ? \n";

    LogMgr.logMetadataSql(new CallerInfo(){}, "identity column", sql, tbl.getRawSchema(), tbl.getRawTableName());
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    String column = null;
    try
    {
      pstmt = conn.getSqlConnection().prepareStatement(sql);
      pstmt.setString(1, tbl.getRawSchema());
      pstmt.setString(2, tbl.getRawTableName());
      rs = pstmt.executeQuery();
      if (rs.next())
      {
        column = rs.getString(1);
      }
    }
    catch (Exception ex)
    {
      LogMgr.logMetadataError(new CallerInfo(){}, ex, "identity column", sql, tbl.getRawSchema(), tbl.getRawTableName());
    }
    finally
    {
      SqlUtil.close(rs, pstmt);
    }
    return column;
  }
}
