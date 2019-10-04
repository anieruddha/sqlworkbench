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

import java.io.File;
import java.util.Properties;
import java.util.Vector;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;

import com.jcraft.jsch.Identity;
import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.agentproxy.Connector;
import com.jcraft.jsch.agentproxy.ConnectorFactory;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;

/**
 *
 * @author Thomas Kellerer
 */
public class PortForwarder
{
  public static final int DEFAULT_SSH_PORT = 22;

  private String sshHost;
  private String sshUser;
  private String password;
  private String privateKeyFile;

  private Session session;
  private int localPort;

  private boolean tryAgent;

  public PortForwarder(SshHostConfig config)
  {
    this.sshHost = config.getHostname();
    this.sshUser = config.getUsername();
    this.password = config.getPassword();
    this.tryAgent = config.getTryAgent();
    setPrivateKeyFile(config.getPrivateKeyFile());
  }

  private void setPrivateKeyFile(String keyFile)
  {
    this.privateKeyFile = null;
    if (keyFile != null)
    {
      File f = new File(keyFile);
      if (f.exists())
      {
        privateKeyFile = f.getAbsolutePath();
      }
    }
  }

  /**
   * Forwards a local port to a remote port.
   *
   * @param remoteHost  the remote host (as seen from the SSH host, typically the DB server)
   * @param remotePort  the port of the remote host
   *
   * @return the local port used for forwarding
   */
  public int startFowarding(String remoteDbServer, int remoteDbPort)
    throws JSchException
  {
    return startForwarding(remoteDbServer, remoteDbPort, 0, DEFAULT_SSH_PORT);
  }

  /**
   * Forwards a local port to a remote port.
   *
   * @param remoteHost      the remote host (as seen from the SSH host, typically the DB server)
   * @param remotePort      the port of the remote host
   * @param localPortToUse  the local port to use. If 0 choose a free port
   *
   * @return the local port  used for forwarding
   */
  public synchronized int startForwarding(String remoteDbServer, int remoteDbPort, int localPortToUse, int sshPort)
    throws JSchException
  {
    Properties props = new Properties();
    props.put("StrictHostKeyChecking", "no");
    JSch jsch = new JSch();

    String li = new CallerInfo(){}.toString();

    long start = System.currentTimeMillis();
    LogMgr.logDebug(li, "Connecting to SSH host: " + sshHost + ":" + sshPort + " using username: " + sshUser);

    boolean useAgent = tryAgent && tryAgent(jsch);

    if (!useAgent && privateKeyFile != null)
    {
      jsch.addIdentity(privateKeyFile, password);
    }

    session = jsch.getSession(sshUser, sshHost, sshPort);

    if (!useAgent && privateKeyFile == null)
    {
      props.put("PreferredAuthentications", "password,keyboard-interactive");
      session.setPassword(password);
    }

    session.setConfig(props);
    session.connect();
    long duration = System.currentTimeMillis() - start;
    LogMgr.logInfo(li, "Connected to SSH host: " + sshHost + ":" + sshPort + " using username: " + sshUser + " (" + duration + "ms)");

    if (localPortToUse < 0) localPortToUse = 0;

    localPort = session.setPortForwardingL(localPortToUse, remoteDbServer, remoteDbPort);
    LogMgr.logInfo(li, "Port forwarding established: localhost:"  + localPort + " -> " + remoteDbServer + ":" + remoteDbPort + " through host " + sshHost);

    return localPort;
  }

  private boolean tryAgent(JSch jsh)
  {
    try
    {
      Connector connector = ConnectorFactory.getDefault().createConnector();
      if (connector == null) return false;

      IdentityRepository irepo = new RemoteIdentityRepository(connector);
      Vector<Identity> identities = irepo.getIdentities();
      if (identities.size() > 0)
      {
        LogMgr.logInfo(new CallerInfo(){}, "Using " + identities.size() + " identities from agent: " + connector.getName());
        jsh.setIdentityRepository(irepo);
        return true;
      }
    }
    catch (Throwable th)
    {
      LogMgr.logError("PortForwarder.tryAgent()", "Error when accessing agent", th);
    }
    return false;
  }

  @Override
  public String toString()
  {
    return this.sshUser + "@" + this.sshHost + " localport: " + this.localPort;
  }

  public synchronized boolean isConnected()
  {
    return session != null && session.isConnected();
  }

  public int getLocalPort()
  {
    return localPort;
  }

  public synchronized void close()
  {
    if (isConnected())
    {
      LogMgr.logDebug(new CallerInfo(){}, "Disconnecting ssh session to host: " + session.getHost());
      session.disconnect();
    }
    session = null;
    localPort = -1;
  }
}
