/*
 * BrowserLauncher.java
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
package workbench.util;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Optional;

import workbench.log.CallerInfo;

import workbench.db.ConnectionInfoBuilder;
import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;

import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;

import workbench.util.function.WbConsumer;

/**
 * Some utility functions for the Desktop class.
 *
 * @author Thomas Kellerer
 * @author Andreas Krist
 */
public class BrowserLauncher
{
  public static void openEmail(final String email, final WbConnection currentConnection)
  {
    final WbConsumer<URI> uriConsumer;
    final Optional<Desktop> desktopOpt = getDesktop(Action.MAIL);
    if (desktopOpt.isPresent())
    {
      uriConsumer = desktopOpt.get()::mail;
    }
    else if (PlatformHelper.isLinux())
    {
      uriConsumer = BrowserLauncher::xdgOpenUrl;
    }
    else
    {
      uriConsumer = null;
      LogMgr.logError("BrowserLauncher.openEmail()", "Desktop not supported!", null);
      WbSwingUtilities.showErrorMessage("Desktop not supported by your Java version");
    }

    if (uriConsumer == null)
    {
      return;
    }

    WbThread t = new WbThread(() ->
    {
      try
      {
        String subject = urlEncode("SQL Workbench/J (Build " + ResourceMgr.getBuildNumber() + ") - feedback");
        String body = ResourceMgr.getFormattedString("TxtFeedbackMail", LogMgr.getLogfile().getFullPath());
        body += "\n\nSQL Workbench/J " + ResourceMgr.getBuildInfo();
        body += "\n" + ResourceMgr.getFullJavaInfo();
        long maxMem = MemoryWatcher.MAX_MEMORY / (1024 * 1024);
        body += "\n" + ResourceMgr.getOSInfo() + ", max. memory=" + maxMem + "MB";

        if (currentConnection != null)
        {
          ConnectionInfoBuilder builder = new ConnectionInfoBuilder();
          String info = builder.getPlainTextDisplay(currentConnection, 5);
          if (StringUtil.isNonEmpty(info))
          {
            String msg = ResourceMgr.getFormattedString("TxtFeedbackMailConInfo", info);
            body += "\n\n" + msg;
          }
        }
        body = urlEncode(body);
        URI uri = new URI("mailto:" + email + "?subject=" + subject + "&body=" + body);
        uriConsumer.accept(uri);
      }
      catch (Exception e)
      {
        LogMgr.logError("BrowserLauncher.openEmail()", "Could not open email program", e);
        WbSwingUtilities.showErrorMessage(ExceptionUtil.getDisplay(e));
      }
    }, "OpenBrowser");
    t.start();
  }

  private static String urlEncode(String str)
    throws Exception
  {
    return URLEncoder.encode(str, "UTF-8").replace("+", "%20");
  }

  public static void openURL(String url)
    throws Exception
  {
    openURL(new URI(url));
  }

  private static Optional<Desktop> getDesktop(Action action)
  {
    return Desktop.isDesktopSupported() ? Optional.ofNullable(Desktop.getDesktop())
      .filter(desktop -> desktop.isSupported(action)) :
      Optional.empty();
  }

  /**
   * Opens the given <code>url</code> on linux using xdg-open.
   *
   * @param url
   *
   * @throws IOException
   */
  private static void xdgOpenUrl(URI url)
    throws IOException
  {
    Runtime.getRuntime().exec(new String[] {"xdg-open", url.toString()});
  }

  public static void openURL(final URI url)
    throws Exception
  {
    final URI realURI;

    String urlString = url.toString();

    if (urlString.indexOf('#') > -1 && GuiSettings.useHTMLRedirectForAnchor())
    {
      File tmpfile = File.createTempFile("sqlwb_show_help", ".html");
      tmpfile.deleteOnExit();

      String redirect =
        "<html><head>\n" +
        "<meta http-equiv=\"refresh\" content=\"0;url=" + urlString + "\"/>\n" +
        "</head></html>";

      FileUtil.writeString(tmpfile, redirect, "UTF-8", false);
      realURI = tmpfile.toURI();
      LogMgr.logDebug(new CallerInfo(){}, "Redirecting to an anchor using intermediate URL: " + realURI.toString());
    }
    else
    {
      realURI = url;
    }

    final Optional<Desktop> optDesktop = getDesktop(Action.BROWSE);
    final WbConsumer<URI> uriConsumer;

    if (optDesktop.isPresent())
    {
      uriConsumer = optDesktop.get()::browse;
    }
    else if (PlatformHelper.isLinux())
    {
      uriConsumer = BrowserLauncher::xdgOpenUrl;
    }
    else
    {
      uriConsumer = null;
      LogMgr.logError(new CallerInfo(){}, "Desktop or Plattform not supported!", null);
      WbSwingUtilities.showErrorMessage("Starting the browser is not supported by your Java installation");
    }

    if (uriConsumer == null)
    {
      return;
    }

    try
    {
      new WbThread(() ->
      {
        try
        {
          LogMgr.logDebug(new CallerInfo(){}, "Opening URL: " + url.toString());
          uriConsumer.accept(url);
        }
        catch (Exception e)
        {
          LogMgr.logError(new CallerInfo(){}, "Error starting browser", e);
          WbSwingUtilities.showErrorMessage(ExceptionUtil.getDisplay(e));
        }

      }, "OpenBrowser").start();
    }
    catch (Exception e)
    {
      LogMgr.logError(new CallerInfo(){}, "Error starting browser", e);
      WbSwingUtilities.showErrorMessage(ExceptionUtil.getDisplay(e));
    }
  }
}
