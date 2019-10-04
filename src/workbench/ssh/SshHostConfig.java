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

import java.io.Serializable;
import java.util.Objects;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SshHostConfig
  implements Serializable
{
  private boolean changed;

  private String sshHost;
  private String password;
  private String username;
  private String privateKeyFile;
  private String configName;
  private String temporaryPassword;

  private int sshPort = PortForwarder.DEFAULT_SSH_PORT;
  private boolean tryAgent;

  public SshHostConfig()
  {
  }

  public SshHostConfig(String configName)
  {
    this.configName = StringUtil.trimToNull(configName);
  }

  public boolean isGlobalConfig()
  {
    return this.configName != null;
  }

  public int getSshPort()
  {
    return sshPort;
  }

  public void setSshPort(int port)
  {
    if (port > 0 && port != sshPort)
    {
      changed = true;
      sshPort = port;
    }
  }

  public boolean getTryAgent()
  {
    return tryAgent;
  }

  public void setTryAgent(boolean flag)
  {
    changed = tryAgent != flag;
    tryAgent = flag;
  }

  public void setTemporaryPassword(String temporaryPassword)
  {
    this.temporaryPassword = temporaryPassword;
  }

  public void clearTemporaryPassword()
  {
    this.temporaryPassword = null;
  }

  public boolean hasTemporaryPassword()
  {
    return this.temporaryPassword != null;
  }

  public String getHostname()
  {
    return sshHost;
  }

  public void setHostname(String sshHost)
  {
    changed = !StringUtil.equalStringIgnoreCase(this.sshHost, sshHost);
    this.sshHost = sshHost;
  }

  public String getPassword()
  {
    if (temporaryPassword != null) return temporaryPassword;
    return password;
  }

  public void setPassword(String password)
  {
    changed = !StringUtil.equalStringIgnoreCase(this.password, password);
    this.password = password;
  }

  public String getUsername()
  {
    return username;
  }

  public void setUsername(String username)
  {
    changed = !StringUtil.equalStringIgnoreCase(this.username, username);
    this.username = username;
  }

  public String getPrivateKeyFile()
  {
    return privateKeyFile;
  }

  public void setPrivateKeyFile(String privateKeyFile)
  {
    changed = !StringUtil.equalStringIgnoreCase(this.privateKeyFile, privateKeyFile);
    this.privateKeyFile = privateKeyFile;
  }

  public String getConfigName()
  {
    return configName;
  }

  public void setConfigName(String configName)
  {
    this.configName = StringUtil.trimToNull(configName);
  }

  public boolean isValid()
  {
    return this.sshHost != null && this.username != null;
  }
  
  public SshHostConfig createStatefulCopy()
  {
    SshHostConfig copy = createCopy();
    copy.changed = this.changed;
    return copy;
  }

  public SshHostConfig createCopy()
  {
    SshHostConfig copy = new SshHostConfig();
    copy.sshHost = this.sshHost;
    copy.password = this.password;
    copy.temporaryPassword = this.temporaryPassword;
    copy.privateKeyFile = this.privateKeyFile;
    copy.sshPort = this.sshPort;
    copy.tryAgent = this.tryAgent;
    copy.username = this.username;
    copy.changed = false;
    copy.configName = this.configName;
    return copy;
  }

  @Override
  public int hashCode()
  {
    int hash = 3;
    hash = 37 * hash + Objects.hashCode(this.sshHost);
    hash = 37 * hash + Objects.hashCode(this.username);
    hash = 37 * hash + Objects.hashCode(this.privateKeyFile);
    hash = 37 * hash + this.sshPort;
    return hash;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final SshHostConfig other = (SshHostConfig)obj;
    if (this.sshPort != other.sshPort) return false;
    if (!Objects.equals(this.sshHost, other.sshHost)) return false;
    if (!Objects.equals(this.username, other.username)) return false;
    if (!Objects.equals(this.privateKeyFile, other.privateKeyFile)) return false;
    return true;
  }

  public String getInfoString()
  {
    if (sshPort == 0 || sshPort == PortForwarder.DEFAULT_SSH_PORT)
    {
      return sshHost;
    }
    return sshHost += ":" + sshPort;
  }

  @Override
  public String toString()
  {
    return configName;
  }

}
