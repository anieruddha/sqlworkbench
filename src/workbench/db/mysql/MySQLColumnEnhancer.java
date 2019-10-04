/*
 * MySQLColumnEnhancer.java
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
package workbench.db.mysql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;

import workbench.db.ColumnDefinitionEnhancer;
import workbench.db.ColumnIdentifier;
import workbench.db.JdbcUtils;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to retrieve enum and collation definitions for the columns of a MySQL table.
 *
 * @author  Thomas Kellerer
 * @see workbench.db.DbMetadata#getTableDefinition(workbench.db.TableIdentifier)
 * @see MySQLEnumReader
 * @see MySQLColumnCollationReader
 */
public class MySQLColumnEnhancer
  implements ColumnDefinitionEnhancer
{

  @Override
  public void updateColumnDefinition(TableDefinition tbl, WbConnection connection)
  {
    MySQLColumnCollationReader collationReader = new MySQLColumnCollationReader();
    collationReader.readCollations(tbl, connection);

    MySQLEnumReader enumReader = new MySQLEnumReader();
    enumReader.readEnums(tbl, connection);

    updateComputedColumns(tbl, connection);
  }

  private void updateComputedColumns(TableDefinition tbl, WbConnection connection)
  {
    PreparedStatement stmt = null;
    ResultSet rs = null;

    boolean supportGeneratedColumns = JdbcUtils.hasMinimumServerVersion(connection, "5.7") && !connection.getMetadata().isMariaDB();

    String sql =
      "select column_name, " +
      "       extra, \n" +
      "       " + (supportGeneratedColumns ? "generation_expression " : "null as generation_expression ") + " \n" +
      "from information_schema.columns \n" +
      "where table_schema = ?\n" +
      "  and table_name = ? \n ";

    LogMgr.logMetadataSql(new CallerInfo(){}, "column information", sql, tbl.getTable().getRawCatalog(), tbl.getTable().getRawTableName());

    try
    {
      stmt = connection.getSqlConnection().prepareStatement(sql);
      stmt.setString(1, tbl.getTable().getRawCatalog());
      stmt.setString(2, tbl.getTable().getRawTableName());
      rs = stmt.executeQuery();
      List<ColumnIdentifier> columns = tbl.getColumns();
      while (rs.next())
      {
        String colname = rs.getString(1);
        String extra = rs.getString(2);
        String expression = rs.getString(3);
        ColumnIdentifier col = ColumnIdentifier.findColumnInList(columns, colname);
        if (col != null)
        {
          // MySQL < 8.0 returns the "extra" as e.g. "GENERATED on UPDATE..."
          // For MySQL 8.0, Oracle chose to change that, and now prefixes this with DEFAUL_GENERATED.
          extra = trimKeyWords(extra, "GENERATED", "DEFAULT_GENERATED");
          if (StringUtil.isNonBlank(expression))
          {
            String genSql = "GENERATED ALWAYS AS (" + expression + ") " + extra;
            col.setComputedColumnExpression(genSql);
          }
          else if (extra != null && extra.toLowerCase().startsWith("on update"))
          {
            String defaultValue = col.getDefaultValue();
            if (defaultValue == null)
            {
              col.setDefaultValue(extra);
            }
            else
            {
              defaultValue += " " + extra;
              col.setDefaultValue(defaultValue);
            }
          }
          else if (StringUtil.equalStringIgnoreCase(extra, "INVISIBLE"))
          {
            // MariaDB 10.3
            col.setSQLOption(extra);
          }
        }
      }
    }
    catch (Exception ex)
    {
      LogMgr.logMetadataError(new CallerInfo(){}, ex, "column information", sql);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
  }

  private String trimKeyWords(String input, String... keywords)
  {
    if (keywords == null || keywords.length == 0) return input;
    if (StringUtil.isBlank(input)) return input;
    input = input.trim();

    for (String kw : keywords)
    {
      if (input.toLowerCase().startsWith(kw.toLowerCase()))
      {
        return input.substring(kw.length() + 1);
      }
    }
    return input;
  }
}
