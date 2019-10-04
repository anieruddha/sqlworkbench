/*
 * MakeNonCharInListAction.java
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

import workbench.interfaces.TextSelectionListener;
import workbench.resource.ResourceMgr;

import workbench.gui.editor.CodeTools;
import workbench.gui.sql.EditorPanel;

/**
 * Make an "IN" list with elements that don't need single quotes.
 *
 * @see workbench.gui.editor.CodeTools#makeInListForNonChar()
 * @see MakeInListAction
 * 
 * @author Thomas Kellerer
 */
public class MakeNonCharInListAction
  extends WbAction
  implements TextSelectionListener
{
  private EditorPanel client;

  public MakeNonCharInListAction(EditorPanel aClient)
  {
    super();
    this.client = aClient;
    this.client.addSelectionListener(this);
    this.initMenuDefinition("MnuTxtMakeNonCharInList");
    this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
    this.setEnabled(false);
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    CodeTools tools = new CodeTools(client);
    tools.makeInListForNonChar();
  }

  @Override
  public void selectionChanged(int newStart, int newEnd)
  {
    if (newEnd > newStart)
    {
      int startLine = this.client.getSelectionStartLine();
      int endLine = this.client.getSelectionEndLine();
      this.setEnabled(startLine < endLine);
    }
    else
    {
      this.setEnabled(false);
    }
  }

}
