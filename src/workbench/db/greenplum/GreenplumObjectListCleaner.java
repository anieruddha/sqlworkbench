/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2017 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
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
package workbench.db.greenplum;

import java.sql.ResultSet;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbMetadata;
import workbench.db.ObjectListCleaner;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.postgres.PostgresObjectListCleaner;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class GreenplumObjectListCleaner
  implements ObjectListCleaner
{

  public boolean doRemovePartitions()
  {
    return Settings.getInstance().getBoolProperty("workbench.db.greenplum." + PostgresObjectListCleaner.CLEANUP_PARTITIONS_PROP, true);
  }

  @Override
  public void cleanupObjectList(WbConnection con, DataStore result, String catalogPattern, String schemaPattern, String objectNamePattern, String[] requestedTypes)
  {
    if (DbMetadata.typeIncluded("TABLE", requestedTypes) && doRemovePartitions())
    {
      removePartitions(con, result, schemaPattern, objectNamePattern);
    }
  }

  private void removePartitions(WbConnection con, DataStore result, String schemaPattern, String objectNamePattern)
  {
    List<TableIdentifier> partitions = getAllPartitions(con, schemaPattern, objectNamePattern);
    int rowCount = result.getRowCount();
    int deleted = 0;
    long start = System.currentTimeMillis();
    for (int row = rowCount - 1; row >= 0; row --)
    {
      String schema = result.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
      String table = result.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
      TableIdentifier tbl = new TableIdentifier(schema, table);
      if (TableIdentifier.findTableByNameAndSchema(partitions, tbl) != null)
      {
        result.deleteRow(row);
        deleted ++;
      }
    }
    long duration = System.currentTimeMillis() - start;
    LogMgr.logDebug(new CallerInfo(){}, "Removing " + deleted + " partitions from table list took: " + duration + "ms");
  }

  private List<TableIdentifier> getAllPartitions(WbConnection conn, String schemaPattern, String objectNamePattern)
  {
    List<TableIdentifier> partitions = new ArrayList<>();
    StringBuilder sql = new StringBuilder(100);
    sql.append(
      "SELECT partitionschemaname, partitiontablename \n" +
      "FROM pg_partitions ");

    boolean whereAdded = false;
    if (StringUtil.isNonBlank(schemaPattern))
    {
      sql.append(" \nWHERE ");
      SqlUtil.appendExpression(sql, "schemaname", schemaPattern, conn);
      whereAdded = true;
    }

    if (StringUtil.isNonBlank(objectNamePattern))
    {
      sql.append(whereAdded ? "\n AND " : "\n WHERE ");
      SqlUtil.appendExpression(sql, "tablename", objectNamePattern, conn);
    }
    Statement stmt = null;
    ResultSet rs = null;
    Savepoint sp = null;

    final CallerInfo ci = new CallerInfo(){};
    long start = System.currentTimeMillis();
    try
    {
      sp = conn.setSavepoint();

      stmt = conn.createStatementForQuery();
      rs = stmt.executeQuery(sql.toString());

      if (Settings.getInstance().getDebugMetadataSql())
      {
        LogMgr.logInfo(ci, "Retrieving all partitions using:\n" + sql);
      }

      while (rs.next())
      {
        String schema = rs.getString(1);
        String name = rs.getString(2);
        partitions.add(new TableIdentifier(schema, name));
      }

      conn.releaseSavepoint(sp);
    }
    catch (Exception ex)
    {
      conn.rollback(sp);
      LogMgr.logError(ci, "Error retrieving all partitions using:\n" + sql, ex);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }

    long duration = System.currentTimeMillis() - start;
    LogMgr.logDebug(ci, "Reading all partitions took: " + duration + "ms");
    return partitions;
  }
}
