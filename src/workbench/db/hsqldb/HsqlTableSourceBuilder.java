/*
 * HsqlTableSourceBuilder.java
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
package workbench.db.hsqldb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.JdbcUtils;
import workbench.db.QuoteHandler;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class HsqlTableSourceBuilder
  extends TableSourceBuilder
{

  public HsqlTableSourceBuilder(WbConnection con)
  {
    super(con);
  }

  @Override
  public void readTableOptions(TableIdentifier tbl, List<ColumnIdentifier> columns)
  {
    if (tbl == null) return;
    if (tbl.getSourceOptions().isInitialized()) return;

    boolean alwaysShowType = Settings.getInstance().getBoolProperty("workbench.db.hsql_database_engine.table_type.show_always", false);

    PreparedStatement pstmt = null;
    ResultSet rs = null;

    final String sql =
      "select hsqldb_type, \n" +
      "       (select upper(property_value) from information_schema.system_properties where property_name = 'hsqldb.default_table_type') as default_type \n" +
      "from information_schema.system_tables \n" +
      "where table_name = ? \n" +
      "  and table_schem = ?";

    try
    {
      pstmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
      pstmt.setString(1, tbl.getTableName());
      pstmt.setString(2, tbl.getSchema());

      LogMgr.logMetadataSql(new CallerInfo(){}, "table options", sql, tbl.getRawTableName(), tbl.getRawSchema());

      rs = pstmt.executeQuery();
      if (rs.next())
      {
        String type = rs.getString(1);
        String defaultType = rs.getString(2);
        if (defaultType == null)
        {
          defaultType = "CACHED";
        }
        if (alwaysShowType || !defaultType.equals(type))
        {
          tbl.getSourceOptions().setTypeModifier(type);
        }
      }
    }
    catch (SQLException e)
    {
      LogMgr.logMetadataError(new CallerInfo(){}, e, "table options", sql, tbl.getRawTableName(), tbl.getRawSchema());
    }
    finally
    {
      SqlUtil.closeAll(rs, pstmt);
    }

    if (JdbcUtils.hasMinimumServerVersion(dbConnection, "2.5"))
    {
      readSystemVersioning(tbl);
    }

    tbl.getSourceOptions().setInitialized();
  }

  private void readSystemVersioning(TableIdentifier tbl)
  {
    final String sql =
      "SELECT period_name, start_column_name, end_column_name \n" +
      "FROM information_schema.periods \n" +
      "where table_name = ? \n" +
      "  and table_schema = ?";

    PreparedStatement pstmt = null;
    ResultSet rs = null;

    try
    {
      pstmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
      pstmt.setString(1, tbl.getTableName());
      pstmt.setString(2, tbl.getSchema());

      LogMgr.logMetadataSql(new CallerInfo(){}, "table period", sql, tbl.getRawTableName(), tbl.getRawSchema());

      QuoteHandler quoter = dbConnection.getMetadata();
      
      rs = pstmt.executeQuery();
      if (rs.next())
      {
        String name = rs.getString(1);
        String startCol = rs.getString(2);
        String endCol = rs.getString(3);
        String period = "PERIOD FOR " + quoter.quoteObjectname(name) +
                                  " (" + quoter.quoteObjectname(startCol) + ", " + quoter.quoteObjectname(endCol) + ")";
        tbl.getSourceOptions().setInlineOption(period);
      }
    }
    catch (SQLException e)
    {
      LogMgr.logMetadataError(new CallerInfo(){}, e, "table period", sql, tbl.getRawTableName(), tbl.getRawSchema());
    }
    finally
    {
      SqlUtil.closeAll(rs, pstmt);
    }
  }
}
