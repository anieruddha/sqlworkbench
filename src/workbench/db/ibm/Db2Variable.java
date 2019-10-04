/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2017 Thomas Kellerer.
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
package workbench.db.ibm;

import java.io.Serializable;
import java.sql.SQLException;

import workbench.db.DbObject;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2Variable
  implements DbObject, Serializable
{
  private String catalog;
  private String schema;
  private String variable;

  private String dataType;
  private String defaultValue;
  private int ccsid = -1;
  private String remarks;

  public Db2Variable(String schema, String variable, String dataType)
  {
    this.schema = schema;
    this.variable = variable;
    this.dataType = dataType;
  }

  @Override
  public String getCatalog()
  {
    return catalog;
  }

  public void setCatalog(String catalog)
  {
    this.catalog = catalog;
  }

  public void setSchema(String schema)
  {
    this.schema = schema;
  }

  @Override
  public String getSchema()
  {
    return schema;
  }

  public String getRemarks()
  {
    return remarks;
  }

  public void setRemarks(String remarks)
  {
    this.remarks = remarks;
  }

  public void setVariableName(String name)
  {
    this.variable = name;
  }

  public void setDataType(String dataType)
  {
    this.dataType = dataType;
  }

  public String getDataType()
  {
    return dataType;
  }

  public String getDefaultValue()
  {
    return defaultValue;
  }

  public int getCcsid()
  {
    return ccsid;
  }

  public void setDefaultValue(String defaultValue)
  {
    this.defaultValue = defaultValue;
  }

  public void setCcsid(int id)
  {
    this.ccsid = id;
  }

  @Override
  public String getObjectType()
  {
    return Db2iVariableReader.TYPE;
  }

  @Override
  public String getObjectName()
  {
    return variable;
  }

  @Override
  public String getObjectName(WbConnection conn)
  {
    return variable;
  }

  @Override
  public String getObjectExpression(WbConnection conn)
  {
    return SqlUtil.buildExpression(conn, this);
  }

  @Override
  public String getFullyQualifiedName(WbConnection conn)
  {
    return SqlUtil.fullyQualifiedName(conn, this);
  }

  @Override
  public CharSequence getSource(WbConnection con)
    throws SQLException
  {
    String sql = "CREATE OR REPLACE VARIABLE " + getObjectExpression(con) + " " + dataType;
    if (ccsid > -1)
    {
      sql += " CCSID " + ccsid;
    }
    if (StringUtil.isNonBlank(defaultValue))
    {
      sql += "\n  DEFAULT " + defaultValue;
    }
    sql += ";";
    return sql;
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
  public void setComment(String remarks)
  {
    this.remarks = remarks;
  }

  @Override
  public String getDropStatement(WbConnection con, boolean cascade)
  {
    return "DROP VARIABLE " + getFullyQualifiedName(con);
  }

  @Override
  public boolean supportsGetSource()
  {
    return true;
  }

}
