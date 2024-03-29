/*
 * SqlServerTableSourceBuilder.java
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
package workbench.db.mssql;


import workbench.db.DependencyNode;
import workbench.db.PkDefinition;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.WbConnection;
import workbench.db.sqltemplates.TemplateHandler;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerTableSourceBuilder
  extends TableSourceBuilder
{
  public static final String CLUSTERED_PLACEHOLDER = "%clustered_attribute%";

  public SqlServerTableSourceBuilder(WbConnection con)
  {
    super(con);
  }

  @Override
  public CharSequence getPkSource(TableIdentifier table, PkDefinition pk, boolean forInlineUse, boolean useFQN)
  {
    CharSequence pkSource = super.getPkSource(table, pk, forInlineUse, useFQN);
    if (StringUtil.isEmptyString(pkSource))
    {
      return pkSource;
    }
    String sql = pkSource.toString();

    String type = pk.getPkIndexDefinition().getIndexType();
    String clustered = "CLUSTERED";
    if ("NORMAL".equals(type))
    {
      clustered = "NONCLUSTERED";
    }

    if (StringUtil.isBlank(clustered))
    {
      sql = TemplateHandler.removePlaceholder(sql, CLUSTERED_PLACEHOLDER, true);
    }
    else
    {
      sql = TemplateHandler.replacePlaceholder(sql, CLUSTERED_PLACEHOLDER, clustered, true);
    }
    return sql;
  }

  @Override
  protected String getAdditionalFkSql(TableIdentifier table, DependencyNode fk, String template)
  {
    if (!fk.isValidated())
    {
      template = template.replace("%nocheck%", "WITH NOCHECK ");
    }
    else
    {
      template = template.replace("%nocheck%", "");
    }

    if (!fk.isEnabled())
    {
      template += "\nALTER TABLE " + table.getObjectExpression(dbConnection) + " NOCHECK CONSTRAINT " + dbConnection.getMetadata().quoteObjectname(fk.getFkName());
    }
    return template;
  }

}
