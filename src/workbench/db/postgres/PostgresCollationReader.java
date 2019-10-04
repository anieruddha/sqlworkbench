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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.JdbcUtils;
import workbench.db.ObjectListExtender;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to read information about collations from Postgres.
 *
 * @author Thomas Kellerer
 */
public class PostgresCollationReader
  implements ObjectListExtender
{
  public PostgresCollationReader()
  {
  }

  private String getSql(WbConnection con, String schemaNamePattern, String namePattern)
  {
    String providerCol = "null::text as provider";
    if (JdbcUtils.hasMinimumServerVersion(con, "10"))
    {
      providerCol = "case c.collprovider when 'i' then 'icu' when 'c' then 'libc' end as provider";
    }
    StringBuilder sql = new StringBuilder(
      "select s.nspname as schema_name, \n" +
      "       c.collname, \n" +
      "       c.collcollate, \n" +
      "       c.collctype, \n" +
      "       " + providerCol + ", \n" +
      "       obj_description(c.oid) as remarks \n" +
      "from pg_collation c\n" +
      "  join pg_namespace s on s.oid = c.collnamespace");

    boolean whereAdded = false;
    if (StringUtil.isNonBlank(namePattern))
    {
      sql.append("\n WHERE ");
      SqlUtil.appendExpression(sql, "c.collname", namePattern, con);
      whereAdded = true;
    }

    if (StringUtil.isNonBlank(schemaNamePattern))
    {
      if (!whereAdded)
      {
        sql.append("\n WHERE ");
        whereAdded = true;
      }
      else
      {
        sql.append("\n AND ");
      }
      SqlUtil.appendExpression(sql, "s.nspname", schemaNamePattern, con);
    }

    sql.append("\n ORDER BY s.nspname, c.collname ");

    return sql.toString();
  }

  public List<PgCollation> getCollations(WbConnection connection, String schemaPattern, String namePattern)
  {
    Statement stmt = null;
    ResultSet rs = null;
    Savepoint sp = null;
    List<PgCollation> result = new ArrayList<>();
    String sql = getSql(connection, schemaPattern, namePattern);

    LogMgr.logMetadataSql(new CallerInfo(){}, "collations", sql);

    try
    {
      sp = connection.setSavepoint();
      stmt = connection.createStatementForQuery();
      rs = stmt.executeQuery(sql);
      while (rs.next())
      {
        String schema = rs.getString("schema_name");
        String name = rs.getString("collname");
        String collate = rs.getString("collcollate");
        String ctype = rs.getString("collctype");
        String comment = rs.getString("remarks");
        String provider = rs.getString("provider");

        PgCollation coll = new  PgCollation(schema, name);
        coll.setComment(comment);
        coll.setProvider(provider);
        coll.setLocale(collate, ctype);
        result.add(coll);
      }
      connection.releaseSavepoint(sp);
    }
    catch (SQLException e)
    {
      connection.rollback(sp);
      LogMgr.logMetadataError(new CallerInfo(){}, e, "collations", sql);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return result;
  }

  @Override
  public PgCollation getObjectDefinition(WbConnection connection, DbObject object)
  {
    List<PgCollation> rules = getCollations(connection, object.getSchema(), object.getObjectName());
    if (rules == null || rules.isEmpty()) return null;
    return rules.get(0);
  }

  @Override
  public boolean extendObjectList(WbConnection con, DataStore result, String catalog, String schema, String objectNamePattern, String[] requestedTypes)
  {
    if (!DbMetadata.typeIncluded(PgCollation.TYPE_NAME, requestedTypes)) return false;

    List<PgCollation> collations = getCollations(con, schema, objectNamePattern);
    if (collations.isEmpty()) return false;

    for (PgCollation coll : collations)
    {
      int row = result.addRow();
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, null);
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, coll.getSchema());
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, coll.getObjectName());
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, coll.getComment());
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, coll.getObjectType());
      result.getRow(row).setUserObject(coll);
    }
    return true;
  }

  @Override
  public boolean isDerivedType()
  {
    return false;
  }

  @Override
  public boolean handlesType(String type)
  {
    return StringUtil.equalStringIgnoreCase(PgCollation.TYPE_NAME, type);
  }

  @Override
  public boolean handlesType(String[] types)
  {
    if (types == null) return true;
    for (String type : types)
    {
      if (handlesType(type)) return true;
    }
    return false;
  }

  @Override
  public DataStore getObjectDetails(WbConnection con, DbObject object)
  {
    if (object == null) return null;
    if (!handlesType(object.getObjectType())) return null;

    PgCollation coll = getObjectDefinition(con, object);
    if (coll == null) return null;

    String[] columns = new String[] { "SCHEMA",      "COLLATION",   "PROVIDER",    "LC_COLLATE",   "LC_CTYPE",   "REMARKS" };
    int[] types = new int[]         { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR };
    int[] sizes = new int[]         { 20,            20,            5,              20,            20,            50 };
    DataStore result = new DataStore(columns, types, sizes);
    result.addRow();
    result.setValue(0, 0, coll.getSchema());
    result.setValue(0, 1, coll.getObjectName());
    result.setValue(0, 2, coll.getProvider());
    result.setValue(0, 3, coll.getCollate());
    result.setValue(0, 4, coll.getCType());
    result.setValue(0, 5, coll.getComment());

    return result;
  }

  @Override
  public List<String> supportedTypes()
  {
    return CollectionUtil.arrayList(PgCollation.TYPE_NAME);
  }

  @Override
  public String getObjectSource(WbConnection con, DbObject object)
  {
    PgCollation coll = getObjectDefinition(con, object);
    if (coll == null) return null;
    try
    {
      String source = coll.getSource(con).toString();
      return source;
    }
    catch (Exception ex)
    {
      return coll.getSource();
    }
  }

  @Override
  public List<ColumnIdentifier> getColumns(WbConnection con, DbObject object)
  {
    return null;
  }

  @Override
  public boolean hasColumns()
  {
    return false;
  }
}
