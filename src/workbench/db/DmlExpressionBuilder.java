/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2019, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
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

package workbench.db;

import workbench.db.postgres.PostgresExpressionBuilder;

/**
 *
 * @author Thomas Kellerer
 */
public interface DmlExpressionBuilder
{
  String getDmlExpression(ColumnIdentifier column, DmlExpressionType forType);
  boolean isDmlExpressionDefined(String baseType, DmlExpressionType forType);

  public static class Factory
  {
    public static DmlExpressionBuilder getBuilder(WbConnection conn)
    {
      if (conn != null && conn.getMetadata().isPostgres())
      {
        return new PostgresExpressionBuilder(conn);
      }
      return new DefaultExpressionBuilder(conn);
    }
  }

}

