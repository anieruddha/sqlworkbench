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
import java.util.ArrayList;
import java.util.List;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.ObjectListExtender;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;


/**
 * A class to read information about extensions from Postgres.
 *
 * @author Thomas Kellerer
 */
public class PostgresExtensionReader
  implements ObjectListExtender
{

  public List<PgExtension> getExtensions(WbConnection connection, String namePattern, String schemaPattern)
  {
    Statement stmt = null;
    ResultSet rs = null;
    Savepoint sp = null;
    List<PgExtension> result = new ArrayList<>();

    StringBuilder sql = new StringBuilder(
      "select nsp.nspname as schema_name, \n" +
      "       ext.extname, \n" +
      "       ext.extversion, \n" +
      "       obj_description(ext.oid) as remarks \n" +
      "from pg_extension ext \n" +
      "  join pg_namespace nsp on nsp.oid = extnamespace \n" +
      "  join pg_user u on u.usesysid = ext.extowner \n" +
      "where ext.extname not in (select l.lanname from pg_language l) ");

    if (StringUtil.isNonBlank(namePattern))
    {
      sql.append("\n  and ");
      SqlUtil.appendExpression(sql, "ext.extname", namePattern, connection);
    }

    if (StringUtil.isNonBlank(schemaPattern))
    {
      sql.append("\n  and ");
      SqlUtil.appendExpression(sql, "nsp.nspname", schemaPattern, connection);
    }

    sql.append("\norder by ext.extname");

    LogMgr.logMetadataSql(new CallerInfo(){}, "extensions", sql);

    try
    {
      sp = connection.setSavepoint();
      stmt = connection.createStatementForQuery();
      rs = stmt.executeQuery(sql.toString());
      while (rs.next())
      {
        String name = rs.getString("extname");
        String schema = rs.getString("schema_name");
        String version = rs.getString("extversion");
        String remarks = rs.getString("remarks");
        PgExtension ext = new PgExtension(schema, name);
        ext.setVersion(version);
        ext.setComment(remarks);
        result.add(ext);
      }
      connection.releaseSavepoint(sp);
    }
    catch (SQLException e)
    {
      connection.rollback(sp);
      LogMgr.logMetadataError(new CallerInfo(){}, e, "extensions", sql);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return result;
  }

  @Override
  public PgExtension getObjectDefinition(WbConnection connection, DbObject object)
  {
    List<PgExtension> extensions = getExtensions(connection, object.getObjectName(), object.getSchema());
    if (extensions == null || extensions.isEmpty()) return null;
    return extensions.get(0);
  }

  @Override
  public boolean extendObjectList(WbConnection con, DataStore result, String catalog, String schema, String objectNamePattern, String[] requestedTypes)
  {
    if (!DbMetadata.typeIncluded(PgExtension.TYPE_NAME, requestedTypes)) return false;

    List<PgExtension> extensions = getExtensions(con, objectNamePattern, schema);
    if (extensions.isEmpty()) return false;
    
    for (PgExtension ext : extensions)
    {
      int row = result.addRow();
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, null);
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, ext.getSchema() );
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, ext.getObjectName());
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, ext.getComment());
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, ext.getObjectType());
      result.getRow(row).setUserObject(ext);
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
    return StringUtil.equalStringIgnoreCase(PgExtension.TYPE_NAME, type);
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
    return null;
  }

  @Override
  public List<String> supportedTypes()
  {
    return CollectionUtil.arrayList(PgExtension.TYPE_NAME);
  }

  @Override
  public String getObjectSource(WbConnection con, DbObject object)
  {
    PgExtension ext = getObjectDefinition(con, object);
    if (ext == null) return null;
    return ext.getSource();
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
