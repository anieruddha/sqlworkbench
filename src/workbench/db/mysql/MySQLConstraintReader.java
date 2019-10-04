/*
 * H2ConstraintReader.java
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
package workbench.db.mysql;

import workbench.db.AbstractConstraintReader;
import workbench.db.DBID;

/**
 * @author  Thomas Kellerer
 */
public class MySQLConstraintReader
  extends AbstractConstraintReader
{

  private final String TABLE_SQL =
    "select tc.constraint_name, cc.check_clause \n" +
    "from information_schema.TABLE_CONSTRAINTS tc\n" +
    "  join information_schema.CHECK_CONSTRAINTS cc on cc.constraint_name = tc.constraint_Name \n" +
    "where tc.table_name = ? \n" +
    " and tc.table_schema = ?";

  public MySQLConstraintReader()
  {
    super(DBID.MySQL.getId());
  }

  @Override
  public int getIndexForCatalogParameter()
  {
    return 2;
  }

  @Override
  public int getIndexForSchemaParameter()
  {
    return -1;
  }

  @Override
  public int getIndexForTableNameParameter()
  {
    return 1;
  }

  @Override
  public String getTableConstraintSql()
  {
    return this.TABLE_SQL;
  }

  @Override
  public boolean isSystemConstraintName(String name)
  {
    return "PRIMARY".equalsIgnoreCase(name);
  }

  @Override
  public String getColumnConstraintSql()
  {
    return null;
  }

}
