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

/**
 *
 * @author Thomas Kellerer
 */
public class H2InsertGenerator
  extends DefaultInsertGenerator
{
  public H2InsertGenerator(TableIdentifier table, List<ColumnIdentifier> targetColumns)
  {
    super(table, targetColumns);
  }

  public H2InsertGenerator(TableIdentifier table, List<ColumnIdentifier> targetColumns, WbConnection conn)
  {
    super(table, targetColumns, conn);
  }

  @Override
  public boolean supportsType(InsertType type)
  {
    return super.supportsType(type) || type == InsertType.Upsert;
  }


  @Override
  protected String getInsertSQLStart()
  {
    if (getInsertType() == InsertType.Upsert)
    {
      return "MERGE INTO ";
    }
    return super.getInsertSQLStart();
  }


}
