/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
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
package workbench.gui.dbobjects.objecttree.vertica;

import workbench.db.TableIdentifier;

import workbench.gui.dbobjects.objecttree.ObjectTreeNode;
import workbench.gui.dbobjects.objecttree.TreeLoader;



/**
 *
 * @author Thomas Kellerer
 */
public class ProjectionNode
  extends ObjectTreeNode
{
  private final TableIdentifier table;

  public ProjectionNode(String projectionName, TableIdentifier baseTable)
  {
    super(projectionName, TreeLoader.TYPE_PROJECTION_NODE);
    setAllowsChildren(true);
    table = baseTable;
    add(new ProjectionColumnsNode());
    add(new BuddyListNode());
  }

  public TableIdentifier getBaseTable()
  {
    return table;
  }
}
