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
package workbench.db;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PackageDefinition
  implements DbObject, Serializable
{
  private String schema;
  private String packageName;
  private String remarks;

  public PackageDefinition(String schemaName, String pkgName)
  {
    schema = schemaName;
    packageName = pkgName;
  }

  @Override
  public String getCatalog()
  {
    // currently I don't know of any DBMS that supports packages AND catalogs
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
    return "PACKAGE";
  }

  @Override
  public String getObjectName()
  {
    return packageName;
  }

  @Override
  public void setName(String name)
  {
    packageName = name;
  }

  @Override
  public String getObjectName(WbConnection conn)
  {
    return SqlUtil.buildExpression(conn, this);
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
    ProcedureReader reader = ReaderFactory.getProcedureReader(con.getMetadata());
    return reader.getPackageSource(getCatalog(), getSchema(), getObjectName());
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
  public void setComment(String comment)
  {
    remarks = comment;
  }

  @Override
  public String getDropStatement(WbConnection con, boolean cascade)
  {
    return "DROP PACKAGE " + getFullyQualifiedName(con);
  }

  public List<ProcedureDefinition> getProcedures()
  {
    return Collections.emptyList();
  }

  @Override
  public boolean supportsGetSource()
  {
    return true;
  }

}
