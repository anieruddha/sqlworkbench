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
package workbench.db.redshift;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import workbench.log.CallerInfo;

import workbench.db.ColumnIdentifier;
import workbench.db.DbSettings;
import workbench.db.DomainIdentifier;
import workbench.db.DropType;
import workbench.db.EnumIdentifier;
import workbench.db.IndexDefinition;
import workbench.db.JdbcUtils;
import workbench.db.ObjectSourceOptions;
import workbench.db.TableGrantReader;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.WbConnection;
import workbench.db.postgres.InheritanceEntry;
import workbench.db.postgres.PostgresColumnEnhancer;
import workbench.db.postgres.PostgresDomainReader;
import workbench.db.postgres.PostgresEnumReader;
import workbench.db.postgres.PostgresInheritanceReader;
import workbench.db.postgres.PostgresPartitionReader;
import workbench.db.postgres.PostgresRuleReader;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/*
 * @author  Miguel Cornejo Silva
 */
public class RedshiftTableSourceBuilder
  extends TableSourceBuilder
{

  public RedshiftTableSourceBuilder(WbConnection con)
  {
    super(con);
  }

  @Override
  public String getTableSource(TableIdentifier table, DropType drop, boolean includeFk, boolean includeGrants)
    throws SQLException
  {
    if ("TABLE".equals(table.getType()))
    {
      String sql = getBaseTableSource(table, drop != DropType.none);
      if (sql != null) return sql;
    }
    return super.getTableSource(table, drop, includeFk, includeGrants);
  }

  private String getBaseTableSource(TableIdentifier table, boolean includeDrop)
    throws SQLException
  {
    String sql =
      "SELECT DDL FROM (\n" +
      "  SELECT\n" +
      "   table_id\n" +
      "   ,REGEXP_REPLACE (schemaname, '^zzzzzzzz', '') AS schemaname\n" +
      "   ,REGEXP_REPLACE (tablename, '^zzzzzzzz', '') AS tablename\n" +
      "   ,seq\n" +
      "   ,ddl\n" +
      "  FROM\n" +
      "   (\n" +
      "   SELECT\n" +
      "    table_id\n" +
      "    ,schemaname\n" +
      "    ,tablename\n" +
      "    ,seq\n" +
      "    ,ddl\n" +
      "   FROM\n" +
      "    (\n" +
      "    SELECT";
    if (includeDrop)
    {
      sql +=
        "    --DROP TABLE\n" +
        "     c.oid::bigint as table_id\n" +
        "     ,n.nspname AS schemaname\n" +
        "     ,c.relname AS tablename\n" +
        "     ,0 AS seq\n" +
        "     ,'DROP TABLE ' + QUOTE_IDENT(n.nspname) + '.' + QUOTE_IDENT(c.relname) + ';\\n\\n' AS ddl\n" +
        "    FROM pg_namespace AS n\n" +
        "    INNER JOIN pg_class AS c ON n.oid = c.relnamespace\n" +
        "    WHERE c.relkind = 'r'\n" +
        "    UNION SELECT";
    }
    sql +=
      "    --CREATE TABLE\n" +
      "     c.oid::bigint as table_id\n" +
      "     ,n.nspname AS schemaname\n" +
      "     ,c.relname AS tablename\n" +
      "     ,2 AS seq\n" +
      "     ,'CREATE TABLE IF NOT EXISTS ' + QUOTE_IDENT(n.nspname) + '.' + QUOTE_IDENT(c.relname) + '\\n' AS ddl\n" +
      "    FROM pg_namespace AS n\n" +
      "    INNER JOIN pg_class AS c ON n.oid = c.relnamespace\n" +
      "    WHERE c.relkind = 'r'\n" +
      "    --OPEN PAREN COLUMN LIST\n" +
      "    UNION SELECT c.oid::bigint as table_id,n.nspname AS schemaname, c.relname AS tablename, 5 AS seq, '(\\n' AS ddl\n" +
      "    FROM pg_namespace AS n\n" +
      "    INNER JOIN pg_class AS c ON n.oid = c.relnamespace\n" +
      "    WHERE c.relkind = 'r'\n" +
      "    --COLUMN LIST\n" +
      "    UNION SELECT\n" +
      "     table_id\n" +
      "     ,schemaname\n" +
      "     ,tablename\n" +
      "     ,seq\n" +
      "     ,'\\t' + col_delim + col_name + ' ' + col_datatype + ' ' + col_nullable + ' ' + col_default + ' ' + col_encoding + '\\n' AS ddl\n" +
      "    FROM\n" +
      "     (\n" +
      "     SELECT\n" +
      "      c.oid::bigint as table_id\n" +
      "     ,n.nspname AS schemaname\n" +
      "      ,c.relname AS tablename\n" +
      "      ,100000000 + a.attnum AS seq\n" +
      "      ,CASE WHEN a.attnum > 1 THEN ',' ELSE '' END AS col_delim\n" +
      "      ,QUOTE_IDENT(a.attname) AS col_name\n" +
      "      ,CASE WHEN STRPOS(UPPER(format_type(a.atttypid, a.atttypmod)), 'CHARACTER VARYING') > 0\n" +
      "        THEN REPLACE(UPPER(format_type(a.atttypid, a.atttypmod)), 'CHARACTER VARYING', 'VARCHAR')\n" +
      "       WHEN STRPOS(UPPER(format_type(a.atttypid, a.atttypmod)), 'CHARACTER') > 0\n" +
      "        THEN REPLACE(UPPER(format_type(a.atttypid, a.atttypmod)), 'CHARACTER', 'CHAR')\n" +
      "       ELSE UPPER(format_type(a.atttypid, a.atttypmod))\n" +
      "       END AS col_datatype\n" +
      "      ,CASE WHEN format_encoding((a.attencodingtype)::integer) = 'none'\n" +
      "       THEN ''\n" +
      "       ELSE 'ENCODE ' + format_encoding((a.attencodingtype)::integer)\n" +
      "       END AS col_encoding\n" +
      "      ,CASE WHEN a.atthasdef IS TRUE THEN 'DEFAULT ' + adef.adsrc ELSE '' END AS col_default\n" +
      "      ,CASE WHEN a.attnotnull IS TRUE THEN 'NOT NULL' ELSE '' END AS col_nullable\n" +
      "     FROM pg_namespace AS n\n" +
      "     INNER JOIN pg_class AS c ON n.oid = c.relnamespace\n" +
      "     INNER JOIN pg_attribute AS a ON c.oid = a.attrelid\n" +
      "     LEFT OUTER JOIN pg_attrdef AS adef ON a.attrelid = adef.adrelid AND a.attnum = adef.adnum\n" +
      "     WHERE c.relkind = 'r'\n" +
      "       AND a.attnum > 0\n" +
      "     ORDER BY a.attnum\n" +
      "     )\n" +
      "    --CONSTRAINT LIST\n" +
      "    UNION (SELECT\n" +
      "     c.oid::bigint as table_id\n" +
      "     ,n.nspname AS schemaname\n" +
      "     ,c.relname AS tablename\n" +
      "     ,200000000 + CAST(con.oid AS INT) AS seq\n" +
      "     ,'\\t,' + pg_get_constraintdef(con.oid) + '\\n' AS ddl\n" +
      "    FROM pg_constraint AS con\n" +
      "    INNER JOIN pg_class AS c ON c.relnamespace = con.connamespace AND c.oid = con.conrelid\n" +
      "    INNER JOIN pg_namespace AS n ON n.oid = c.relnamespace\n" +
      "    WHERE c.relkind = 'r' AND pg_get_constraintdef(con.oid) NOT LIKE 'FOREIGN KEY%'\n" +
      "    ORDER BY seq)\n" +
      "    --CLOSE PAREN COLUMN LIST\n" +
      "    UNION SELECT c.oid::bigint as table_id,n.nspname AS schemaname, c.relname AS tablename, 299999999 AS seq, ')\\n' AS ddl\n" +
      "    FROM pg_namespace AS n\n" +
      "    INNER JOIN pg_class AS c ON n.oid = c.relnamespace\n" +
      "    WHERE c.relkind = 'r'\n" +
      "    --BACKUP\n" +
      "    UNION SELECT\n" +
      "    c.oid::bigint as table_id\n" +
      "     ,n.nspname AS schemaname\n" +
      "     ,c.relname AS tablename\n" +
      "     ,300000000 AS seq\n" +
      "     ,'BACKUP NO\\n' as ddl\n" +
      "  FROM pg_namespace AS n\n" +
      "    INNER JOIN pg_class AS c ON n.oid = c.relnamespace\n" +
      "    INNER JOIN (SELECT\n" +
      "      SPLIT_PART(key,'_',5) id\n" +
      "      FROM pg_conf\n" +
      "      WHERE key LIKE 'pg_class_backup_%'\n" +
      "      AND SPLIT_PART(key,'_',4) = (SELECT\n" +
      "        oid\n" +
      "        FROM pg_database\n" +
      "        WHERE datname = current_database())) t ON t.id=c.oid\n" +
      "    WHERE c.relkind = 'r'\n" +
      "    --BACKUP WARNING\n" +
      "    UNION SELECT\n" +
      "    c.oid::bigint as table_id\n" +
      "     ,n.nspname AS schemaname\n" +
      "     ,c.relname AS tablename\n" +
      "     ,1 AS seq\n" +
      "     ,'--WARNING: This DDL inherited the BACKUP NO property from the source table\\n' as ddl\n" +
      "  FROM pg_namespace AS n\n" +
      "    INNER JOIN pg_class AS c ON n.oid = c.relnamespace\n" +
      "    INNER JOIN (SELECT\n" +
      "      SPLIT_PART(key,'_',5) id\n" +
      "      FROM pg_conf\n" +
      "      WHERE key LIKE 'pg_class_backup_%'\n" +
      "      AND SPLIT_PART(key,'_',4) = (SELECT\n" +
      "        oid\n" +
      "        FROM pg_database\n" +
      "        WHERE datname = current_database())) t ON t.id=c.oid\n" +
      "    WHERE c.relkind = 'r'\n" +
      "    --DISTSTYLE\n" +
      "    UNION SELECT\n" +
      "     c.oid::bigint as table_id\n" +
      "     ,n.nspname AS schemaname\n" +
      "     ,c.relname AS tablename\n" +
      "     ,300000001 AS seq\n" +
      "     ,CASE WHEN c.reldiststyle = 0 THEN 'DISTSTYLE EVEN'\n" +
      "      WHEN c.reldiststyle = 1 THEN 'DISTSTYLE KEY'\n" +
      "      WHEN c.reldiststyle = 8 THEN 'DISTSTYLE ALL'\n" +
      "      ELSE '<<Error - UNKNOWN DISTSTYLE>>'\n" +
      "      END + '\\n' AS ddl\n" +
      "    FROM pg_namespace AS n\n" +
      "    INNER JOIN pg_class AS c ON n.oid = c.relnamespace\n" +
      "    WHERE c.relkind = 'r'\n" +
      "    --DISTKEY COLUMNS\n" +
      "    UNION SELECT\n" +
      "     c.oid::bigint as table_id\n" +
      "     ,n.nspname AS schemaname\n" +
      "     ,c.relname AS tablename\n" +
      "     ,400000000 + a.attnum AS seq\n" +
      "     ,'DISTKEY (' + QUOTE_IDENT(a.attname) + ')\\n' AS ddl\n" +
      "    FROM pg_namespace AS n\n" +
      "    INNER JOIN pg_class AS c ON n.oid = c.relnamespace\n" +
      "    INNER JOIN pg_attribute AS a ON c.oid = a.attrelid\n" +
      "    WHERE c.relkind = 'r'\n" +
      "      AND a.attisdistkey IS TRUE\n" +
      "      AND a.attnum > 0\n" +
      "    --SORTKEY COLUMNS\n" +
      "    UNION select table_id,schemaname, tablename, seq,\n" +
      "         case when min_sort <0 then 'INTERLEAVED SORTKEY (' else 'SORTKEY (' end as ddl\n" +
      "  from (SELECT\n" +
      "     c.oid::bigint as table_id\n" +
      "     ,n.nspname AS schemaname\n" +
      "     ,c.relname AS tablename\n" +
      "     ,499999999 AS seq\n" +
      "     ,min(attsortkeyord) min_sort FROM pg_namespace AS n\n" +
      "    INNER JOIN  pg_class AS c ON n.oid = c.relnamespace\n" +
      "    INNER JOIN pg_attribute AS a ON c.oid = a.attrelid\n" +
      "    WHERE c.relkind = 'r'\n" +
      "    AND abs(a.attsortkeyord) > 0\n" +
      "    AND a.attnum > 0\n" +
      "    group by 1,2,3,4 )\n" +
      "    UNION (SELECT\n" +
      "     c.oid::bigint as table_id\n" +
      "     ,n.nspname AS schemaname\n" +
      "     ,c.relname AS tablename\n" +
      "     ,500000000 + abs(a.attsortkeyord) AS seq\n" +
      "     ,CASE WHEN abs(a.attsortkeyord) = 1\n" +
      "      THEN QUOTE_IDENT(a.attname)\n" +
      "      ELSE ', ' + QUOTE_IDENT(a.attname)\n" +
      "      END AS ddl\n" +
      "    FROM  pg_namespace AS n\n" +
      "    INNER JOIN pg_class AS c ON n.oid = c.relnamespace\n" +
      "    INNER JOIN pg_attribute AS a ON c.oid = a.attrelid\n" +
      "    WHERE c.relkind = 'r'\n" +
      "      AND abs(a.attsortkeyord) > 0\n" +
      "      AND a.attnum > 0\n" +
      "    ORDER BY abs(a.attsortkeyord))\n" +
      "    UNION SELECT\n" +
      "     c.oid::bigint as table_id\n" +
      "     ,n.nspname AS schemaname\n" +
      "     ,c.relname AS tablename\n" +
      "     ,599999999 AS seq\n" +
      "     ,')\\n' AS ddl\n" +
      "    FROM pg_namespace AS n\n" +
      "    INNER JOIN  pg_class AS c ON n.oid = c.relnamespace\n" +
      "    INNER JOIN  pg_attribute AS a ON c.oid = a.attrelid\n" +
      "    WHERE c.relkind = 'r'\n" +
      "      AND abs(a.attsortkeyord) > 0\n" +
      "      AND a.attnum > 0\n" +
      "    --END SEMICOLON\n" +
      "    UNION SELECT c.oid::bigint as table_id ,n.nspname AS schemaname, c.relname AS tablename, 600000000 AS seq, ';\\n' AS ddl\n" +
      "    FROM  pg_namespace AS n\n" +
      "    INNER JOIN pg_class AS c ON n.oid = c.relnamespace\n" +
      "    WHERE c.relkind = 'r' )\n" +
      "    UNION (\n" +
      "      SELECT c.oid::bigint as table_id,'zzzzzzzz' || n.nspname AS schemaname,\n" +
      "         'zzzzzzzz' || c.relname AS tablename,\n" +
      "         700000000 + CAST(con.oid AS INT) AS seq,\n" +
      "         'ALTER TABLE ' + QUOTE_IDENT(n.nspname) + '.' + QUOTE_IDENT(c.relname) + ' ADD ' + pg_get_constraintdef(con.oid)::VARCHAR(1024) + ';' AS ddl\n" +
      "      FROM pg_constraint AS con\n" +
      "        INNER JOIN pg_class AS c\n" +
      "               ON c.relnamespace = con.connamespace\n" +
      "               AND c.oid = con.conrelid\n" +
      "        INNER JOIN pg_namespace AS n ON n.oid = c.relnamespace\n" +
      "      WHERE c.relkind = 'r'\n" +
      "      AND con.contype = 'f'\n" +
      "      ORDER BY seq\n" +
      "    )\n" +
      "   ORDER BY table_id,schemaname, tablename, seq\n" +
      "   )\n" +
      " ) X WHERE schemaname = ? and tablename = ? \n" +
      " order by schemaname, tablename, seq";

    StringBuilder createSql = new StringBuilder(100);

    LogMgr.logMetadataSql(new CallerInfo(){}, "table source", sql, table.getSchema(), table.getTableName());

    PreparedStatement stmt = null;
    ResultSet rs = null;

    try
    {
      stmt = dbConnection.getSqlConnection().prepareStatement(sql);
      stmt.setString(1, table.getSchema());
      stmt.setString(2, table.getTableName());
      rs = stmt.executeQuery();
      while (rs.next())
      {
        String create = rs.getString(1);
        createSql.append(create);
      }
    }
    catch (SQLException ex)
    {
      LogMgr.logMetadataError(new CallerInfo(){}, ex, "table source", sql, table.getSchema(), table.getTableName());
      return null;
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }

    TableGrantReader grantReader = TableGrantReader.createReader(dbConnection);
    StringBuilder grants = grantReader.getTableGrantSource(this.dbConnection, table);
    if (grants.length() > 0)
    {
      createSql.append("\n");
      createSql.append(grants);
    }

    return createSql.toString();
  }

  @Override
  public void readTableOptions(TableIdentifier table, List<ColumnIdentifier> columns)
  {
    ObjectSourceOptions option = table.getSourceOptions();
    if (option.isInitialized())
      return;

    PostgresRuleReader ruleReader = new PostgresRuleReader();
    CharSequence rule = ruleReader.getTableRuleSource(dbConnection, table);
    if (rule != null)
    {
      option.setAdditionalSql(rule.toString());
    }

    if ("FOREIGN TABLE".equals(table.getType()))
    {
      readForeignTableOptions(table);
    }
    else
    {
      readTableOptions(table);
    }
    option.setInitialized();
  }

  private void readTableOptions(TableIdentifier tbl)
  {
    if (!JdbcUtils.hasMinimumServerVersion(dbConnection, "8.1")) return;

    ObjectSourceOptions option = tbl.getSourceOptions();
    StringBuilder inherit = readInherits(tbl);

    StringBuilder tableSql = new StringBuilder();

    String persistenceCol = null;
    if (JdbcUtils.hasMinimumServerVersion(dbConnection, "9.1"))
    {
      persistenceCol = "ct.relpersistence";
    }
    else if (JdbcUtils.hasMinimumServerVersion(dbConnection, "8.4"))
    {
      persistenceCol = "case when ct.relistemp then 't' else null::char end as relpersitence";
    }
    else
    {
      persistenceCol = "null::char as relpersistence";
    }

    String spcnameCol;
    String defaultTsCol;
    String defaultTsQuery;

    boolean showNonStandardTablespace = dbConnection.getDbSettings().getBoolProperty("show.nonstandard.tablespace",
      true);

    if (JdbcUtils.hasMinimumServerVersion(dbConnection, "8.0"))
    {
      spcnameCol = "spc.spcname";
      defaultTsCol = "ts.default_tablespace";
      defaultTsQuery =
        "  cross join (\n" +
        "    select ts.spcname as default_tablespace\n" +
        "    from pg_database d\n" +
        "      join pg_tablespace ts on ts.oid = d.dattablespace\n" +
        "    where d.datname = current_database()\n" +
        "  ) ts \n ";
    }
    else
    {
      spcnameCol = "null as spcname";
      defaultTsCol = "null as default_tablespace";
      defaultTsQuery = "";
      showNonStandardTablespace = false;
    }

    PreparedStatement pstmt = null;
    ResultSet rs = null;

    String sql =
      "select " + persistenceCol + ", \n" +
      "       ct.relkind, \n" +
      "       array_to_string(ct.reloptions, ', ') as options, \n" +
      "       " + spcnameCol + ", \n" +
      "       own.rolname as owner, \n" +
      "       " + defaultTsCol + " \n" +
      "from pg_catalog.pg_class ct \n" +
      "  join pg_catalog.pg_namespace cns on ct.relnamespace = cns.oid \n " +
      "  join pg_catalog.pg_roles own on ct.relowner = own.oid \n " +
      "  left join pg_catalog.pg_tablespace spc on spc.oid = ct.reltablespace \n" + defaultTsQuery +
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

      LogMgr.logMetadataSql(new CallerInfo(){}, "table options", sql, tbl.getSchema(), tbl.getTableName());

      rs = pstmt.executeQuery();

      if (rs.next())
      {
        String persistence = rs.getString("relpersistence");
        String type = rs.getString("relkind");
        String settings = rs.getString("options");
        String tableSpace = rs.getString("spcname");
        String owner = rs.getString("owner");
        String defaultTablespace = rs.getString("default_tablespace");

        if (showNonStandardTablespace && !"pg_default".equals(defaultTablespace) &&
          StringUtil.isEmptyString(tableSpace))
        {
          tableSpace = defaultTablespace;
        }

        tbl.setOwner(owner);
        tbl.setTablespace(tableSpace);

        if (StringUtil.isNonEmpty(persistence))
        {
          switch (persistence.charAt(0))
          {
            case 'u':
              option.setTypeModifier("UNLOGGED");
              break;
            case 't':
              option.setTypeModifier("TEMPORARY");
              break;
          }
        }

        if ("f".equalsIgnoreCase(type))
        {
          option.setTypeModifier("FOREIGN");
        }

        isPartitioned = "p".equals(type);

        if (!isPartitioned && inherit != null)
        {
          if (tableSql.length() > 0)
            tableSql.append('\n');
          tableSql.append(inherit);
        }

        if (StringUtil.isNonEmpty(settings))
        {
          setConfigSettings(settings, option);
          if (tableSql.length() > 0) tableSql.append('\n');
          tableSql.append("WITH (");
          tableSql.append(settings);
          tableSql.append(")");
        }

        if (StringUtil.isNonBlank(tableSpace))
        {
          if (tableSql.length() > 0)
            tableSql.append('\n');
          tableSql.append("TABLESPACE ");
          tableSql.append(tableSpace);
        }
      }
      dbConnection.releaseSavepoint(sp);
    }
    catch (SQLException e)
    {
      dbConnection.rollback(sp);
      LogMgr.logError("PostgresTableSourceBuilder.readTableOptions()", "Error retrieving table options using:\n" +
        SqlUtil.replaceParameters(sql, tbl.getSchema(), tbl.getTableName()), e);
    }
    finally
    {
      SqlUtil.closeAll(rs, pstmt);
    }
    option.setTableOption(tableSql.toString());

    if (isPartitioned)
    {
      handlePartitions(tbl);
    }
  }

  private void setConfigSettings(String options, ObjectSourceOptions tblOption)
  {
    List<String> l = StringUtil.stringToList(options, ",", true, true, false, true);
    for (String s : l)
    {
      String[] opt = s.split("=");
      if (opt.length == 2)
      {
        tblOption.addConfigSetting(opt[0], opt[1]);
      }
    }
  }

  private void handlePartitions(TableIdentifier table)
  {
    PostgresPartitionReader reader = new PostgresPartitionReader(table, dbConnection);
    reader.readPartitionInformation();
    ObjectSourceOptions option = table.getSourceOptions();
    String def = reader.getPartitionDefinition();
    String sql = option.getTableOption();
    if (sql == null)
    {
      sql = def;
    }
    else
    {
      sql = def + "\n" + sql;
    }
    option.setTableOption(sql);
    option.addConfigSetting(PostgresPartitionReader.OPTION_KEY_STRATEGY, reader.getStrategy().toLowerCase());
    option.addConfigSetting(PostgresPartitionReader.OPTION_KEY_EXPRESSION, reader.getPartitionExpression());

    String createPartitions = reader.getCreatePartitions();
    if (createPartitions != null)
    {
      option.setAdditionalSql(createPartitions);
    }
  }

  private StringBuilder readInherits(TableIdentifier table)
  {
    if (table == null)
      return null;

    StringBuilder result = null;
    PostgresInheritanceReader reader = new PostgresInheritanceReader();

    List<TableIdentifier> parents = reader.getParents(dbConnection, table);
    if (CollectionUtil.isEmpty(parents))
      return null;

    result = new StringBuilder(parents.size() * 30);
    result.append("INHERITS (");

    for (int i = 0; i < parents.size(); i++)
    {
      TableIdentifier tbl = parents.get(i);
      table.getSourceOptions().addConfigSetting("inherits", tbl.getTableName());
      result.append(tbl.getTableName());
      if (i > 0)
        result.append(',');
    }
    result.append(')');

    return result;
  }

  public void readForeignTableOptions(TableIdentifier table)
  {
    ObjectSourceOptions option = table.getSourceOptions();

    String sql =
      "select ft.ftoptions, fs.srvname \n" +
      "from pg_foreign_table ft \n" +
      "  join pg_class tbl on tbl.oid = ft.ftrelid  \n" +
      "  join pg_namespace ns on tbl.relnamespace = ns.oid  \n" +
      "  join pg_foreign_server fs on ft.ftserver = fs.oid \n " +
      " WHERE tbl.relname = ? \n" +
      "   and ns.nspname = ? ";

    PreparedStatement stmt = null;
    ResultSet rs = null;
    StringBuilder result = new StringBuilder(100);
    Savepoint sp = null;
    try
    {
      sp = dbConnection.setSavepoint();
      stmt = dbConnection.getSqlConnection().prepareStatement(sql);
      stmt.setString(1, table.getRawTableName());
      stmt.setString(2, table.getRawSchema());

      if (Settings.getInstance().getDebugMetadataSql())
      {
        LogMgr.logDebug("PostgresTableSourceBuilder.readForeignTableOptions()", "Retrieving table options using:\n" +
          SqlUtil.replaceParameters(sql, table.getSchema(), table.getTableName()));
      }

      rs = stmt.executeQuery();
      if (rs.next())
      {
        Array array = rs.getArray(1);
        String[] options = array == null ? null : (String[])array.getArray();
        String serverName = rs.getString(2);
        result.append("SERVER ");
        result.append(serverName);
        if (options != null && options.length > 0)
        {
          result.append("\nOPTIONS (");
          for (int i = 0; i < options.length; i++)
          {
            if (i > 0)
            {
              result.append(", ");
            }
            String[] optValues = options[i].split("=");
            if (optValues.length == 2)
            {
              result.append(optValues[0] + " '" + optValues[1] + "'");
            }
          }
          result.append(')');
        }
        option.setTableOption(result.toString());
      }
    }
    catch (SQLException ex)
    {
      dbConnection.rollback(sp);
      sp = null;
      LogMgr.logMetadataError(new CallerInfo(){}, ex, "table options", sql, table.getSchema(), table.getTableName());
    }
    finally
    {
      dbConnection.releaseSavepoint(sp);
      SqlUtil.closeAll(rs, stmt);
    }
  }

  @Override
  public String getAdditionalTableInfo(TableIdentifier table, List<ColumnIdentifier> columns,
                                       List<IndexDefinition> indexList)
  {
    String schema = table.getSchemaToUse(this.dbConnection);
    CharSequence enums = getEnumInformation(columns, schema);
    CharSequence domains = getDomainInformation(columns, schema);
    CharSequence sequences = getColumnSequenceInformation(table, columns);
    CharSequence stats = null;
    CharSequence children = null;
    ObjectSourceOptions sourceOptions = table.getSourceOptions();
    if (sourceOptions.getConfigSettings().get(PostgresPartitionReader.OPTION_KEY_STRATEGY) == null)
    {
      children = getChildTables(table);
    }
    StringBuilder storage = getColumnStorage(table, columns);
    String owner = getOwnerSql(table);

    if (StringUtil.allEmpty(enums, domains, sequences, children, owner, storage, stats))
      return null;

    StringBuilder result = new StringBuilder(200);

    if (storage != null) result.append(storage);
    if (enums != null) result.append(enums);
    if (domains != null) result.append(domains);
    if (sequences != null) result.append(sequences);
    if (stats != null) result.append(stats);
    if (children != null) result.append(children);
    if (owner != null) result.append(owner);

    return result.toString();
  }

  private StringBuilder getColumnStorage(TableIdentifier table, List<ColumnIdentifier> columns)
  {
    StringBuilder result = null;
    String tname = table.getTableExpression(dbConnection);

    for (ColumnIdentifier col : columns)
    {
      int storage = col.getPgStorage();
      String option = PostgresColumnEnhancer.getStorageOption(storage);
      if (option != null && !isDefaultStorage(col.getDataType(), storage))
      {
        if (result == null)
        {
          result = new StringBuilder(50);
          result.append('\n');
        }
        result.append("ALTER TABLE ");
        result.append(tname);
        result.append(" ALTER ");
        result.append(dbConnection.getMetadata().quoteObjectname(col.getColumnName()));
        result.append(" SET STORAGE ");
        result.append(option);
        result.append(";\n");
      }
    }
    return result;
  }

  private boolean isDefaultStorage(int columnType, int storage)
  {
    if (columnType == Types.NUMERIC && storage == PostgresColumnEnhancer.STORAGE_MAIN)
      return true;
    return storage == PostgresColumnEnhancer.STORAGE_EXTENDED;
  }

  private String getOwnerSql(TableIdentifier table)
  {
    try
    {
      DbSettings.GenerateOwnerType genType = dbConnection.getDbSettings().getGenerateTableOwner();
      if (genType == DbSettings.GenerateOwnerType.never)
        return null;

      String owner = table.getOwner();
      if (StringUtil.isBlank(owner))
        return null;

      if (genType == DbSettings.GenerateOwnerType.whenNeeded)
      {
        String user = dbConnection.getCurrentUser();
        if (user.equalsIgnoreCase(owner))
          return null;
      }

      return "\nALTER TABLE " + table.getFullyQualifiedName(dbConnection) + " OWNER TO " +
        SqlUtil.quoteObjectname(owner) + ";";
    }
    catch (Exception ex)
    {
      return null;
    }
  }

  private CharSequence getColumnSequenceInformation(TableIdentifier table, List<ColumnIdentifier> columns)
  {
    if (!JdbcUtils.hasMinimumServerVersion(this.dbConnection, "8.4"))
      return null;
    if (table == null)
      return null;
    if (CollectionUtil.isEmpty(columns))
      return null;
    String tblname = table.getTableExpression(dbConnection);
    ResultSet rs = null;
    Statement stmt = null;
    StringBuilder b = new StringBuilder(100);

    Savepoint sp = null;
    String sql = null;
    try
    {
      sp = dbConnection.setSavepoint();
      stmt = dbConnection.createStatementForQuery();
      for (ColumnIdentifier col : columns)
      {
        String defaultValue = col.getDefaultValue();
        // if the default value is shown as nextval, the sequence name is already
        // visible
        if (defaultValue != null && defaultValue.toLowerCase().contains("nextval"))
          continue;

        String colname = StringUtil.trimQuotes(col.getColumnName());
        sql = "select pg_get_serial_sequence('" + tblname + "', '" + colname + "')";
        rs = stmt.executeQuery(sql);
        if (rs.next())
        {
          String seq = rs.getString(1);
          if (StringUtil.isNonBlank(seq))
          {
            String msg = ResourceMgr.getFormattedString("TxtSequenceCol", col.getColumnName(), seq);
            b.append("\n-- ");
            b.append(msg);
          }
        }
      }
      dbConnection.releaseSavepoint(sp);
    }
    catch (Exception e)
    {
      dbConnection.rollback(sp);
      LogMgr.logWarning("PostgresTableSourceBuilder.getColumnSequenceInformation()",
        "Error reading sequence information using: " + sql, e);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    if (b.length() == 0)
      return null;
    return b;
  }

  private CharSequence getEnumInformation(List<ColumnIdentifier> columns, String schema)
  {
    PostgresEnumReader reader = new PostgresEnumReader();
    Map<String, EnumIdentifier> enums = reader.getEnumInfo(dbConnection, schema, null);
    if (CollectionUtil.isEmpty(enums))
      return null;

    StringBuilder result = null;

    for (ColumnIdentifier col : columns)
    {
      String dbType = col.getDbmsType();
      EnumIdentifier enumDef = enums.get(dbType);
      if (enumDef != null)
      {
        if (result == null)
          result = new StringBuilder(50);
        result.append("\n-- enum '");
        result.append(dbType);
        result.append("': ");
        result.append(StringUtil.listToString(enumDef.getValues(), ",", true, '\''));
      }
    }

    return result;
  }

  public CharSequence getDomainInformation(List<ColumnIdentifier> columns, String schema)
  {
    PostgresDomainReader reader = new PostgresDomainReader();
    Map<String, DomainIdentifier> domains = reader.getDomainInfo(dbConnection, schema);
    if (domains == null || domains.isEmpty())
      return null;
    StringBuilder result = null;

    for (ColumnIdentifier col : columns)
    {
      String dbType = col.getDbmsType();
      DomainIdentifier domain = domains.get(dbType);
      if (domain != null)
      {
        if (result == null)
          result = new StringBuilder(50);
        result.append("\n-- domain '");
        result.append(dbType);
        result.append("': ");
        result.append(domain.getSummary());
      }
    }

    return result;
  }

  protected CharSequence getChildTables(TableIdentifier table)
  {
    if (table == null)
      return null;

    StringBuilder result = null;

    PostgresInheritanceReader reader = new PostgresInheritanceReader();

    List<InheritanceEntry> tables = reader.getChildren(dbConnection, table);
    final boolean is84 = JdbcUtils.hasMinimumServerVersion(dbConnection, "8.4");

    for (int i = 0; i < tables.size(); i++)
    {
      if (i == 0)
      {
        result = new StringBuilder(50);
        if (is84)
        {
          result.append("\n/* Inheritance tree:\n\n");
          result.append(table.getSchema());
          result.append('.');
          result.append(table.getTableName());
        }
        else
        {
          result.append("\n-- Child tables:");
        }
      }
      String tableName = tables.get(i).getTable().getTableName();
      String schemaName = tables.get(i).getTable().getSchema();
      int level = tables.get(i).getLevel();
      if (is84)
      {
        result.append('\n');
        result.append(StringUtil.padRight(" ", level * 2));
      }
      else
      {
        result.append("\n--  ");
      }
      result.append(schemaName);
      result.append('.');
      result.append(tableName);
    }
    if (is84 && result != null)
    {
      result.append("\n*/");
    }
    return result;
  }

}
