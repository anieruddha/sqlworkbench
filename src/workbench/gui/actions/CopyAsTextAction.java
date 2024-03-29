/*
 * CopyAsTextAction.java
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
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.resource.GuiSettings;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

import workbench.gui.components.ClipBoardCopier;
import workbench.gui.components.WbTable;

/**
 * Action to copy the contents of a WbTable as tab-separated text to the clipboard
 *
 * @see workbench.gui.components.ClipBoardCopier
 * @see GuiSettings#alwaysDisplayCopyAsTextDialog()
 *
 * @author Thomas Kellerer
 */
public class CopyAsTextAction
  extends WbAction
{
  private final WbTable client;
  protected boolean copySelected;

  public CopyAsTextAction(WbTable aClient)
  {
    super();
    this.client = aClient;
    this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
    this.initMenuDefinition("MnuTxtDataToClipboard", KeyStroke.getKeyStroke(KeyEvent.VK_Y, PlatformShortcuts.getDefaultModifier()));
    copySelected = false;
    this.setEnabled(false);
  }

  @Override
  public boolean hasCtrlModifier()
  {
    return true;
  }

  @Override
  public boolean hasShiftModifier()
  {
    return true;
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    ClipBoardCopier copier = new ClipBoardCopier(this.client);
    boolean copyHeaders = true;
    boolean selectColumns = false;

    if (invokedByMouse(e))
    {
      copyHeaders = !isShiftPressed(e);
      selectColumns = isCtrlPressed(e) || GuiSettings.alwaysDisplayCopyAsTextDialog();
    }
    else
    {
      selectColumns = GuiSettings.alwaysDisplayCopyAsTextDialog();
    }
    copier.copyDataToClipboard(copyHeaders, copySelected, selectColumns);
  }

}
