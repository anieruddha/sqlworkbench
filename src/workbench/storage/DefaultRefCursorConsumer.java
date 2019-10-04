/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2018 Thomas Kellerer.
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
package workbench.storage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.WbConnection;

import workbench.storage.reader.ResultHolder;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class DefaultRefCursorConsumer
  implements RefCursorConsumer
{
  private final List<DataStore> refCursorData = new ArrayList<>();
  private final Map<Integer, String> refCursorColumns = new HashMap<>();;
  private boolean onlyRefCursors;
  private final WbConnection sourceConnection;
  private int dbmsRefType = Types.REF_CURSOR;
  private final Set<String> refTypeNames = CollectionUtil.caseInsensitiveSet();

  public DefaultRefCursorConsumer(ResultInfo rs, WbConnection conn)
  {
    sourceConnection = conn;
    int dbmsTypeOverride = conn.getDbSettings().getRefCursorDataType();
    if (dbmsTypeOverride != Integer.MIN_VALUE)
    {
      dbmsRefType = dbmsTypeOverride;
    }
    refTypeNames.addAll(conn.getDbSettings().getRefCursorTypeNames());
    initRefCursorNames(rs);
  }

  @Override
  public Object readRefCursor(ResultHolder rs, int columnIndex)
    throws SQLException
  {
    String name = null;
    if (refCursorColumns != null)
    {
      name = refCursorColumns.get(columnIndex);
    }

    if (name == null)
    {
      name = "[" + ResourceMgr.getString("LblTabResult") + " " + getResults().size() + "]";
    }

    ResultSet refCursor = null;
    try
    {
      refCursor = (ResultSet)rs.getObject(columnIndex);
      DataStore ds = new DataStore(refCursor, sourceConnection, true);
      ds.setResultName(name);
      this.refCursorData.add(ds);
    }
    catch (SQLException ex)
    {
      LogMgr.logWarning(new CallerInfo(){}, "Could not retrieve refcursor for column: " + columnIndex, ex);
      throw ex;
    }
    catch (Exception ex)
    {
      LogMgr.logWarning(new CallerInfo(){}, "Could not retrieve refcursor for column: " + columnIndex, ex);
    }
    finally
    {
      SqlUtil.close(refCursor);
    }
    return name;
  }

  @Override
  public boolean containsOnlyRefCursors()
  {
    return onlyRefCursors;
  }

  @Override
  public Collection<String> getRefCursorColumns()
  {
    if (refCursorColumns == null) return Collections.emptyList();
    return Collections.unmodifiableCollection(refCursorColumns.values());
  }

  @Override
  public List<DataStore> getResults()
  {
    return Collections.unmodifiableList(refCursorData);
  }

  private void initRefCursorNames(ResultInfo info)
  {
    if (info == null) return;

    try
    {
      for (int i = 0; i < info.getColumnCount(); i++)
      {
        if (isRefCursor(info.getColumnType(i), info.getDbmsTypeName(i)))
        {
          refCursorColumns.put(i + 1, info.getColumnName(i));
        }
      }
      this.onlyRefCursors = info.getColumnCount() == refCursorColumns.size();
    }
    catch (Throwable th)
    {
      LogMgr.logWarning(new CallerInfo(){}, "Could not check for refcursor columns", th);
    }
  }

  @Override
  public boolean isRefCursor(int jdbcType, String dbmsType)
  {
    return jdbcType == dbmsRefType || refTypeNames.contains(dbmsType);
  }

}
