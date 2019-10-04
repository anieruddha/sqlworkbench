/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2016 Thomas Kellerer.
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
package workbench.gui.profiles;

import java.io.Serializable;

import javax.swing.tree.TreePath;

/**
 *
 * @author Thomas Kellerer
 */
public class TransferableProfileNode
  implements Serializable
{
  private final TreePath[] path;
  private final String sourceName;

  public TransferableProfileNode(TreePath[] tp, ProfileTree source)
  {
    path = tp;
    sourceName = source.getName();
  }

  public TreePath[] getPath()
  {
    return path;
  }

  public String getSourceName()
  {
    return sourceName;
  }
}
