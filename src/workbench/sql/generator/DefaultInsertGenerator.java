/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2018 Thomas Kellerer.
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
package workbench.sql.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.db.ColumnIdentifier;
import workbench.db.QuoteHandler;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.importer.ConstantColumnValues;

import workbench.storage.ColumnData;
import workbench.storage.RowData;
import workbench.storage.SqlLiteralFormatter;

import workbench.sql.DelimiterDefinition;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;
import workbench.util.VersionNumber;

/**
 *
 * @author Thomas Kellerer
 */
public class DefaultInsertGenerator
  implements InsertGenerator
{
  private final TableIdentifier table;
  protected final List<ColumnIdentifier> targetColumns;
  private List<ColumnIdentifier> keyColumns;
  private InsertType type;
  private String insertSqlStart;
  private ConstantColumnValues columnConstants;
  protected final QuoteHandler quoteHandler;
  protected VersionNumber dbVersion;
  private final List<RowData> rows = new ArrayList<>();
  private String delimiter = ";";
  private WbConnection dbConnection;

  public DefaultInsertGenerator(TableIdentifier table, List<ColumnIdentifier> targetColumns)
  {
    this(table, targetColumns, null);
  }

  public DefaultInsertGenerator(TableIdentifier table, List<ColumnIdentifier> targetColumns, WbConnection conn)
  {
    this.table = table;
    this.targetColumns = new ArrayList<>(targetColumns);
    this.type = InsertType.Insert;
    this.dbConnection = conn;
    if (conn == null)
    {
      quoteHandler = QuoteHandler.STANDARD_HANDLER;
      dbVersion = new VersionNumber(0, 0);
    }
    else
    {
      quoteHandler = conn.getMetadata();
      dbVersion = conn.getDatabaseVersion();
    }
  }

  @Override
  public void setColumnConstants(ConstantColumnValues constants)
  {
    this.columnConstants = constants;
  }

  @Override
  public void setDelimiter(DelimiterDefinition delim)
  {
    if (delim.isSingleLine())
    {
      this.delimiter = delim.getDelimiter();
    }
    else
    {
      this.delimiter = delim.getDelimiter() + '\n';
    }
  }

  @Override
  public List<ColumnIdentifier> getKeyColumns()
  {
    if (CollectionUtil.isEmpty(keyColumns))
    {
      return getPKColumns();
    }
    return keyColumns;
  }

  /**
   * Define alternate key columns to be used for updating rows.
   *
   * If no alternate key columns are defined the primary key columns are used.
   *
   * @param keys   the columns to be used as a unique key
   */
  @Override
  public void setKeyColumns(List<ColumnIdentifier> keys)
  {
    this.keyColumns = new ArrayList<>(1);
    if (keys == null)
    {
      return;
    }
    for (ColumnIdentifier col : keys)
    {
      keyColumns.add(col.createCopy());
    }
  }

  @Override
  public void setInsertStartSQL(String insertStart)
  {
    this.insertSqlStart = insertStart;
  }

  @Override
  public String createLiteralSQL(SqlLiteralFormatter literalFormatter)
  {
    return createLiteralSQL(rows, literalFormatter);
  }

  @Override
  public String createLiteralSQL(List<RowData> data, SqlLiteralFormatter literalFormatter)
  {
    switch (type)
    {
      case Insert:
      case InsertIgnore:
      case Upsert:
        return createInsertWithLiterals(data, literalFormatter);
    }
    return null;
  }

  protected String createInsertWithLiterals(List<RowData> data, SqlLiteralFormatter literalFormatter)
  {
    StringBuilder result = new StringBuilder(targetColumns.size() * 50 + targetColumns.size() * data.size() * 15 + 50);
    if (supportsMultiRowInserts())
    {
      result.append(buildInsertPart());
      result.append(buildValuesList(data, literalFormatter));
      result.append(buildFinalPart());
    }
    else
    {
      boolean useDelimiter = data.size() > 1;
      for (RowData row : data)
      {
        result.append(buildInsertPart());
        result.append(buildValuesList(Collections.singletonList(row), literalFormatter));
        result.append(buildFinalPart());
        if (useDelimiter) result.append(delimiter);
      }
    }
    return result.toString();
  }


  @Override
  public String createPreparedSQL(int numRows)
  {
    switch (type)
    {
      case Insert:
      case InsertIgnore:
      case Upsert:
        return createInsertPrepared(numRows);
    }
    return null;
  }

  protected String createInsertPrepared(int numRows)
  {
    StringBuilder result = new StringBuilder(targetColumns.size() * 50 + targetColumns.size() * numRows * 5 + 50);
    if (supportsMultiRowInserts())
    {
      result.append(buildInsertPart());
      result.append(buildParameterList(numRows));
      result.append(buildFinalPart());
    }
    else
    {
      for (int row=0; row < numRows; row++)
      {
        result.append(buildInsertPart());
        result.append(buildParameterList(1));
        result.append(buildFinalPart());
        if (numRows > 1) result.append(";\n");
      }
    }
    return result.toString();
  }

  protected String getInsertSQLStart()
  {
    return insertSqlStart;
  }

  protected CharSequence buildInsertPart()
  {
    StringBuilder text = new StringBuilder(targetColumns.size() * 50);
    String sql = getInsertSQLStart();
    if (StringUtil.isNonBlank(sql))
    {
      text.append(sql);
      text.append(' ');
    }
    else
    {
      text.append("INSERT INTO ");
    }
    text.append(getTargetTableName());
    text.append("\n  (");

    for (int i=0; i < targetColumns.size(); i++)
    {
      ColumnIdentifier col = this.targetColumns.get(i);
      if (i > 0)
      {
        text.append(',');
      }
      String colname = col.getDisplayName();
      colname = quoteHandler.quoteObjectname(colname);
      text.append(colname);
    }

    if (columnConstants != null)
    {
      int cols = columnConstants.getColumnCount();
      for (int i = 0; i < cols; i++)
      {
        text.append(',');
        text.append(columnConstants.getColumn(i).getColumnName());
      }
    }
    text.append(")");
    return text;
  }

  protected CharSequence buildParameterList(int numRows)
  {
    StringBuilder list = new StringBuilder(this.targetColumns.size() * 5 + numRows);
    list.append("\nVALUES\n  ");
    for (int i=0; i < numRows; i++)
    {
      if (i>0)
      {
        list.append(",\n  ");
      }

      list.append('(');
      for (int c = 0; c < targetColumns.size(); c++)
      {
        if (c > 0)
        {
          list.append(',');
        }
        list.append('?');
      }
      list.append(')');
    }
    return list;
  }

  protected CharSequence createValuesStart()
  {
    return "\nVALUES\n  ";
  }
  
  protected CharSequence buildValuesList(List<RowData> data, SqlLiteralFormatter formatter)
  {
    StringBuilder list = new StringBuilder(this.targetColumns.size() * 5 + data.size());
    list.append(createValuesStart());
    for (int i=0; i < data.size(); i++)
    {
      if (i>0)
      {
        list.append(",\n  ");
      }

      RowData row = data.get(i);
      list.append('(');
      for (int c = 0; c < targetColumns.size(); c++)
      {
        if (c > 0)
        {
          list.append(',');
        }
        ColumnData colData = new ColumnData(row.getValue(c), targetColumns.get(c));
        CharSequence literal = formatter.getDefaultLiteral(colData);
        list.append(literal);
      }
      list.append(')');
    }
    return list;
  }

  protected String buildRowValues()
  {
    StringBuilder values = new StringBuilder(targetColumns.size() * 50);
    return values.toString();
  }

  protected CharSequence buildFinalPart()
  {
    return "";
  }

  protected String getColumnConstant(ColumnIdentifier col)
  {
    if (this.columnConstants == null) return null;
    return null;
  }

  public String getTargetTableName()
  {
    return table.getFullyQualifiedName(dbConnection);
  }

  @Override
  public TableIdentifier getTargetTable()
  {
    return table;
  }

  @Override
  public boolean supportsType(InsertType type)
  {
    return type == InsertType.Insert;
  }

  @Override
  public InsertType getInsertType()
  {
    return type;
  }

  @Override
  public void setInsertType(InsertType type)
  {
    if (!this.supportsType(type)) throw new IllegalArgumentException("Insert type " + type + " not supported!");
    if (this.type != type)
    {
      this.type = type;
      buildInsertPart();
    }
  }

  @Override
  public boolean supportsMultiRowInserts()
  {
    return true;
  }

  @Override
  public int addRow(RowData row)
  {
    rows.add(row);
    return rows.size();
}

  @Override
  public List<RowData> getValues()
  {
    return Collections.unmodifiableList(rows);
  }

  @Override
  public void clearRowData()
  {
    this.rows.clear();
  }

  @Override
  public List<ColumnIdentifier> getPKColumns()
  {
    if (this.targetColumns == null) return Collections.emptyList();
    List<ColumnIdentifier> pkCols = new ArrayList<>(1);
    for (ColumnIdentifier col : targetColumns)
    {
      if (col.isPkColumn())
      {
        pkCols.add(col);
      }
    }
    return pkCols;
  }
}
