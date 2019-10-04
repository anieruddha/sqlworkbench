/*
 * ConnectionPropertiesEditor.java
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
package workbench.gui.profiles;

import java.awt.BorderLayout;
import java.awt.Window;
import java.sql.Types;

import javax.swing.JCheckBox;

import workbench.interfaces.FileActions;
import workbench.resource.ResourceMgr;

import workbench.db.ConnectionProfile;

import workbench.gui.components.MapEditor;
import workbench.gui.components.ValidatingDialog;

import workbench.storage.DataStore;

/**
 *
 * @author Thomas Kellerer
 */
public class ConnectionPropertiesEditor
	extends MapEditor
	implements FileActions
{
	private JCheckBox copyProps;

	public ConnectionPropertiesEditor(ConnectionProfile profile)
	{
    super(profile.getConnectionProperties());
		copyProps = new JCheckBox(ResourceMgr.getString("LblCpProps2System"));
		copyProps.setToolTipText(ResourceMgr.getDescription("LblCpProps2System"));
		this.add(copyProps, BorderLayout.SOUTH);
		this.copyProps.setSelected(profile.getCopyExtendedPropsToSystem());
	}

  @Override
  protected DataStore createDataStore()
  {
		String[] cols = new String[] { ResourceMgr.getString("TxtConnDataPropName"), ResourceMgr.getString("TxtConnDataPropValue") };
		int[] types = new int[] { Types.VARCHAR, Types.VARCHAR };
		int[] sizes = new int[] { 15, 5 };

		return new DataStore(cols, types, sizes);
  }

	public boolean getCopyToSystem()
	{
		return this.copyProps.isSelected();
	}

	public static void editProperties(Window parent, ConnectionProfile profile)
	{
		ConnectionPropertiesEditor editor = new ConnectionPropertiesEditor(profile);
    editor.optimizeColumnWidths();

		boolean ok = ValidatingDialog.showConfirmDialog(parent, editor, ResourceMgr.getString("TxtEditConnPropsWindowTitle"));
		if (ok)
		{
			profile.setConnectionProperties(editor.getProperties());
			profile.setCopyExtendedPropsToSystem(editor.getCopyToSystem());
		}
	}
}
