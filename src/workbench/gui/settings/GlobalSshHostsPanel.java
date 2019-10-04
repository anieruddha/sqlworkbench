/*
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import workbench.gui.actions.DeleteListEntryAction;
import workbench.gui.actions.NewListEntryAction;
import workbench.gui.components.DividerBorder;
import workbench.gui.components.WbToolbar;

import workbench.interfaces.FileActions;
import workbench.interfaces.Restoreable;
import workbench.ssh.SshConfigMgr;
import workbench.ssh.SshHostConfig;

import workbench.gui.WbSwingUtilities;
import workbench.gui.profiles.SshHostConfigPanel;

import workbench.util.StringUtil;


/**
 *
 * @author Thomas Kellerer
 */
public class GlobalSshHostsPanel
	extends JPanel
	implements Restoreable, ListSelectionListener, FileActions,
	           PropertyChangeListener
{
	private JList hostList;
	private SshHostConfigPanel hostConfig;
	private WbToolbar toolbar;
	private DefaultListModel<SshHostConfig> configs;

	public GlobalSshHostsPanel()
	{
		super();
		setLayout(new BorderLayout());

		hostList = new JList();
		hostList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		hostList.setBorder(new EmptyBorder(2,2,2,2));

    int width = WbSwingUtilities.calculateCharWidth(hostList, 16);
    Dimension ps = hostList.getPreferredSize();


		JScrollPane scroll = new JScrollPane(hostList);
    scroll.setPreferredSize(new Dimension(width, ps.height));

		this.toolbar = new WbToolbar();
		this.toolbar.add(new NewListEntryAction(this));
		this.toolbar.add(new DeleteListEntryAction(this));
		toolbar.setBorder(DividerBorder.BOTTOM_DIVIDER);

    hostConfig = new SshHostConfigPanel(true);

		add(toolbar, BorderLayout.NORTH);
		add(scroll, BorderLayout.WEST);
		add(hostConfig, BorderLayout.CENTER);
	}

	@Override
	public void saveSettings()
	{
    applyConfig();
		List<SshHostConfig> l = new ArrayList<>();
    Enumeration<SshHostConfig> elements = this.configs.elements();
		while (elements.hasMoreElements())
		{
      l.add(elements.nextElement());
		}
    SshConfigMgr.getDefaultInstance().setConfigs(l);
    SshConfigMgr.getDefaultInstance().saveGlobalConfig();
	}

	@Override
	public void restoreSettings()
	{
		configs = new DefaultListModel();
    List<SshHostConfig> sshDefs = SshConfigMgr.getDefaultInstance().getGlobalConfigs();
		for (SshHostConfig config : sshDefs)
		{
			configs.addElement(config.createCopy());
		}
		hostList.setModel(configs);
		hostList.addListSelectionListener(this);
		hostList.setSelectedIndex(0);
	}

  private void applyConfig()
  {
    SshHostConfig config = hostConfig.getConfig();
    replaceConfig(config);
  }

  private void replaceConfig(SshHostConfig config)
  {
    if (config == null) return;

    for (int i=0; i < this.configs.size(); i++)
    {
      SshHostConfig cfg = configs.get(i);
      if (StringUtil.equalStringIgnoreCase(cfg.getConfigName(), config.getConfigName()))
      {
        configs.setElementAt(cfg, i);
      }
    }
  }

	@Override
	public void valueChanged(ListSelectionEvent evt)
	{
    applyConfig();
    SshHostConfig config = configs.getElementAt(evt.getFirstIndex());
    this.hostConfig.setConfig(config);
	}

	@Override
	public void saveItem() throws Exception
	{
	}

	@Override
	public void deleteItem() throws Exception
	{
		int index = hostList.getSelectedIndex();
		if (index > -1)
		{
			configs.remove(index);
		}
		if (hostList.getModel().getSize() == 0)
		{
			hostConfig.setConfig(null);
		}
		hostList.repaint();
	}

	@Override
	public void newItem(boolean copyCurrent) throws Exception
	{
		try
		{
      SshHostConfig config = new SshHostConfig("SSH Host");
			configs.addElement(config);
			hostList.setSelectedIndex(configs.size()-1);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals("name"))
		{
			hostList.repaint();
		}
	}

}
