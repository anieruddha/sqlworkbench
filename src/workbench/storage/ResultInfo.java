/*
 * ResultInfo.java
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

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;

import workbench.db.ColumnIdentifier;
import workbench.db.DataTypeResolver;
import workbench.db.DbMetadata;
import workbench.db.IndexReader;
import workbench.db.PkDefinition;
import workbench.db.QuoteHandler;
import workbench.db.ReaderFactory;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to cache the meta information of a ResultSet.
 *
 * @author  Thomas Kellerer
 */
public class ResultInfo
{
  // Cached ResultSetMetaData information
  private List<ColumnIdentifier> columns;
  private int realColumns;
  private TableIdentifier updateTable;
  private boolean isUserDefinedPK;
  private boolean useGetStringForBit;
  private boolean columnTablesDetected;

  public ResultInfo(ColumnIdentifier[] cols)
  {
    this.columns = new ArrayList<>(cols.length);
    for (ColumnIdentifier col : cols)
    {
      this.columns.add(col.createCopy());
    }
  }

  public ResultInfo(List<ColumnIdentifier> cols)
  {
    this.columns = new ArrayList<>(cols.size());
    for (ColumnIdentifier col : cols)
    {
      this.columns.add(col.createCopy());
    }
  }

  public ResultInfo(String[] colNames, int[] colTypes, int[] colSizes)
  {
    this.columns = new ArrayList<>(colNames.length);
    for (int i=0; i < colNames.length; i++)
    {
      ColumnIdentifier col = new ColumnIdentifier(colNames[i]);
      if (colSizes != null) col.setColumnSize(colSizes[i]);
      col.setDataType(colTypes[i]);
      this.columns.add(col);
    }
  }

  public ResultInfo(TableIdentifier table, WbConnection conn)
    throws SQLException
  {
    DbMetadata meta = conn.getMetadata();

    TableIdentifier toUse = meta.findObject(table);

    if (toUse == null)
    {
      toUse = table.createCopy();
      if (toUse.getType() == null)
      {
        String type = meta.getObjectType(toUse);
        toUse.setType(type);
      }

      if (toUse.getSchema() == null)
      {
        toUse.setSchema(meta.getCurrentSchema());
      }

      if (toUse.getCatalog() == null)
      {
        toUse.setCatalog(meta.getCurrentCatalog());
      }
    }

    this.updateTable = toUse;
    List<ColumnIdentifier> cols = meta.getTableColumns(toUse);
    this.columns = new ArrayList<>(cols);
    initDbConfig(conn);
  }

  private void initDbConfig(WbConnection sourceConnection)
  {
    if (sourceConnection != null)
    {
      useGetStringForBit = sourceConnection.getDbSettings().useGetStringForBit();
    }
  }

  public ResultInfo(ResultSetMetaData metaData, WbConnection sourceConnection)
    throws SQLException
  {
    int colCount = metaData.getColumnCount();
    this.columns = new ArrayList<>(colCount);

    final CallerInfo ci = new CallerInfo(){};

    initDbConfig(sourceConnection);

    DbMetadata dbMeta = null;
    DataTypeResolver resolver = null;
    if (sourceConnection != null)
    {
      dbMeta = sourceConnection.getMetadata();
      resolver = dbMeta.getDataTypeResolver();
    }

    boolean checkReadOnly = (sourceConnection == null ? false : sourceConnection.getDbSettings().getCheckResultSetReadOnlyCols());
    boolean reportsSizeAsDisplaySize = (sourceConnection == null ? false : sourceConnection.getDbSettings().reportsRealSizeAsDisplaySize()) ;
    boolean supportsGetTable = (sourceConnection == null ? false : sourceConnection.getDbSettings().supportsResultMetaGetTable());
    boolean showTableName = GuiSettings.showTableNameInColumnHeader();

    for (int i=0; i < colCount; i++)
    {
      String name = null;
      String alias = null;
      try
      {
        // Not all drivers will really return the column label if an alias is used
        name = metaData.getColumnName(i + 1);
        alias = metaData.getColumnLabel(i + 1);
      }
      catch (Throwable th)
      {
        LogMgr.logWarning(ci, "Could not obtain column name or alias", th);
      }

      if (StringUtil.isNonBlank(name))
      {
        this.realColumns ++;
      }
      else
      {
        name = "Col" + (i+1);
      }

      ColumnIdentifier col = new ColumnIdentifier(name);
      col.setPosition(i+1);

      if (StringUtil.stringsAreNotEqual(name, alias))
      {
        // only set the alias if it's different than the name
        col.setColumnAlias(alias);
      }

      try
      {
        int nullInfo = metaData.isNullable(i + 1);
        col.setIsNullable(nullInfo != ResultSetMetaData.columnNoNulls);
      }
      catch (Throwable th)
      {
        LogMgr.logWarning(ci, "Error when checking nullable for column : " + name, th);
      }

      if (checkReadOnly)
      {
        try
        {
          boolean isReadonly = metaData.isReadOnly(i + 1);
          col.setReadonly(isReadonly);
        }
        catch (Throwable th)
        {
          LogMgr.logWarning(ci, "Error when checking readonly attribute for column : " + name, th);
          checkReadOnly = false;
        }
      }

      if (supportsGetTable || showTableName)
      {
        try
        {
          String tname = metaData.getTableName(i + 1);
          col.setSourceTableName(tname);
        }
        catch (Throwable th)
        {
          if (sourceConnection != null && supportsGetTable)
          {
            LogMgr.logWarning(ci, "Disabling usage of ResultSetMetaData.getTableName() for DBID: " + sourceConnection.getDbId(), th);
            sourceConnection.getDbSettings().setSupportsResultMetaGetTable(false);
          }
          supportsGetTable = false;
          showTableName = false;
        }
      }

      String typename = null;
      try
      {
        typename = metaData.getColumnTypeName(i + 1);
      }
      catch (Exception e)
      {
        typename = null;
      }

      int type = metaData.getColumnType(i + 1);
      if (StringUtil.isEmptyString(typename))
      {
        // use the Java name if the driver did not return a type name for this column
        typename = SqlUtil.getTypeName(type);
      }

      if (resolver != null)
      {
        type = resolver.fixColumnType(type, typename);
      }

      col.setDataType(type);
      col.setColumnTypeName(typename);

      int scale = 0;
      int prec = 0;

      // Some JDBC drivers (e.g. Oracle, MySQL) do not like getScale() on all column types,
      // so we only call it for number data types (the only ones were it seems to make sense)
      try
      {
        if (SqlUtil.isNumberType(type)) scale = metaData.getScale(i + 1);
      }
      catch (Throwable th)
      {
        scale = -1;
      }

      try
      {
        prec = metaData.getPrecision(i + 1);
      }
      catch (Throwable th)
      {
        prec = 0;
      }

      int displaySize = 0;
      try
      {
        displaySize = metaData.getColumnDisplaySize(i + 1);
      }
      catch (Exception e)
      {
        displaySize = prec;
      }

      col.setDisplaySize(displaySize);
      col.setDecimalDigits(scale);
      String dbmsType = null;

      int sizeToUse = prec;

      if (type == Types.VARCHAR && reportsSizeAsDisplaySize)
      {
        sizeToUse = displaySize;
      }

      if (resolver != null)
      {
        dbmsType = resolver.getSqlTypeDisplay(typename, col.getDataType(), sizeToUse, scale);
      }
      else
      {
        dbmsType = SqlUtil.getSqlTypeDisplay(typename, col.getDataType(), sizeToUse, scale);
      }

      if (type == Types.VARCHAR)
      {
        col.setColumnSize(sizeToUse);
      }
      else
      {
        col.setColumnSize(displaySize);
      }

      col.setDbmsType(dbmsType);

      try
      {
        if (useGetStringForBit && type == Types.BIT)
        {
          col.setColumnClassName("java.lang.String");
        }
        else
        {
          String cls = null;
          if (resolver != null)
          {
            cls = resolver.getColumnClassName(col.getDataType(), col.getDbmsType());
          }
          if (cls == null) cls = metaData.getColumnClassName(i + 1);
          col.setColumnClassName(cls);
        }
      }
      catch (Throwable e)
      {
        col.setColumnClassName("java.lang.Object");
      }

      try
      {
        boolean autoIncrement = metaData.isAutoIncrement(i + 1);
        col.setIsAutoincrement(autoIncrement);
      }
      catch (Throwable th)
      {
        LogMgr.logDebug(ci, "Error when checking autoincrement attribute for column : " + name, th);
      }
      this.columns.add(col);
    }
  }

  public boolean columnTablesAvailable()
  {
    for (ColumnIdentifier col : columns)
    {
      if (StringUtil.isBlank(col.getSourceTableName()))
      {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if the information returned from ColumnIdentifier.getSourceTableName() can be trusted
   *
   * @return true if the source table for each column is properly initialized
   */
  public boolean isColumnTableDetected()
  {
    return columnTablesDetected;
  }

  public void setColumnTableDetected(boolean flag)
  {
    columnTablesDetected = flag;
  }

  public ColumnIdentifier getColumn(int i)
  {
    return this.columns.get(i);
  }

  public ColumnIdentifier[] getColumns()
  {
    return this.columns.toArray(new ColumnIdentifier[0]);
  }

  public List<ColumnIdentifier> getColumnList()
  {
    if (this.columns == null) return Collections.emptyList();

    return Collections.unmodifiableList(columns);
  }

  public boolean isNullable(int col)
  {
    return this.columns.get(col).isNullable();
  }

  public void resetPkColumns()
  {
    isUserDefinedPK = false;
    for (ColumnIdentifier col : columns)
    {
      col.setIsPkColumn(false);
    }
  }

  public void setIsPkColumn(String column, boolean flag)
  {
    int index = this.findColumn(column);
    if (index > -1) this.setIsPkColumn(index, flag);
  }

  public void setIsPkColumn(int col, boolean flag)
  {
    this.columns.get(col).setIsPkColumn(flag);
  }

  public boolean hasUpdateableColumns()
  {
    return this.realColumns > 0;
  }

  public boolean isUpdateable(int col)
  {
    return this.columns.get(col).isUpdateable();
  }

  public void setIsNullable(int col, boolean flag)
  {
    this.columns.get(col).setIsNullable(flag);
  }

  public void setUpdateable(int col, boolean flag)
  {
    this.columns.get(col).setUpdateable(flag);
  }

  public boolean isPkColumn(int col)
  {
    return this.columns.get(col).isPkColumn();
  }

  public void setPKColumns(ColumnIdentifier[] cols)
  {
    for (ColumnIdentifier col1 : cols)
    {
      String name = col1.getColumnName();
      int col = this.findColumn(name);
      if (col > -1)
      {
        boolean pk = col1.isPkColumn();
        this.columns.get(col).setIsPkColumn(pk);
      }
    }
  }

  /**
   * Redefines the PK columns for this ResultInfo.
   *
   * All existing PK flags will be cleared and for all columns passed the PK flag is set.
   */
  public void setPKColumns(List<ColumnIdentifier> cols)
  {
    // reset existing PK definition
    for (ColumnIdentifier col : columns)
    {
      col.setIsPkColumn(false);
    }

    for (ColumnIdentifier col : cols)
    {
      String name = col.getColumnName();
      int colIndex = this.findColumn(name);
      if (colIndex > -1)
      {
        this.columns.get(colIndex).setIsPkColumn(true);
      }
    }
  }

  public boolean hasPkColumns()
  {
    for (ColumnIdentifier col : columns)
    {
      if (col.isPkColumn())
      {
        return true;
      }
    }
    return false;
  }


  public void setUpdateTable(TableIdentifier table)
  {
    this.updateTable = table;
  }

  public TableIdentifier getUpdateTable()
  {
    return this.updateTable;
  }

  public int getColumnSize(int col)
  {
    return this.columns.get(col).getColumnSize();
  }

  public void setColumnSizes(int[] sizes)
  {
    if (sizes == null) return;
    if (sizes.length != this.columns.size()) return;
    for (int i=0; i < this.columns.size(); i++)
    {
      columns.get(i).setColumnSize(sizes[i]);
    }
  }

  public int getColumnType(int i)
  {
    return this.columns.get(i).getDataType();
  }

  public void setColumnClassName(int i, String name)
  {
    this.columns.get(i).setColumnClassName(name);
  }

  public String getColumnClassName(int i)
  {
    String className = this.columns.get(i).getColumnClassName();
    if (className != null) return className;
    return this.getColumnClass(i).getName();
  }

  public String getColumnDisplayName(int i)
  {
    return this.columns.get(i).getDisplayName();
  }

  public String getColumnName(int i)
  {
    return this.columns.get(i).getColumnName();
  }

  public String getDbmsTypeName(int i)
  {
    return this.columns.get(i).getDbmsType();
  }

  public int getColumnCount()
  {
    return this.columns.size();
  }

  public Class getColumnClass(int aColumn)
  {
    if (aColumn > getColumnCount()) return null;
    return this.columns.get(aColumn).getColumnClass();
  }

  public void readPkDefinition(WbConnection aConnection)
    throws SQLException
  {
    if (aConnection == null) return;
    if (this.updateTable == null) return;

    resetPkColumns();

    PkDefinition pk = updateTable.getPrimaryKey();
    if (pk == null)
    {
      IndexReader reader = ReaderFactory.getIndexReader(aConnection.getMetadata());
      pk = reader.getPrimaryKey(this.updateTable);
    }

    if (pk != null)
    {
      for (String colName : pk.getColumns())
      {
        int index = findColumn(colName);
        if (index > -1)
        {
          this.columns.get(index).setIsPkColumn(true);
        }
      }
    }
    else
    {
      readPkColumnsFromMapping();
    }
  }

  public int findColumn(String name)
  {
    return findColumn(name, QuoteHandler.STANDARD_HANDLER);
  }

  public int findColumn(String name, QuoteHandler handler)
  {
    if (name == null) return -1;

    String plain = handler.removeQuotes(name);

    for (int i = 0; i < getColumnCount(); i++)
    {
      String colName = handler.removeQuotes(getColumnName(i));
      if (plain.equalsIgnoreCase(colName))
      {
        return i;
      }
    }
    return -1;
  }

  public boolean isUserDefinedPK()
  {
    return isUserDefinedPK;
  }

  public boolean readPkColumnsFromMapping()
  {
    if (this.updateTable == null) return false;
    List<String> cols = PkMapping.getInstance().getPKColumns(this.updateTable.createCopy());
    if (cols == null) return false;
    isUserDefinedPK = false;

    for (String col : cols)
    {
      int index = this.findColumn(col);
      if (index > -1)
      {
        this.setIsPkColumn(index, true);
        isUserDefinedPK = true;
      }
    }
    if (isUserDefinedPK)
    {
      LogMgr.logInfo(new CallerInfo(){}, "Using pk definition for " + updateTable.getTableName() + " from mapping file: " + StringUtil.listToString(cols, ',', false));
    }
    return isUserDefinedPK;
  }

  public ResultInfo createCopy()
  {
    ResultInfo copy = new ResultInfo(this.columns);
    copy.realColumns = this.realColumns;
    if (this.updateTable != null)
    {
      copy.updateTable = this.updateTable.createCopy();
    }
    copy.isUserDefinedPK = this.isUserDefinedPK;
    return copy;
  }

  public void addColumn(ColumnIdentifier newColumn)
  {
    addColumnAt(newColumn, columns.size());
  }

  public void addColumnAt(ColumnIdentifier newColumn, int columnPosition)
  {
    columns.add(columnPosition, newColumn);
  }

}
