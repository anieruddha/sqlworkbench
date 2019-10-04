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
package workbench.db.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DefaultViewReader;
import workbench.db.DropType;
import workbench.db.JdbcUtils;
import workbench.db.QuoteHandler;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresViewReader
  extends DefaultViewReader
{

  public PostgresViewReader(WbConnection con)
  {
    super(con);
  }

  @Override
  public CharSequence getExtendedViewSource(TableDefinition view, DropType dropType, boolean includeCommit)
    throws SQLException
  {
    CharSequence source = super.getExtendedViewSource(view, dropType, false);

    PostgresRuleReader ruleReader = new PostgresRuleReader();
    CharSequence rules = ruleReader.getTableRuleSource(this.connection, view.getTable());
    String defaults = getColumnDefaults(view);

    StringBuilder result = new StringBuilder(source.length() + 50);
    result.append(source);

    if (StringUtil.isNonBlank(defaults))
    {
      result.append(defaults);
    }

    if (rules != null)
    {
      result.append("\n");
      result.append(rules);
    }

    if (includeCommit)
    {
      result.append("COMMIT;");
      result.append(Settings.getInstance().getInternalEditorLineEnding());
    }
    return result;
  }

  private String getColumnDefaults(TableDefinition view)
  {
    boolean isPg12 = JdbcUtils.hasMinimumServerVersion(connection, "12");
    String src;
    if (isPg12)
    {
      src = "pg_get_expr(d.adbin, d.adrelid)";
    }
    else
    {
      src = "d.adsrc";
    }
    
    String sql =
      "select c.attname as column_name, \n" +
      "       " + src + " as expression\n" +
      "from pg_attrdef d\n" +
      "  join pg_attribute c on c.attrelid = d.adrelid and c.attnum = d.adnum\n" +
      "  join pg_class v on v.oid = d.adrelid\n" +
      "  join pg_namespace n on n.oid = v.relnamespace\n" +
      "where v.relname = ? \n" +
      "  and n.nspname = ? ";

    PreparedStatement pstmt = null;
    ResultSet rs = null;

    TableIdentifier t = view.getTable();
    StringBuilder result = new StringBuilder();

    QuoteHandler quote = connection.getMetadata();

    LogMgr.logMetadataSql(new CallerInfo(){}, "view column defaults", sql, t.getRawTableName(), t.getRawSchema());
    try
    {
      pstmt = this.connection.getSqlConnection().prepareStatement(sql);
      pstmt.setString(1, t.getRawTableName());
      pstmt.setString(2, t.getRawSchema());
      rs = pstmt.executeQuery();
      while (rs.next())
      {
        String column = rs.getString(1);
        String expression = rs.getString(2);
        result.append("ALTER TABLE " + t.getTableExpression(connection) + " ALTER " + quote.quoteObjectname(column) + " SET DEFAULT " + expression + ";\n");
      }
    }
    catch (Exception ex)
    {
      LogMgr.logMetadataError(new CallerInfo(){}, ex, "view column defaults", sql, t.getRawTableName(), t.getRawSchema());
    }
    return result.toString();
  }
}
