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
package workbench.gui.dbobjects.objecttree;

import java.sql.SQLException;

import workbench.log.LogMgr;

import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.DbSettings;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import static workbench.gui.dbobjects.objecttree.TreeLoader.*;

/**
 *
 * @author Thomas Kellerer
 */
public class GlobalTypeNode
    extends ObjectTreeNode
{
  public GlobalTypeNode(String name)
  {
    super(name, TYPE_DBO_TYPE_NODE);
    setAllowsChildren(true);
  }

  @Override
  public boolean loadChildren(WbConnection connection)
  {
    if (connection == null) return false;
    DbSettings dbs = connection.getDbSettings();
    if (dbs == null) return false;
    DbMetadata meta = connection.getMetadata();
    if (meta == null) return false;

    try
    {
      DataStore ds = meta.getObjects(null, null, new String[]{getName()});
      for (int row=0; row < ds.getRowCount(); row ++)
      {
        DbObject dbo = (DbObject)ds.getRow(row).getUserObject();
        if (dbo != null)
        {
          ObjectTreeNode node = new ObjectTreeNode(dbo);
          add(node);
        }
      }
    }
    catch (SQLException sql)
    {
      LogMgr.logError("GlobalTreeNode.loadChildren()", "Could not load type: " + getName(), sql);
    }
    setChildrenLoaded(true);
    return true;
  }


}
