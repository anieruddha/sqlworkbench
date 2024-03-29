/*
 * EditorOptionsPanel.java
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
package workbench.gui.settings;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import workbench.interfaces.Restoreable;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.StoreableKeyStroke;

import workbench.gui.components.TextFieldWidthAdjuster;

/**
 *
 * @author  Thomas Kellerer
 */
public class MacroOptionsPanel
	extends JPanel
	implements Restoreable
{

	public MacroOptionsPanel()
	{
		super();
		initComponents();
		enterRuns.setText(ResourceMgr.getFormattedString("LblMacroPopEnterRun", KeyEvent.getKeyText(KeyEvent.VK_ENTER)));
		TextFieldWidthAdjuster adjuster = new TextFieldWidthAdjuster();
		adjuster.adjustAllFields(this);
		StoreableKeyStroke space = new StoreableKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
		StoreableKeyStroke shiftSpace = new StoreableKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.SHIFT_MASK));
		StoreableKeyStroke tab = new StoreableKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
		ComboBoxModel model = new DefaultComboBoxModel(new Object[] { space, shiftSpace, tab});
		cbExpansionKey.setModel(model);
	}

	@Override
	public void restoreSettings()
	{

		StoreableKeyStroke key = new StoreableKeyStroke(GuiSettings.getExpansionKey());
		cbExpansionKey.setSelectedItem(key);
		closeEsc.setSelected(GuiSettings.getCloseMacroPopupWithEsc());
		saveWksp.setSelected(GuiSettings.getStoreMacroPopupInWorkspace());
		enterRuns.setSelected(GuiSettings.getRunMacroWithEnter());
    showQuickFilter.setSelected(GuiSettings.getShowFilterInMacroPopup());
	}

	@Override
	public void saveSettings()
	{
		StoreableKeyStroke key = (StoreableKeyStroke) cbExpansionKey.getSelectedItem();
		GuiSettings.setExpansionKey(key.getKeyStroke());
		GuiSettings.setCloseMacroPopupWithEsc(closeEsc.isSelected());
		GuiSettings.setStoreMacroPopupInWorkspace(saveWksp.isSelected());
		GuiSettings.setRunMacroWithEnter(enterRuns.isSelected());
    GuiSettings.setShowFilterInMacroPopup(showQuickFilter.isSelected());
	}


	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents()
  {
    GridBagConstraints gridBagConstraints;

    jLabel3 = new JLabel();
    cbExpansionKey = new JComboBox();
    jPanel1 = new JPanel();
    closeEsc = new JCheckBox();
    saveWksp = new JCheckBox();
    showQuickFilter = new JCheckBox();
    enterRuns = new JCheckBox();

    setLayout(new GridBagLayout());

    jLabel3.setLabelFor(cbExpansionKey);
    jLabel3.setText(ResourceMgr.getString("LblExpandKey")); // NOI18N
    jLabel3.setToolTipText(ResourceMgr.getString("d_LblExpandKey")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    add(jLabel3, gridBagConstraints);

    cbExpansionKey.setModel(new DefaultComboBoxModel(new String[] { "Space", "Shift-Space", "Tab" }));
    cbExpansionKey.setToolTipText(ResourceMgr.getString("d_LblExpandKey")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.insets = new Insets(0, 11, 0, 15);
    add(cbExpansionKey, gridBagConstraints);

    jPanel1.setBorder(BorderFactory.createTitledBorder(ResourceMgr.getString("LblMacroPopup"))); // NOI18N
    jPanel1.setLayout(new GridBagLayout());

    closeEsc.setText(ResourceMgr.getString("LblMacroPopCloseEsc")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    jPanel1.add(closeEsc, gridBagConstraints);

    saveWksp.setText(ResourceMgr.getString("LblMacroPopStoreWksp")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    jPanel1.add(saveWksp, gridBagConstraints);

    showQuickFilter.setText(ResourceMgr.getString("LblMacroPopFilter")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    jPanel1.add(showQuickFilter, gridBagConstraints);

    enterRuns.setText(ResourceMgr.getString("LblMacroPopEnterRun")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    jPanel1.add(enterRuns, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(10, 0, 0, 0);
    add(jPanel1, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private JComboBox cbExpansionKey;
  private JCheckBox closeEsc;
  private JCheckBox enterRuns;
  private JLabel jLabel3;
  private JPanel jPanel1;
  private JCheckBox saveWksp;
  private JCheckBox showQuickFilter;
  // End of variables declaration//GEN-END:variables

}
