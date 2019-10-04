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
package workbench.ssh;

import java.io.Serializable;
import java.util.Objects;

import workbench.db.ConnectionMgr;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SshConfig
  implements Serializable
{
  private boolean changed;

  private SshHostConfig hostConfig;
  private int localPort;
  private int dbPort;
  private String dbHostname;
  private String privateKeyFile;
  private String sshHostConfigName;

  public SshConfig()
  {
  }

  public String getSshHostConfigName()
  {
    return sshHostConfigName;
  }

  public void setSshHostConfigName(String configName)
  {
    this.changed = StringUtil.stringsAreNotEqual(sshHostConfigName, configName);
    this.sshHostConfigName = StringUtil.trimToNull(configName);
    this.hostConfig = null;
  }

  public SshHostConfig getSshHostConfig()
  {
    if (sshHostConfigName != null)
    {
      SshHostConfig config = SshConfigMgr.getDefaultInstance().getHostConfig(sshHostConfigName);
      if (config != null)
      {
        return config;
      }
    }
    return hostConfig;
  }

  public SshHostConfig getHostConfig()
  {
    return hostConfig;
  }

  public void setHostConfig(SshHostConfig config)
  {
    if (config == null) return;

    if (config.getConfigName() != null)
    {
      this.setSshHostConfigName(config.getConfigName());
    }
    else
    {
      this.changed = this.hostConfig == null || !config.equals(this.hostConfig);
      this.hostConfig = config.createCopy();
      this.sshHostConfigName = null;
    }
  }

  /**
   * Returns the local port that should be used for port forwarding.
   *
   * @return the local port or 0 if a free port should be chosen automatically.
   */
  public int getLocalPort()
  {
    return localPort;
  }

  public void setLocalPort(int port)
  {
    changed = changed || port != localPort;
    this.localPort = port < 0 ? 0 : port;
  }

  public int getDbPort()
  {
    return dbPort;
  }

  public void setDbPort(int port)
  {
    if (port > 0 && port != dbPort)
    {
      changed = true;
      dbPort = port;
    }
  }

  public String getDbHostname()
  {
    return dbHostname;
  }

  public void setDbHostname(String hostname)
  {
    if (StringUtil.equalStringOrEmpty(dbHostname, hostname) == false)
    {
      this.changed = true;
      this.dbHostname = hostname;
    }
  }

  public boolean isValid()
  {
    return hostConfig != null  && hostConfig.isValid();
  }

  public void resetChanged()
  {
    changed = false;
  }

  public boolean isChanged()
  {
    return changed;
  }

  public void copyFrom(SshConfig config)
  {
    if (config == this) return;
    if (config.getSshHostConfigName() != null)
    {
      setSshHostConfigName(config.getSshHostConfigName());
    }
    else
    {
      setHostConfig(config.getSshHostConfig().createStatefulCopy());
    }
    setLocalPort(config.getLocalPort());
    setDbHostname(config.getDbHostname());
    setDbPort(config.getDbPort());
  }

  public SshConfig createCopy()
  {
    SshConfig copy = new SshConfig();
    copy.localPort = this.localPort;
    copy.privateKeyFile = this.privateKeyFile;
    copy.changed = this.changed;
    copy.dbPort = this.dbPort;
    copy.dbHostname = this.dbHostname;
    copy.sshHostConfigName = this.sshHostConfigName;
    if (this.sshHostConfigName == null && hostConfig != null)
    {
      copy.hostConfig = hostConfig.createCopy();
    }
    return copy;
  }

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 19 * hash + Objects.hashCode(this.hostConfig);
    hash = 19 * hash + this.dbPort;
    hash = 19 * hash + Objects.hashCode(this.dbHostname);
    if (localPort > 0)
    {
      hash = 19 * hash + this.localPort;
    }
    return hash;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;

    SshConfig other = (SshConfig)obj;
    if (this.dbPort != other.dbPort) return false;
    if (this.localPort > 0 || other.localPort > 0)
    {
      if (this.localPort != other.localPort) return false;
    }
    if (!StringUtil.equalStringIgnoreCase(this.dbHostname, other.dbHostname)) return false;
    if (!Objects.equals(this.hostConfig, other.hostConfig)) return false;

    return true;
  }

  public String getInfoString()
  {
    int currentPort = ConnectionMgr.getInstance().getSshManager().getLocalPort(this);
    String info = "";
    if (currentPort > 0)
    {
      info += "localhost:" + currentPort + " > ";
    }
    info += getSshHostConfig().getInfoString();
    info += " > " + dbHostname;
    if (dbPort > 0) info += ":" + dbPort;
    return info;
  }

  /**
   * This method is only here for backward compatibility to be able
   * to read XML profiles with the old SshConfig structure.
   */
  public void setHostname(String host)
  {
    if (this.hostConfig == null)
    {
      this.changed = true;
      this.hostConfig = new SshHostConfig();
    }
    this.hostConfig.setHostname(host);
  }

  /**
   * This method is only here for backward compatibility to be able
   * to read XML profiles with the old SshConfig structure.
   */
  public void setUsername(String name)
  {
    if (this.hostConfig == null)
    {
      this.changed = true;
      this.hostConfig = new SshHostConfig();
    }
    this.hostConfig.setUsername(name);
  }

  /**
   * This method is only here for backward compatibility to be able
   * to read XML profiles with the old SshConfig structure.
   */
  public void setPrivateKeyFile(String keyFile)
  {
    if (this.hostConfig == null)
    {
      this.changed = true;
      this.hostConfig = new SshHostConfig();
    }
    this.hostConfig.setPrivateKeyFile(keyFile);
  }

  /**
   * This method is only here for backward compatibility to be able
   * to read XML profiles with the old SshConfig structure.
   */
  public void setSshPort(int port)
  {
    if (this.hostConfig == null)
    {
      this.changed = true;
      this.hostConfig = new SshHostConfig();
    }
    this.hostConfig.setSshPort(port);
  }

  /**
   * This method is only here for backward compatibility to be able
   * to read XML profiles with the old SshConfig structure.
   */
  public void setTryAgent(boolean flag)
  {
    if (this.hostConfig == null)
    {
      this.changed = true;
      this.hostConfig = new SshHostConfig();
    }
    this.hostConfig.setTryAgent(flag);
  }

  /**
   * This method is only here for backward compatibility to be able
   * to read XML profiles with the old SshConfig structure.
   */
  public void setPassword(String password)
  {
    if (this.hostConfig == null)
    {
      this.changed = true;
      this.hostConfig = new SshHostConfig();
    }
    this.hostConfig.setPassword(password);
  }


}
