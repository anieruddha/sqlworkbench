/*
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

package workbench.db.postgres;

import java.sql.Types;

import workbench.db.ColumnIdentifier;
import workbench.db.DefaultExpressionBuilder;
import workbench.db.DmlExpressionType;
import workbench.db.WbConnection;


public class PostgresExpressionBuilder
  extends DefaultExpressionBuilder
{

  public PostgresExpressionBuilder(WbConnection conn)
  {
    super(conn);
  }

  @Override
  public String getDmlExpression(ColumnIdentifier column, DmlExpressionType typeFor)
  {
    String expression = settings.getDmlExpressionValue(column.getDbmsType(), typeFor);
    if (expression != null)
    {
      return expression;
    }
    if (column.getDataType() == Types.STRUCT)
    {
      return "cast(? as " + column.getDbmsType() + ")";
    }
    return "?";
  }

}
