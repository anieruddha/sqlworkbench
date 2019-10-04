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
import workbench.db.WbConnection;

import workbench.util.VersionNumber;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresInsertGenerator
  extends DefaultInsertGenerator
{

  public PostgresInsertGenerator(TableIdentifier table, List<ColumnIdentifier> targetColumns)
  {
    super(table, targetColumns);
  }

  public PostgresInsertGenerator(TableIdentifier table, List<ColumnIdentifier> targetColumns, WbConnection conn)
  {
    super(table, targetColumns, conn);
  }

  protected boolean is95()
  {
    return dbVersion.isNewerThan(new VersionNumber(9,5));
  }

  @Override
  public boolean supportsType(InsertType type)
  {
    if (type == InsertType.Upsert)
    {
      return is95() && getPKColumns().size() > 0;
    }
    return super.supportsType(type) || type == InsertType.InsertIgnore;
  }

  @Override
  protected CharSequence buildFinalPart()
  {
    switch (getInsertType())
    {
      case InsertIgnore:
        return buildIgnore();
      case Upsert:
      case Merge:
        return buildDoUpdate();
    }
    return "";
  }

  private CharSequence buildIgnore()
  {
    return "\nON CONFLICT DO NOTHING";
  }

  private CharSequence buildDoUpdate()
  {
    String onConflict = buildOnConflict();
    onConflict +=
      "\nDO UPDATE \n" +
      " SET ";

    List<ColumnIdentifier> keys = getKeyColumns();
    int colCount = 0;
    for (int i=0; i < targetColumns.size(); i++)
    {
      ColumnIdentifier col = targetColumns.get(i);
      if (keys.contains(col) ) continue;
      if (colCount > 0) onConflict += ",\n      ";
      String colname = quoteHandler.quoteObjectname(col.getDisplayName());
      onConflict += colname + " = EXCLUDED." + colname;
      colCount ++;
    }
    return onConflict;
  }

  private String buildOnConflict()
  {
    String onConflict = "\nON CONFLICT (";

    List<ColumnIdentifier> keys = getKeyColumns();
    for (int i=0; i < keys.size(); i++)
    {
      if (i > 0) onConflict += ", ";
      String colname = keys.get(i).getDisplayName();
      colname = quoteHandler.quoteObjectname(colname);
      onConflict += colname;
    }
    onConflict += ")";

    return onConflict;
  }

}
