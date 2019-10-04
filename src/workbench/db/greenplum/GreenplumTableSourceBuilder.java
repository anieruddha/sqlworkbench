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
package workbench.db.greenplum;

import workbench.db.postgres.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.List;

import workbench.log.CallerInfo;

import workbench.db.ColumnIdentifier;
import workbench.db.IndexDefinition;
import workbench.db.ObjectSourceOptions;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DependencyNode;
import workbench.db.DropType;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class GreenplumTableSourceBuilder
  extends PostgresTableSourceBuilder
{

  public GreenplumTableSourceBuilder(WbConnection con)
  {
    super(con);
  }

  @Override
  public CharSequence getCreateTable(TableIdentifier table, List<ColumnIdentifier> columns, List<IndexDefinition> indexList, List<DependencyNode> fkDefinitions, DropType dropType, boolean includeFk, boolean includePK, boolean useFQN)
  {
    return super.baseCreateTable(table, columns, indexList, fkDefinitions, dropType, includeFk, includePK, useFQN);
  }

  @Override
  public void readTableOptions(TableIdentifier table, List<ColumnIdentifier> columns)
  {
    ObjectSourceOptions option = table.getSourceOptions();
    if (option.isInitialized()) return;

    PostgresRuleReader ruleReader = new PostgresRuleReader();
    CharSequence rule = ruleReader.getTableRuleSource(dbConnection, table);
    if (rule != null)
    {
      option.setAdditionalSql(rule.toString());
    }

    if (table.getType().equals(GreenplumExternalTableReader.EXT_TABLE_TYPE))
    {
      GreenplumExternalTableReader reader = new GreenplumExternalTableReader();
      reader.readTableOptions(dbConnection, table, columns);
    }
    else
    {
      retrieveTableOptions(table, columns);
    }
    option.setInitialized();
  }

  private void retrieveTableOptions(TableIdentifier tbl, List<ColumnIdentifier> columns)
  {
    ObjectSourceOptions option = tbl.getSourceOptions();

    StringBuilder tableSql = new StringBuilder();

    PreparedStatement pstmt = null;
    ResultSet rs = null;

    final CallerInfo ci = new CallerInfo(){};
    String sql =
      "select ct.relstorage, \n" +
      "       ct.relkind, \n" +
      "       array_to_string(ct.reloptions, ', ') as options, \n" +
      "       own.rolname as owner, \n" +
      "       p.attrnums, \n" +
      "       (exists (select * from pg_partition pt where pt.parrelid = ct.oid)) as is_partitioned \n" +
      "from pg_catalog.pg_class ct \n" +
      "  join pg_catalog.pg_namespace cns on ct.relnamespace = cns.oid \n " +
      "  join pg_catalog.pg_roles own on ct.relowner = own.oid \n " +
      "  left join pg_catalog.gp_distribution_policy p on p.localoid = ct.oid \n" +
      " where cns.nspname = ? \n" +
      "   and ct.relname = ?";

    boolean isPartitioned = false;

    Savepoint sp = null;
    try
    {
      sp = dbConnection.setSavepoint();
      pstmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
      pstmt.setString(1, tbl.getRawSchema());
      pstmt.setString(2, tbl.getRawTableName());

      if (Settings.getInstance().getDebugMetadataSql())
      {
        LogMgr.logDebug(ci, "Retrieving table options using:\n" + SqlUtil.replaceParameters(sql, tbl.getSchema(), tbl.getTableName()));
      }

      rs = pstmt.executeQuery();

      if (rs.next())
      {
        String storage = rs.getString("relstorage");
        String type = rs.getString("relkind");
        String settings = rs.getString("options");
        String owner = rs.getString("owner");
        isPartitioned = rs.getBoolean("is_partitioned");
        String attrNums = rs.getString("attrnums");
        int[] distrCols = GreenplumUtil.parseIntArray(attrNums);

        tbl.setOwner(owner);

        if ("u".equals(storage))
        {
          option.setTypeModifier("TEMPORARY");
        }
        if (StringUtil.isNonEmpty(settings))
        {
          setConfigSettings(settings, option);
          if (tableSql.length() > 0) tableSql.append('\n');
          tableSql.append("WITH (");
          tableSql.append(settings);
          tableSql.append(")");
        }
        // The "distributed by" needs to go after the WITH part
        if (!tbl.getType().startsWith("EXTERNAL"))
        {
          // The DISTRIBUTED BY for external tables needs to be handle there
          tableSql.append(getDistribution(distrCols, columns));
        }
      }
      dbConnection.releaseSavepoint(sp);
    }
    catch (SQLException e)
    {
      dbConnection.rollback(sp);
      LogMgr.logError(ci, "Error retrieving table options using:\n" + SqlUtil.replaceParameters(sql, tbl.getSchema(), tbl.getTableName()), e);
    }
    finally
    {
      SqlUtil.closeAll(rs, pstmt);
    }
    option.appendTableOptionSQL(tableSql.toString());

    if (isPartitioned)
    {
      tbl.setIsPartitioned(true);
      handlePartitions(tbl);
    }
  }

  public static String getDistribution(int[] distrCols, List<ColumnIdentifier> columns)
  {
    if (distrCols == null) return "";
    if (distrCols.length == 0)
    {
      return "DISTRIBUTED RANDOMLY";
    }
    String distr = "DISTRIBUTED BY (";
    for (int i=0; i < distrCols.length; i++)
    {
      int colIndex = distrCols[i];
      if (colIndex > 0)
      {
        String columnName = columns.get(colIndex - 1).getColumnName();
        if (i > 0)
        {
          distr += ", ";
        }
        distr += columnName;
      }
    }
    return distr + ")";
  }

  @Override
  protected void handlePartitions(TableIdentifier table)
  {
    String def = null;
    String query =
      "select pg_get_partition_def('" + table.getFullyQualifiedName(dbConnection) + "'::regclass, true, false)";
    Savepoint sp = null;
    ResultSet rs = null;
    Statement stmt = null;
    try
    {
      sp = dbConnection.setSavepoint();
      stmt = this.dbConnection.createStatementForQuery();
      rs = stmt.executeQuery(query);
      if (rs.next())
      {
        def = rs.getString(1);
      }
      dbConnection.releaseSavepoint(sp);
    }
    catch (SQLException e)
    {
      dbConnection.rollback(sp);
      LogMgr.logError(new CallerInfo(){}, "Error retrieving table options using: \n" + query, e);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    ObjectSourceOptions option = table.getSourceOptions();
    String sql = option.getTableOption();
    if (sql == null)
    {
      sql = def;
    }
    else
    {
      sql += "\n" + def;
    }
    option.setTableOption(sql);
  }

  @Override
  public String getAdditionalTableInfo(TableIdentifier table, List<ColumnIdentifier> columns, List<IndexDefinition> indexList)
  {
    String schema = table.getSchemaToUse(this.dbConnection);
    CharSequence enums = getEnumInformation(columns, schema);
    CharSequence domains = getDomainInformation(columns, schema);
    CharSequence sequences = getColumnSequenceInformation(table, columns);
    CharSequence children = null;
    if (!table.isPartitioned())
    {
      children = getChildTables(table);
    }
    String owner = getOwnerSql(table);

    if (StringUtil.allEmpty(enums, domains, sequences, children, owner)) return null;

    StringBuilder result = new StringBuilder(200);

    if (enums != null) result.append(enums);
    if (domains != null) result.append(domains);
    if (sequences != null) result.append(sequences);
    if (children != null) result.append(children);
    if (owner != null) result.append(owner);

    return result.toString();
  }

}
