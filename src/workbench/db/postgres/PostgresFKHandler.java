/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2018 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DefaultFKHandler;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresFKHandler
  extends DefaultFKHandler
{
  public PostgresFKHandler(WbConnection conn)
  {
    super(conn);
  }

  @Override
  public boolean supportsRemarks()
  {
    return true;
  }

  @Override
  protected DataStore getRawKeyList(TableIdentifier tbl, boolean exported)
    throws SQLException
  {
    DataStore ds = super.getRawKeyList(tbl, exported);
    ds.addColumn(REMARKS_COLUMN);
    updateConstraintResult(tbl, ds);
    return ds;
  }

  @Override
  protected DataStore getKeyList(TableIdentifier tbl, boolean getOwnFk, boolean includeNumericRuleValue)
  {
    DataStore keys = super.getKeyList(tbl, getOwnFk, includeNumericRuleValue);
    updateConstraintResult(tbl, keys);
    return keys;
  }

  private void updateConstraintResult(TableIdentifier tbl, DataStore keys)
  {
    int remarksColumn = keys.getColumnIndex(COLUMN_NAME_REMARKS);
    int nameColumn = keys.getColumnIndex("FK_NAME");
    if (remarksColumn > -1)
    {
      List<String> names = new ArrayList<>(keys.getRowCount());
      for (int row = 0; row < keys.getRowCount(); row++)
      {
        names.add(keys.getValueAsString(row, nameColumn));
      }
      Map<String, String> remarks = getConstraintRemarks(tbl, names);
      for (Map.Entry<String, String> entry : remarks.entrySet())
      {
        if (StringUtil.isNonBlank(entry.getValue()))
        {
          int row = findConstraint(keys, nameColumn, entry.getKey());
          if (row > -1)
          {
            keys.setValue(row, remarksColumn, entry.getValue());
          }
        }
      }
    }
  }

  private int findConstraint(DataStore keys, int nameColumn, String name)
  {
    for (int row = 0; row < keys.getRowCount(); row++)
    {
      String fkName = keys.getValueAsString(row, nameColumn);
      if (StringUtil.equalStringIgnoreCase(fkName, name))
      {
        return row;
      }
    }
    return -1;
  }

  public Map<String, String> getConstraintRemarks(TableIdentifier table, List<String> fkNames)
  {
    Map<String, String> result = new HashMap<>();

    if (CollectionUtil.isEmpty(fkNames)) return result;

    String sql =
      "select c.conname, \n" +
      "       obj_description(c.oid, 'pg_constraint') as remarks\n" +
      "from pg_constraint c\n" +
      "  join pg_class t on t.oid = c.conrelid\n" +
      "  join pg_namespace s on s.oid = t.relnamespace\n" +
      "where contype = 'f' \n" +
      "  and (s.nspname, t.relname) = (?,?) \n" +
      "  and conname in (";

    for (int i=0; i < fkNames.size(); i++)
    {
      if (i > 0)
      {
        sql += ',';
      }
      sql += "'" + SqlUtil.escapeQuotes(fkNames.get(i)) + "'";
    }
    sql += ")";

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logDebug(new CallerInfo(){}, "Retrieving FK comments using:\n" + SqlUtil.replaceParameters(sql, table.getRawSchema(), table.getRawTableName()));
    }

    PreparedStatement pstmt = null;
    ResultSet rs = null;

    try
    {
      pstmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
      pstmt.setString(1, table.getRawSchema());
      pstmt.setString(2, table.getRawTableName());
      rs = pstmt.executeQuery();
      while (rs.next())
      {
        String conname = rs.getString(1);
        String comment = rs.getString(2);
        if (StringUtil.isNonBlank(comment))
        {
          result.put(conname, comment);
        }
      }
    }
    catch (Throwable th)
    {
      LogMgr.logError(new CallerInfo(){}, "Could not read FK comments using:\n" + SqlUtil.replaceParameters(sql, table.getRawSchema(), table.getRawTableName()), th);
    }
    finally
    {
      SqlUtil.close(pstmt, rs);
    }
    return result;
  }


}
