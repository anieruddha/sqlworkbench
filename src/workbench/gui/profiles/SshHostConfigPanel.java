/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2016 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.sql-workbench.eu/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.eu
 */
package workbench.gui.profiles;


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import workbench.resource.ResourceMgr;
import workbench.ssh.PortForwarder;
import workbench.ssh.SshHostConfig;
import workbench.ssh.SshManager;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.WbFilePicker;

import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class SshHostConfigPanel
  extends JPanel
{
  private boolean canUseAgent;
  private SshHostConfig currentConfig;

  public SshHostConfigPanel()
  {
    this(false);
  }

  public SshHostConfigPanel(boolean showConfigName)
  {
    initComponents();
    if (!showConfigName)
    {
      this.remove(labelConfigName);
      this.remove(configName);
    }
    keyPassFile.setAllowMultiple(false);
    keyPassFile.setLastDirProperty("workbench.ssh.keypass.lastdir");
    keyPassFile.setToolTipText(labelKeyPass.getToolTipText());
  }

  public void checkAgentUsage()
  {
    WbThread th = new WbThread("Check JNA libraries")
    {
      @Override
      public void run()
      {
        canUseAgent = SshManager.canUseAgent();
        if (!canUseAgent)
        {
          WbSwingUtilities.invoke(SshHostConfigPanel.this::disableAgentCheckBox);
        }
      }
    };
    th.start();
  }

  private void disableAgentCheckBox()
  {
    useAgent.setEnabled(false);
    useAgent.setToolTipText(ResourceMgr.getString("d_LblSshAgentNotAvailable"));
  }

  public void setConfig(SshHostConfig config)
  {
    clear();

    if (config != null)
    {
      configName.setText(StringUtil.coalesce(config.getConfigName(), ""));
      hostname.setText(StringUtil.coalesce(config.getHostname(), ""));
      username.setText(StringUtil.coalesce(config.getUsername(), ""));
      password.setText(StringUtil.coalesce(config.getPassword(), ""));
      keyPassFile.setFilename(config.getPrivateKeyFile());
      if (useAgent.isEnabled())
      {
        useAgent.setSelected(config.getTryAgent());
      }

      int port = config.getSshPort();
      if (port > 0 && port != PortForwarder.DEFAULT_SSH_PORT)
      {
        sshPort.setText(Integer.toString(port));
      }
      this.currentConfig = config;
    }
  }


  @Override
  public void setEnabled(boolean flag)
  {
    super.setEnabled(flag);
    keyPassFile.setEnabled(flag);
    hostname.setEnabled(flag);
    username.setEnabled(flag);
    password.setEnabled(flag);
    sshPort.setEnabled(flag);
    if (flag)
    {
      useAgent.setEnabled(canUseAgent);
    }
    else
    {
      useAgent.setEnabled(false);
    }
  }

  public void clear()
  {
    configName.setText("");
    keyPassFile.setFilename("");
    hostname.setText("");
    username.setText("");
    password.setText("");
    sshPort.setText("");
    useAgent.setSelected(false);
    currentConfig = null;
  }

  private void syncConfig(SshHostConfig config)
  {
    config.setUsername(StringUtil.trimToNull(username.getText()));
    config.setHostname(StringUtil.trimToNull(hostname.getText()));
    config.setPassword(password.getText());
    config.setSshPort(StringUtil.getIntValue(sshPort.getText(), 0));
    config.setPrivateKeyFile(StringUtil.trimToNull(keyPassFile.getFilename()));
    config.setTryAgent(useAgent.isSelected());
  }

  public SshHostConfig getConfig()
  {
    if (currentConfig != null)
    {
      syncConfig(currentConfig);
      currentConfig.setConfigName(configName.getText());
      return currentConfig;
    }

    String user = StringUtil.trimToNull(username.getText());
    String host = StringUtil.trimToNull(hostname.getText());
    if (user == null || host == null)
    {
      return null;
    }

    SshHostConfig config = new SshHostConfig(null);
    syncConfig(config);
    return config;
  }

  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents()
  {
    GridBagConstraints gridBagConstraints;

    labelHost = new JLabel();
    hostname = new JTextField();
    labelUsername = new JLabel();
    username = new JTextField();
    labelPassword = new JLabel();
    password = new JPasswordField();
    labelSshPort = new JLabel();
    sshPort = new JTextField();
    keyPassFile = new WbFilePicker();
    labelKeyPass = new JLabel();
    useAgent = new JCheckBox();
    labelConfigName = new JLabel();
    configName = new JTextField();

    setLayout(new GridBagLayout());

    labelHost.setLabelFor(hostname);
    labelHost.setText(ResourceMgr.getString("LblSshHost")); // NOI18N
    labelHost.setToolTipText(ResourceMgr.getString("d_LblSshHost")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(5, 5, 0, 0);
    add(labelHost, gridBagConstraints);

    hostname.setToolTipText(ResourceMgr.getString("d_LblSshHost")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(5, 5, 0, 11);
    add(hostname, gridBagConstraints);

    labelUsername.setLabelFor(username);
    labelUsername.setText(ResourceMgr.getString("LblSshUser")); // NOI18N
    labelUsername.setToolTipText(ResourceMgr.getString("d_LblSshUser")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(5, 5, 0, 0);
    add(labelUsername, gridBagConstraints);

    username.setToolTipText(ResourceMgr.getString("d_LblSshUser")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(5, 5, 0, 11);
    add(username, gridBagConstraints);

    labelPassword.setLabelFor(password);
    labelPassword.setText(ResourceMgr.getString("LblSshPwd")); // NOI18N
    labelPassword.setToolTipText(ResourceMgr.getString("d_LblSshPwd")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(5, 5, 0, 0);
    add(labelPassword, gridBagConstraints);

    password.setToolTipText(ResourceMgr.getString("d_LblSshPwd")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(5, 5, 0, 11);
    add(password, gridBagConstraints);

    labelSshPort.setLabelFor(sshPort);
    labelSshPort.setText(ResourceMgr.getString("LblSshPort")); // NOI18N
    labelSshPort.setToolTipText(ResourceMgr.getString("d_LblSshPort")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(5, 5, 0, 0);
    add(labelSshPort, gridBagConstraints);

    sshPort.setToolTipText(ResourceMgr.getString("d_LblSshPort")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(5, 5, 0, 11);
    add(sshPort, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(5, 5, 0, 11);
    add(keyPassFile, gridBagConstraints);

    labelKeyPass.setText(ResourceMgr.getString("LblSshKeyFile")); // NOI18N
    labelKeyPass.setToolTipText(ResourceMgr.getString("d_LblSshKeyFile")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(5, 5, 0, 0);
    add(labelKeyPass, gridBagConstraints);

    useAgent.setText(ResourceMgr.getString("LblSshUseAgent")); // NOI18N
    useAgent.setToolTipText(ResourceMgr.getString("d_LblSshUseAgent")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(4, 1, 0, 0);
    add(useAgent, gridBagConstraints);

    labelConfigName.setText(ResourceMgr.getString("LblSshCfgName")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(5, 5, 0, 0);
    add(labelConfigName, gridBagConstraints);

    configName.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent evt)
      {
        configNameActionPerformed(evt);
      }
    });
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(5, 5, 0, 11);
    add(configName, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents

  private void configNameActionPerformed(ActionEvent evt)//GEN-FIRST:event_configNameActionPerformed
  {//GEN-HEADEREND:event_configNameActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_configNameActionPerformed


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private JTextField configName;
  private JTextField hostname;
  private WbFilePicker keyPassFile;
  private JLabel labelConfigName;
  private JLabel labelHost;
  private JLabel labelKeyPass;
  private JLabel labelPassword;
  private JLabel labelSshPort;
  private JLabel labelUsername;
  private JPasswordField password;
  private JTextField sshPort;
  private JCheckBox useAgent;
  private JTextField username;
  // End of variables declaration//GEN-END:variables
}
