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
package workbench.db.ibm;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnDefinitionEnhancer;
import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2iColumnEnhancer
  implements ColumnDefinitionEnhancer
{

  @Override
  public void updateColumnDefinition(TableDefinition table, WbConnection conn)
  {
    boolean showComments = conn.getDbSettings().getBoolProperty("remarks.columns.use_columntext", false);
    boolean showCCSID = conn.getDbSettings().getBoolProperty("remarks.columns.show_ccsid", true);

    if (showCCSID || showComments)
    {
      updateColumns(table, conn, showComments, showCCSID);
    }
  }

  public void updateColumns(TableDefinition table, WbConnection conn, boolean showComments, boolean showCCSID)
  {
    PreparedStatement stmt = null;
    ResultSet rs = null;

    String tablename = conn.getMetadata().removeQuotes(table.getTable().getTableName());
    String schema = conn.getMetadata().removeQuotes(table.getTable().getSchema());

    String sql =
      "select column_name, \n" +
      "       column_text, \n" +
      "       ccsid \n" +
      "from qsys2" + conn.getMetadata().getCatalogSeparator() + "syscolumns \n" +
      "where table_schema = ? \n" +
      "  and table_name  = ?";

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logInfo("Db2iColumnEnhancer.updateComputedColumns()", "Query to retrieve column information:\n" + SqlUtil.replaceParameters(sql, schema, tablename));
    }

    try
    {
      stmt = conn.getSqlConnection().prepareStatement(sql);
      stmt.setString(1, schema);
      stmt.setString(2, tablename);
      rs = stmt.executeQuery();
      while (rs.next())
      {
        String colname = rs.getString(1);
        String comment = rs.getString(2);
        int charset = rs.getInt(3);
        if (rs.wasNull())
        {
          charset = -1;
        }
        ColumnIdentifier col = ColumnIdentifier.findColumnInList(table.getColumns(), colname);
        if (col != null)
        {
          if (showComments && StringUtil.isNonEmpty(comment))
          {
            col.setComment(comment);
          }
          if (showCCSID && charset > 0)
          {
            col.setCollationExpression("CCSID " + charset);
          }
        }
      }
    }
    catch (Exception e)
    {
      LogMgr.logError("Db2iColumnEnhancer.updateComputedColumns()", "Error retrieving column comments using:\n" + SqlUtil.replaceParameters(sql, schema, tablename), e);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
  }

}
