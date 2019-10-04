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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.ObjectListExtender;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2iVariableReader
  implements ObjectListExtender
{
  public static final String TYPE = "VARIABLE";

  @Override
  public List<String> supportedTypes()
  {
    return CollectionUtil.arrayList(TYPE);
  }

  @Override
  public boolean isDerivedType()
  {
    return false;
  }

  @Override
  public boolean handlesType(String type)
  {
    return TYPE.equalsIgnoreCase(type);
  }

  @Override
  public boolean handlesType(String[] types)
  {
    if (types == null) return true;

    for (String type : types)
    {
      if (handlesType(type)) return true;
    }
    return false;
  }

  @Override
  public DataStore getObjectDetails(WbConnection con, DbObject object)
  {
    return null;
  }

  @Override
  public DbObject getObjectDefinition(WbConnection con, DbObject name)
  {
    List<Db2Variable> variables = getVariables(con, name.getSchema(), name.getObjectName());
    if (CollectionUtil.isNonEmpty(variables))
    {
      return variables.get(0);
    }
    return null;
  }

  @Override
  public String getObjectSource(WbConnection con, DbObject object)
  {
    if (object instanceof Db2Variable)
    {
      return getSource(con, (Db2Variable)object);
    }
    DbObject variable = getObjectDefinition(con, object);
    if (variable != null)
    {
      return getSource(con, (Db2Variable)variable);
    }
    return null;
  }

  private String getSource(WbConnection con, Db2Variable var)
  {
    try
    {
      return var.getSource(con).toString();
    }
    catch (SQLException ex)
    {
      // cannot happen
    }
    return null;
  }

  @Override
  public List<ColumnIdentifier> getColumns(WbConnection con, DbObject object)
  {
    return null;
  }

  @Override
  public boolean hasColumns()
  {
    return false;
  }

  @Override
  public boolean extendObjectList(WbConnection con, DataStore result, String aCatalog, String schemaPattern, String objectNamePattern, String[] requestedTypes)
  {
    if (!DbMetadata.typeIncluded(TYPE, requestedTypes)) return false;

    List<Db2Variable> variables = getVariables(con, schemaPattern, objectNamePattern);
    if (variables.isEmpty()) return false;

    for (Db2Variable var : variables)
    {
      int row = result.addRow();
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, null);
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, var.getSchema());
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, var.getObjectName());
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, var.getObjectType());
      result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, var.getComment());
      result.getRow(row).setUserObject(var);
    }
    return true;
  }

  public List<Db2Variable> getVariables(WbConnection con, String schemaPattern, String namePattern)
  {
    String select =
      "SELECT variable_schema, variable_name, \n" +
      "       case \n" +
      "         when data_type in ('SMALLINT', 'INTEGER', 'INT', 'BIGINT') then data_type\n" +
      "         when data_type in ('DECIMAL', 'DEC', 'NUMERIC', 'NUM') then data_type||'('||length||','||numeric_scale||')'\n" +
      "         when data_type in ('FLOAT', 'DECFLOAT') then data_type||'('||numeric_precision||')'\n" +
      "         else data_type||'('||length||')'\n" +
      "       end as data_type,\n" +
      "       ccsid, \n" +
      "       default, \n" +
      "       long_comment\n" +
      "from qsys2" + con.getMetadata().getCatalogSeparator() + "sysvariables";

    List<Db2Variable> result = new ArrayList<>();

    boolean whereAdded = false;

    if (StringUtil.isNonBlank(schemaPattern))
    {
      select += "\nwhere ";
      whereAdded = true;
      if (schemaPattern.indexOf('%') > -1)
      {
        select += "variable_schema LIKE '" + schemaPattern + "' ";
      }
      else
      {
        select += "variable_schema = '" + schemaPattern + "' ";
      }
    }

    if (StringUtil.isNonBlank(namePattern))
    {
      if (whereAdded)
      {
        select += "\n  AND ";
      }
      else
      {
        select += "\nWHERE ";
      }

      if (namePattern.indexOf('%') > -1)
      {
        select += "variable_name LIKE '" + namePattern + "' ";
      }
      else
      {
        select += "variable_name = '" + namePattern + "' ";
      }
    }

    select += " ORDER BY variable_schema, variable_name ";
    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logInfo("Db2iVariableReader.getVariables()", "Query to retrieve variables:\n" + select);
    }

    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      stmt = con.createStatementForQuery();
      rs = stmt.executeQuery(select);
      while (rs.next())
      {
        String schema = rs.getString("variable_schema");
        String name = rs.getString("variable_name");
        String remarks = rs.getString("long_comment");
        String dataType = rs.getString("data_type");
        String defaultValue = rs.getString("default");
        int ccsid = rs.getInt("ccsid");
        Db2Variable var = new Db2Variable(schema, name, dataType);
        var.setCcsid(ccsid);
        var.setDefaultValue(defaultValue);
        var.setComment(remarks);

        result.add(var);
      }
    }
    catch (Exception ex)
    {
      LogMgr.logError("Db2iVariableReader.getVariables()", "Could not retrieve list of variables using:\n" + select, ex);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return result;
  }

}
