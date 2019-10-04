/*
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
package workbench.db.postgres;

import java.sql.SQLException;

import workbench.db.DbObject;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;


/**
 *
 * @author Thomas Kellerer
 */
public class PgCollation
  implements DbObject
{
  public static final String TYPE_NAME = "COLLATION";
  private String name;
  private String remarks;
  private String schema;
  private String provider;
  private String collate;
  private String cType;

  public PgCollation(String schema, String name)
  {
    this.name = name;
    this.schema = schema;
  }

  public void setLocale(String collate, String cType)
  {
    this.collate = collate;
    this.cType = cType;
  }

  public String getCollate()
  {
    return collate;
  }

  public String getCType()
  {
    return cType;
  }

  public String getProvider()
  {
    return provider;
  }

  public void setProvider(String provider)
  {
    this.provider = provider;
  }

  @Override
  public String getCatalog()
  {
    return null;
  }

  @Override
  public String getSchema()
  {
    return schema;
  }

  @Override
  public String getObjectType()
  {
    return TYPE_NAME;
  }

  @Override
  public String getObjectName()
  {
    return name;
  }

  @Override
  public String getObjectName(WbConnection conn)
  {
    return name;
  }

  @Override
  public String getFullyQualifiedName(WbConnection conn)
  {
    return SqlUtil.fullyQualifiedName(conn, this);
  }

  @Override
  public String getObjectExpression(WbConnection conn)
  {
    return SqlUtil.buildExpression(conn, this);
  }

  @Override
  public String toString()
  {
    return getObjectName();
  }

  public void setSource(String sql)
  {
  }

  public String getSource()
  {
    return buildSource(SqlUtil.buildExpression(null, null, schema, name));
  }

  private String buildSource(String nameExpression)
  {
    String sql = "CREATE COLLATION IF NOT EXISTS " + nameExpression + " (";
    if (StringUtil.isNonBlank(provider))
    {
      sql += "provider = " + provider + ", ";
    }
    sql += "lc_collate='" + collate + "', lc_ctype='" + cType + "');";
    return sql;
  }

  @Override
  public CharSequence getSource(WbConnection con)
    throws SQLException
  {
    return buildSource(SqlUtil.buildExpression(con, this));
  }

  @Override
  public String getDropStatement(WbConnection con, boolean cascade)
  {
    StringBuilder sql = new StringBuilder(50);
    sql.append("DROP COLLATION IF EXISTS ");
    sql.append(getObjectNameForDrop(con));
    if (cascade)
    {
      sql.append(" CASCADE");
    }
    sql.append(';');
    return sql.toString();
  }

  @Override
  public String getObjectNameForDrop(WbConnection con)
  {
    return getFullyQualifiedName(con);
  }

  @Override
  public String getComment()
  {
    return remarks;
  }

  @Override
  public void setComment(String cmt)
  {
    remarks = cmt;
  }

  @Override
  public boolean supportsGetSource()
  {
    return true;
  }

}
