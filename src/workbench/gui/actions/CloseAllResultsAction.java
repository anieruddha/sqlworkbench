/*
 * CloseAllResultsAction.java
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

import workbench.gui.sql.SqlPanel;

/**
 * An action to close all result tabs of a SqlPanel.
 *
 * @author Thomas Kellerer
 */
public class CloseAllResultsAction
  extends WbAction
{
  private SqlPanel panel;

  public CloseAllResultsAction(SqlPanel sqlPanel)
  {
    super();
    panel = sqlPanel;
    this.initMenuDefinition("MnuTxtCloseAllResults");
    this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
    this.setIcon(null);
    this.setEnabled(panel.getResultTabCount() > 0);
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    panel.closeAllResults();
  }

}
