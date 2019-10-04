/*
 * TransposeRowAction.java
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

import java.awt.Component;
import java.awt.event.ActionEvent;

import workbench.resource.GuiSettings;

import workbench.gui.components.WbTable;
import workbench.gui.sql.SqlPanel;

import workbench.storage.DataStore;
import workbench.storage.DatastoreTransposer;

/**
 * An action to transpose the selected row in a WbTable.
 *
 * @author Thomas Kellerer
 * @see DatastoreTransposer
 */
public class TransposeRowAction
  extends WbAction
{
  private WbTable client;

  public TransposeRowAction(WbTable aClient)
  {
    super();
    this.client = aClient;
    this.initMenuDefinition("MnuTxtTransposeRow");
    this.setEnabled(client != null);
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    DatastoreTransposer transpose = new DatastoreTransposer(client.getDataStore());
    transpose.setUseTableNameForResult(GuiSettings.getUseTablenameAsResultName());
    int[] rows = null;
    if (client.getSelectedRowCount() > 0)
    {
      rows = client.getSelectedRows();
    }
    DataStore ds = transpose.transposeRows(rows);
    if (ds.getResultName() != null)
    {
      ds.setResultName("<[ " + ds.getResultName() + " ]>");
    }
    showDatastore(ds);
  }

  private void showDatastore(DataStore ds)
  {
    SqlPanel p = findPanel();
    if (p != null)
    {
      p.showData(ds);
    }
  }

  private SqlPanel findPanel()
  {
    if (client == null) return null;
    Component c = client.getParent();
    while (c != null)
    {
      if (c instanceof SqlPanel)
      {
        return (SqlPanel)c;
      }
      c = c.getParent();
    }
    return null;
  }

  @Override
  public boolean useInToolbar()
  {
    return false;
  }
}
