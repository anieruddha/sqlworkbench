/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2018 Thomas Kellerer.
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
package workbench.ssh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.WbProperties;

import static workbench.db.IniProfileStorage.*;


/**
 *
 * @author Thomas Kellerer
 */
public class SshConfigMgr
{
  private static final String PREFIX = "config";
  private static final String CONFIG_NAME = ".name";

  private final List<SshHostConfig> globalConfigs = new ArrayList<>();
  private boolean loaded = false;
  private boolean changed = false;
  private final WbFile configFile;

  private static class InstanceHolder
  {
    protected static final SshConfigMgr INSTANCE = new SshConfigMgr(Settings.getInstance().getGlogalSshConfigFile());
  }

  public static final SshConfigMgr getDefaultInstance()
  {
    return InstanceHolder.INSTANCE;
  }

  public SshConfigMgr(WbFile cfgFile)
  {
    this.configFile = cfgFile;
  }

  public List<SshHostConfig> getGlobalConfigs()
  {
    ensureLoaded();
    return Collections.unmodifiableList(globalConfigs);
  }

  public void saveGlobalConfig()
  {
    if (!loaded) return;

    WbProperties props = new WbProperties(0);
    synchronized (PREFIX)
    {
      for (int i=0; i < globalConfigs.size(); i++)
      {
        String key = StringUtil.formatInt(i + 1, 4).toString();
        writeConfig(globalConfigs.get(i), props, key);
      }
    }

    final CallerInfo ci = new CallerInfo(){};
    try
    {
      if (globalConfigs.isEmpty() && changed)
      {
        configFile.delete();
        LogMgr.logInfo(ci, "Global SSH host configuration file " + configFile.getFullPath() + " removed.");
      }
      else
      {
        props.saveToFile(configFile);
        LogMgr.logInfo(ci, "Global SSH host configurations saved to: " + configFile.getFullPath());
      }
      changed = false;
    }
    catch (Exception ex)
    {
      LogMgr.logError(ci, "Could not save global SSH host configurations", ex);
    }
  }

  public void setConfigs(List<SshHostConfig> newConfigs)
  {
    synchronized (PREFIX)
    {
      globalConfigs.clear();
      globalConfigs.addAll(newConfigs);
    }
    changed = true;
    loaded = true;
  }

  public void replaceConfig(SshHostConfig config)
  {
    if (config == null) return;

    int index = findConfig(config);
    if (index > -1)
    {
      globalConfigs.set(index, config);
    }
    else
    {
      globalConfigs.add(config);
    }
    this.changed = true;
  }

  public SshHostConfig getHostConfig(String configName)
  {
    if (StringUtil.isBlank(configName)) return null;

    ensureLoaded();
    int index = findConfig(configName);
    if (index > -1)
    {
      return globalConfigs.get(index);
    }
    return null;
  }

  private int findConfig(SshHostConfig config)
  {
    if (config == null) return -1;
    return findConfig(config.getConfigName());
  }

  private int findConfig(String configName)
  {
    if (StringUtil.isBlank(configName)) return -1;

    for (int i=0; i < globalConfigs.size(); i++)
    {
      if (StringUtil.equalStringIgnoreCase(configName, globalConfigs.get(i).getConfigName()))
      {
        return i;
      }
    }
    return -1;
  }

  private void ensureLoaded()
  {
    synchronized (PREFIX)
    {
      if (!loaded)
      {
        loadConfigs();
      }
    }
  }

  private void writeConfig(SshHostConfig config, WbProperties props, String key)
  {
    if (!key.startsWith("."))
    {
      key = "." + key;
    }
    props.setProperty(PREFIX + key + PROP_SSH_HOST, config.getHostname());
    props.setProperty(PREFIX + key + PROP_SSH_USER, config.getUsername());
    props.setProperty(PREFIX + key + PROP_SSH_KEYFILE, config.getPrivateKeyFile());
    props.setProperty(PREFIX + key + PROP_SSH_PWD, config.getPassword());
    props.setProperty(PREFIX + key + PROP_SSH_PORT, config.getSshPort());
    props.setProperty(PREFIX + key + CONFIG_NAME, config.getConfigName());
    if (config.getTryAgent())
    {
      props.setProperty(PREFIX + key + PROP_SSH_TRY_AGENT, config.getTryAgent());
    }
  }

  private SshHostConfig readConfig(WbProperties props, String key)
  {
    if (!key.startsWith("."))
    {
      key = "." + key;
    }
    String hostName = props.getProperty(PREFIX + key + PROP_SSH_HOST, null);
    String user = props.getProperty(PREFIX + key + PROP_SSH_USER, null);
    String keyFile = props.getProperty(PREFIX + key + PROP_SSH_KEYFILE, null);
    String pwd = props.getProperty(PREFIX + key + PROP_SSH_PWD, null);
    String name = props.getProperty(PREFIX + key + CONFIG_NAME, null);
    int port = props.getIntProperty(PREFIX + key + PROP_SSH_PORT, PortForwarder.DEFAULT_SSH_PORT);
    boolean tryAgent = props.getBoolProperty(PREFIX + key + PROP_SSH_TRY_AGENT, false);
    if (name != null && hostName != null && user != null)
    {
      SshHostConfig config = new SshHostConfig(name);
      config.setPassword(pwd);
      config.setHostname(hostName);
      config.setUsername(user);
      config.setPrivateKeyFile(keyFile);
      config.setTryAgent(tryAgent);
      config.setSshPort(port);
      return config;
    }
    return null;
  }

  private Set<String> getConfigKeys(WbProperties props)
  {
    Set<String> uniqueKeys = new TreeSet<>();
    Set<String> keys = props.getKeys();
    for (String key : keys)
    {
      String[] elements = key.split("\\.");
      if (elements.length > 2)
      {
        uniqueKeys.add(elements[1]);
      }
    }
    return uniqueKeys;
  }

  private void loadConfigs()
  {
    if (configFile == null || !configFile.exists()) return;

    globalConfigs.clear();
    try
    {
      WbProperties props = new WbProperties(0);
      props.loadTextFile(configFile);
      Set<String> keys = getConfigKeys(props);
      for (String key : keys)
      {
        SshHostConfig config = readConfig(props, key);
        if (config != null)
        {
          globalConfigs.add(config);
        }
      }
      Collections.sort(globalConfigs, (SshHostConfig o1, SshHostConfig o2) -> StringUtil.compareStrings(o1.getConfigName(), o2.getConfigName(), true));

      loaded = true;
      changed = false;
      LogMgr.logInfo("SshConfigMgr.loadConfigs()", "Loaded global SSH host configurations from " + configFile.getFullPath());
    }
    catch (Exception ex)
    {
      LogMgr.logWarning("SshConfigMgr.loadConfigs()", "Could not load global SSH host configurations", ex);
      loaded = false;
    }
  }

}
