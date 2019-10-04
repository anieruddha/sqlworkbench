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

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import workbench.storage.reader.ResultHolder;

/**
 *
 * @author Thomas Kellerer
 */
public interface RefCursorConsumer
{
  Object readRefCursor(ResultHolder rs, int columnIndex)
    throws SQLException;
  List<DataStore> getResults();
  Collection<String> getRefCursorColumns();
  boolean containsOnlyRefCursors();
  boolean isRefCursor(int jdbcType, String dbmsType);

  public static class DummyConsumer
    implements RefCursorConsumer
  {
    @Override
    public Object readRefCursor(ResultHolder rs, int columnIndex)
    {
      return null;
    }

    @Override
    public List<DataStore> getResults()
    {
      return Collections.emptyList();
    }

    @Override
    public Collection<String> getRefCursorColumns()
    {
      return Collections.emptyList();
    }

    @Override
    public boolean containsOnlyRefCursors()
    {
      return false;
    }

    @Override
    public boolean isRefCursor(int jdbcType, String dbmsType)
    {
      return false;
    }
  }
}
