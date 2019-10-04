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

import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class FirebirdInsertGenerator
  extends DefaultInsertGenerator
{
  public FirebirdInsertGenerator(TableIdentifier table, List<ColumnIdentifier> targetColumns)
  {
    super(table, targetColumns);
  }

  public FirebirdInsertGenerator(TableIdentifier table, List<ColumnIdentifier> targetColumns, WbConnection conn)
  {
    super(table, targetColumns, conn);
  }

  @Override
  public boolean supportsType(InsertType type)
  {
    return type == InsertType.Insert || type == InsertType.Upsert || type == InsertType.Merge;
  }


  @Override
  protected String getInsertSQLStart()
  {
    if (getInsertType() == InsertType.Upsert)
    {
      return "UPDATE OR INSERT INTO ";
    }
    return super.getInsertSQLStart(); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  protected CharSequence buildFinalPart()
  {
    if (getInsertType() == InsertType.Upsert)
    {
      return getMatchingClause();
    }
    return super.buildFinalPart(); //To change body of generated methods, choose Tools | Templates.
  }

  private CharSequence getMatchingClause()
  {
    List<ColumnIdentifier> keys = getKeyColumns();

    if (CollectionUtil.isEmpty(keys))
    {
      return null;
    }
    StringBuilder sql = new StringBuilder(keys.size() * 20);
    sql.append("\nMATCHING (");
    for (int i = 0; i < keys.size(); i++)
    {
      if (i > 0) sql.append(',');
      String colname = keys.get(i).getDisplayName();
      sql.append(quoteHandler.quoteObjectname(colname));
    }
    sql.append(')');
    return sql;
  }


}
