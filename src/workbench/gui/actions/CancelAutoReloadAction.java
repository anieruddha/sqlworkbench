/*
 * SqlPanelReloadAction.java
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

import workbench.gui.sql.DwPanel;
import workbench.gui.sql.SqlPanel;

/**
 *
 * @author Thomas Kellerer
 */
public class CancelAutoReloadAction
  extends WbAction
{
  private SqlPanel client;

  public CancelAutoReloadAction(SqlPanel panel)
  {
    initMenuDefinition("MnuTxtRemoveRefresh");
    setClient(panel);
  }

  public final void setClient(SqlPanel panel)
  {
    client = panel;
    checkEnabled();
  }

  public void checkEnabled()
  {
    boolean isRegistered = false;
    DwPanel dw = client.getCurrentResult();
    if (dw != null)
    {
      isRegistered = client.getRefreshMgr().isRegistered(dw);
    }
    setEnabled(isRegistered);
  }

  @Override
  public void executeAction(ActionEvent evt)
  {
    DwPanel dw = client.getCurrentResult();
    client.getRefreshMgr().removeRefresh(dw);
    client.checkAutoRefreshIndicator(dw);
  }

  @Override
  public boolean useInToolbar()
  {
    return false;
  }
}
