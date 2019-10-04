/*
 * ResultColumnMetaData.java
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
package workbench.storage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.Alias;
import workbench.util.CaseInsensitiveComparator;
import workbench.util.CollectionUtil;
import workbench.util.SelectColumn;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to retrieve additional column meta data for result (query)
 * columns from a datastore.
 *
 * @author Thomas Kellerer
 */
public class ResultColumnMetaData
{
  private List<Alias> tables;
  private List<String> queryColumns;
  private WbConnection connection;

  public ResultColumnMetaData(WbConnection conn)
  {
    connection = conn;
  }

  public ResultColumnMetaData(DataStore ds)
  {
    this(ds.getGeneratingSql(), ds.getOriginalConnection());
  }

  public ResultColumnMetaData(String sql, WbConnection conn)
  {
    connection = conn;
    if (StringUtil.isBlank(sql)) return;

    tables = SqlUtil.getTables(sql, true, conn);
    queryColumns = SqlUtil.getSelectColumns(sql, true, conn);
  }

  public void retrieveColumnRemarks(ResultInfo info)
    throws SQLException
  {
    DbMetadata meta = connection.getMetadata();

    Map<String, TableDefinition> tableDefs = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);
    for (Alias alias : tables)
    {
      if (StringUtil.isBlank(alias.getNameToUse())) continue;

      TableIdentifier tbl = new TableIdentifier(alias.getObjectName(), connection);
      TableDefinition def = meta.getTableDefinition(tbl);
      tableDefs.put(alias.getNameToUse().toLowerCase(), def);
    }

    updateFromQueryColumns(info, tableDefs);
  }


  private void updateFromQueryColumns(ResultInfo info, Map<String, TableDefinition> tableDefs)
  {
    if (CollectionUtil.isEmpty(queryColumns)) return;

    List<SelectColumn> columns = expandQueryColumns(tableDefs);

    for (SelectColumn c : columns)
    {
      TableDefinition def = findTableForColumn(c, tableDefs);
      if (def != null)
      {
        ColumnIdentifier tcol = def.findColumn(c.getObjectName());
        ColumnIdentifier resultCol = findResultColumn(info, c);
        if (resultCol != null)
        {
          resultCol.setComment(tcol.getComment());
          resultCol.setSourceTableName(def.getTable().getRawTableName());
        }
      }
    }
  }

  private ColumnIdentifier findResultColumn(ResultInfo info, SelectColumn toFind)
  {
    DbMetadata meta = connection.getMetadata();

    String findName = meta.removeQuotes(toFind.getObjectName());
    String findAlias = meta.removeQuotes(toFind.getAlias());
    String findExpr = meta.removeQuotes(toFind.getNameToUse());
    String findTable = meta.removeQuotes(toFind.getColumnTable());

    if (findTable != null && findExpr != null && connection.getDbSettings().supportsResultMetaGetTable())
    {
      ColumnIdentifier col = findByTableAndColumn(info, findName, findTable);
      if (col != null)
      {
        return col;
      }
    }

    int queryIndex = toFind.getIndexInResult();
    if (queryIndex > -1 && queryIndex < info.getColumnCount())
    {
      return info.getColumn(queryIndex);
    }


    for (ColumnIdentifier col : info.getColumns())
    {
      String colName = meta.removeQuotes(col.getColumnName());
      String colAlias = meta.removeQuotes(col.getColumnAlias());
      if (findAlias != null && colAlias != null && findAlias.equalsIgnoreCase(colAlias)) return col;
      if (findName != null && colName != null && findName.equalsIgnoreCase(colName)) return col;
      if (findExpr != null && findExpr.equalsIgnoreCase(colName)) return col;
    }
    return null;
  }

  private ColumnIdentifier findByTableAndColumn(ResultInfo info, String selectCol, String selectTable)
  {
    DbMetadata meta = connection.getMetadata();

    for (ColumnIdentifier col : info.getColumns())
    {
      String colName = meta.removeQuotes(col.getColumnName());
      String table = meta.removeQuotes(col.getSourceTableName());
      if (StringUtil.equalStringIgnoreCase(table, selectTable) && StringUtil.equalStringIgnoreCase(colName, selectCol))
      {
        return col;
      }
    }
    return null;
  }
  /**
   * Try to expand wildcard "columns" to the real columns.
   */
  private List<SelectColumn> expandQueryColumns(Map<String, TableDefinition> tableDefs)
  {
    List<SelectColumn> result = new ArrayList<>();

    if (queryColumns.size() == 1 && queryColumns.get(0).equals("*"))
    {
      int index = 0;
      // easy case, just process all tables in the order they were specified
      for (Alias alias : tables)
      {
        TableDefinition tdef = tableDefs.get(alias.getNameToUse());
        if (tdef == null) continue;
        for (ColumnIdentifier col : tdef.getColumns())
        {
          SelectColumn c = new SelectColumn(col.getColumnName());
          c.setColumnTable(tdef.getTable().getRawTableName());
          c.setIndexInResult(index++);
          result.add(c);
        }
      }
      return result;
    }

    int index = 0;
    for (String col : queryColumns)
    {
      SelectColumn c = new SelectColumn(col);

      String tname = c.getColumnTable();
      if (StringUtil.isBlank(tname))
      {
        c.setIndexInResult(index++);
        result.add(c);
        continue;
      }

      TableDefinition tdef = tableDefs.get(c.getColumnTable());
      if (tdef != null)
      {
        if (c.getObjectName().equals("*"))
        {
          for (ColumnIdentifier cid : tdef.getColumns())
          {
            SelectColumn sc = new SelectColumn(cid.getColumnName());
            sc.setColumnTable(tdef.getTable().getRawTableName());
            sc.setIndexInResult(index++);
            result.add(sc);
          }
        }
        else
        {
          c.setColumnTable(tname);
          c.setIndexInResult(index++);
          result.add(c);
        }
      }
    }
    return result;
  }

  private String getTableNameForAlias(String alias)
  {
    if (StringUtil.isEmptyString(alias)) return null;
    for (Alias table : tables)
    {
      if (StringUtil.equalStringOrEmpty(table.getAlias(), alias))
      {
        return table.getObjectName();
      }
    }
    return alias;
  }

  private TableDefinition findTableForColumn(SelectColumn column, Map<String, TableDefinition> tableDefs)
  {
    for (TableDefinition def : tableDefs.values())
    {
      String colTable = getTableNameForAlias(connection.getMetadata().removeQuotes(column.getColumnTable()));
      if (colTable == null || StringUtil.equalStringIgnoreCase(colTable, def.getTable().getRawTableName()))
      {
        ColumnIdentifier c = ColumnIdentifier.findColumnInList(def.getColumns(), column.getObjectName());
        if (c != null)
        {
          return def;
        }
      }
    }
    return null;
  }

  public void updateCommentsFromDefinition(DataStore ds, TableDefinition def)
  {
    if (ds == null) return;
    ResultInfo info = ds.getResultInfo();
    if (info == null) return;

    String tableName = def.getTable().getRawTableName();
    for (ColumnIdentifier col : def.getColumns())
    {
      int index = info.findColumn(col.getColumnName(), connection.getMetadata());
      if (index > -1)
      {
        ColumnIdentifier resultColumn = info.getColumn(index);
        resultColumn.setComment(col.getComment());
        if (resultColumn.getSourceTableName() == null)
        {
          resultColumn.setSourceTableName(tableName);
        }
      }
    }
  }
  
}
