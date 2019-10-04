/*
 * SaveDataAsAction.java
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

import javax.swing.SwingUtilities;

import workbench.resource.ResourceMgr;

import workbench.gui.components.WbTable;
import workbench.gui.dialogs.export.DataStoreExporter;

import workbench.util.EncodingUtil;
import workbench.util.StringUtil;

/**
 * Save the content of the ResultSet as an external file.
 *
 * @see workbench.gui.dialogs.export.DataStoreExporter
 * @author Thomas Kellerer
 */
public class SaveDataAsAction
  extends WbAction
{
  private WbTable client;
  private String lastDirKey;

  public SaveDataAsAction(WbTable aClient)
  {
    super();
    this.client = aClient;
    this.initMenuDefinition("MnuTxtSaveDataAs");
    this.setIcon("save-as");
    this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
    this.setEnabled(false);
  }

  public void setLastDirKey(String saveAsConfig)
  {
    this.lastDirKey = StringUtil.trimToNull(saveAsConfig);
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    EncodingUtil.fetchEncodings();
    final DataStoreExporter exporter = new DataStoreExporter(client.getDataStore(), client, lastDirKey);
    SwingUtilities.invokeLater(exporter::saveAs);
  }
}
