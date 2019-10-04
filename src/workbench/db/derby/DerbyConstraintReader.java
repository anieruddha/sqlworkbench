/*
 * DerbyConstraintReader.java
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
package workbench.db.derby;

import workbench.db.AbstractConstraintReader;
import workbench.db.DBID;

/**
 * Constraint reader for the Derby database
 * @author  Thomas Kellerer
 */
public class DerbyConstraintReader
  extends AbstractConstraintReader
{
  private final String TABLE_SQL =
    "select cons.constraintname, c.checkdefinition \n" +
    "from sys.sysconstraints cons \n" +
    "  join sys.syschecks c on cons.constraintid = c.constraintid \n" +
    "  join sys.systables t on t.tableid = cons.tableid \n" +
    "  join sys.sysschemas s on t.schemaid = s.schemaid \n" +
    "where t.tablename = ? \n" +
    "  and s.schemaname = ?";

  public DerbyConstraintReader()
  {
    super(DBID.Derby.getId());
  }


  @Override
  public String getColumnConstraintSql()
  {
    return null;
  }

  @Override
  public String getTableConstraintSql()
  {
    return TABLE_SQL;
  }

  @Override
  public int getIndexForTableNameParameter()
  {
    return 1;
  }

  @Override
  public int getIndexForSchemaParameter()
  {
    return 2;
  }
}
