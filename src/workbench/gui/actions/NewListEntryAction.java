/*
 * NewListEntryAction.java
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

import workbench.interfaces.FileActions;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 * @author Thomas Kellerer
 */
public class NewListEntryAction
  extends WbAction
{
  private FileActions client;

  public NewListEntryAction(FileActions aClient, String aKey)
  {
    super();
    this.client = aClient;
    this.initMenuDefinition(aKey);
    this.setIcon("New");
  }

  public NewListEntryAction(FileActions aClient)
  {
    super();
    this.client = aClient;
    this.setIcon("New");
    String tip = ResourceMgr.getDescription("LblNewListEntry", true);
    this.initMenuDefinition(ResourceMgr.getString("LblNewListEntry"), tip, null);
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    try
    {
      this.client.newItem(isShiftPressed(e));
    }
    catch (Exception ex)
    {
      LogMgr.logError("NewListEntryAction.executeAction()", "Error creating new list entry", ex);
    }

  }

  @Override
  public boolean useInToolbar()
  {
    return false;
  }
}
