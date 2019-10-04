/*
 * ObjectSearchAction.java
 *
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
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.resource.ResourceMgr;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.tools.ObjectSourceSearchPanel;

/**
 * Action to display the Object Searcher window
 *
 * @author Thomas Kellerer
 */
public class ObjectSearchAction
  extends WbAction
{
  private MainWindow parent;

  public ObjectSearchAction(MainWindow win)
  {
    super();
    this.initMenuDefinition("MnuTxtObjectSearch");
    this.setMenuItemName(ResourceMgr.MNU_TXT_TOOLS);
    this.setIcon("searchsource");
    this.parent = win;
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    if (parent != null)
    {
      WbSwingUtilities.showWaitCursor(parent);
    }
    try
    {
      ObjectSourceSearchPanel panel = new ObjectSourceSearchPanel();
      panel.showWindow(parent);
    }
    finally
    {
      if (parent != null) WbSwingUtilities.showDefaultCursor(parent);
    }
  }
}
