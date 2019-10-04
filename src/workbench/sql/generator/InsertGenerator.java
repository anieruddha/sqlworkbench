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

import java.util.List;

import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.importer.ConstantColumnValues;

import workbench.storage.RowData;
import workbench.storage.SqlLiteralFormatter;

import workbench.sql.DelimiterDefinition;

/**
 *
 * @author Thomas Kellerer
 */
public interface InsertGenerator
{
  int addRow(RowData row);

  void clearRowData();

  String createLiteralSQL(SqlLiteralFormatter literalFormatter);

  String createLiteralSQL(List<RowData> data, SqlLiteralFormatter literalFormatter);

  String createPreparedSQL(int numRows);

  InsertType getInsertType();

  void setInsertType(InsertType type);

  List<ColumnIdentifier> getKeyColumns();

  void setKeyColumns(List<ColumnIdentifier> keys);

  List<ColumnIdentifier> getPKColumns();

  TableIdentifier getTargetTable();

  List<RowData> getValues();

  void setInsertStartSQL(String insertStart);

  boolean supportsMultiRowInserts();

  boolean supportsType(InsertType type);

  void setDelimiter(DelimiterDefinition delim);

  void setColumnConstants(ConstantColumnValues constants);
}
