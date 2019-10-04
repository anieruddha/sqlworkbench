/*
 * OpenFileAction.java
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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.KeyStroke;

import workbench.interfaces.TextFileContainer;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ConnectionProfile;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ExtensionFileFilter;
import workbench.gui.components.FileEncodingAccessoryPanel;
import workbench.gui.components.WbFileChooser;
import workbench.gui.menu.RecentFileManager;
import workbench.gui.sql.SqlPanel;

import workbench.util.EncodingUtil;
import workbench.util.ExceptionUtil;
import workbench.util.FileUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.WbProperties;

/**
 * Open a new file in the main window, with the option to open the file in a new tab
 *
 * @author Thomas Kellerer
 */
public class OpenFileAction
  extends WbAction
{
  private MainWindow mainWindow;
  private TextFileContainer container;
  private static final String TOOLNAME = "directories";
  private static final String LAST_DIR_KEY = "last.script.dir";
  private File fileToLoad;

  public OpenFileAction(MainWindow mainWindow)
  {
    this(mainWindow, (TextFileContainer)null);
  }

  public OpenFileAction(TextFileContainer client)
  {
    this(null, client);
  }

  public OpenFileAction(MainWindow window, TextFileContainer client)
  {
    super();
    mainWindow = window;
    container = client;
    this.initMenuDefinition("MnuTxtFileOpen", KeyStroke.getKeyStroke(KeyEvent.VK_O, PlatformShortcuts.getDefaultModifier()));
    this.setIcon("Open");
    this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
    setCreateMenuSeparator(true);
  }

  public OpenFileAction(MainWindow window, WbFile toLoad)
  {
    super();
    mainWindow = window;
    container = null;
    fileToLoad = toLoad;
    setMenuText(toLoad.getName());
    setTooltip(toLoad.getFullPath());
    this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
    setCreateMenuSeparator(true);
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    if (fileToLoad == null)
    {
      selectAndLoad();
      return;
    }
    final MainWindow window = getWindow();
    final WbFile f = new WbFile(fileToLoad);

    final String fname = f.getFullPath();
    EventQueue.invokeLater(() ->
    {
      String encodingToUse = FileUtil.detectFileEncoding(f);

      final SqlPanel currentPanel = getCurrentPanel();
      if (currentPanel == null) return;

      if (!currentPanel.checkAndSaveFile()) return;

      currentPanel.readFile(fname, encodingToUse);
      window.invalidate();
      // this is necessary to update all menus and toolbars
      // even if the current tab didn't really change
      window.currentTabChanged();
    });
  }

  private void selectAndLoad()
  {
    EncodingUtil.fetchEncodings();

    final MainWindow window = getWindow();
    final SqlPanel currentPanel = getCurrentPanel();

    if (currentPanel != null)
    {
      if (!currentPanel.checkAndSaveFile()) return;
    }

    try
    {
      File lastDir = getLastSQLDir(window);

      WbFileChooser fc = new WbFileChooser(lastDir);
      fc.setSettingsID("workbench.editor.file.opendialog");
      fc.setMultiSelectionEnabled(true);

      FileEncodingAccessoryPanel acc = new FileEncodingAccessoryPanel(window);

      fc.addEncodingPanel(acc);
      fc.addChoosableFileFilter(ExtensionFileFilter.getSqlFileFilter());

      boolean rememberNewTabSetting = window.getCurrentSqlPanel() != null;

      int answer = fc.showOpenDialog(window);

      GuiSettings.setAutoDetectFileEncoding(acc.getAutoDetect());

      if (answer == JFileChooser.APPROVE_OPTION)
      {
        final String encoding = acc.getEncoding();

        storeLastSQLDir(window, fc.getCurrentDirectory());

        Settings.getInstance().setDefaultFileEncoding(encoding);

        File[] files = fc.getSelectedFiles();

        final boolean openInNewTab;
        if (files.length == 1)
        {
          openInNewTab = acc.openInNewTab();
        }
        else
        {
          openInNewTab = true;
        }

        if (rememberNewTabSetting)
        {
          Settings.getInstance().setProperty("workbench.file.newtab", openInNewTab);
        }

        for (File sf : files)
        {
          final WbFile f = new WbFile(sf);

          final String fname = f.getFullPath();
          EventQueue.invokeLater(() ->
          {
            SqlPanel sql;
            String encodingToUse = encoding;
            if (StringUtil.isEmptyString(encodingToUse))
            {
              encodingToUse = FileUtil.detectFileEncoding(f);
            }

            if (openInNewTab)
            {
              sql = (SqlPanel)window.addTab();
            }
            else
            {
              sql = currentPanel;
            }

            if (sql != null)
            {
              sql.readFile(fname, encodingToUse);
            }
            RecentFileManager.getInstance().editorFileLoaded(f);
            window.invalidate();
            // this is necessary to update all menus and toolbars
            // even if the current tab didn't really change
            window.currentTabChanged();
          });
        }
      }
    }
    catch (Throwable th)
    {
      LogMgr.logError("EditorPanel.openFile()", "Error selecting file", th);
      WbSwingUtilities.showErrorMessage(ExceptionUtil.getDisplay(th));
    }
  }

  private MainWindow getWindow()
  {
    if (mainWindow != null) return mainWindow;
    if (container != null)
    {
      return container.getMainWindow();
    }
    return null;
  }

  private SqlPanel getCurrentPanel()
  {
    if (getWindow() != null)
    {
      return getWindow().getCurrentSqlPanel();
    }
    return null;
  }

  public static void storeLastSQLDir(MainWindow window, File lastDir)
  {
    if (Settings.getInstance().getStoreScriptDirInWksp())
    {
      window.getToolProperties(TOOLNAME).setProperty(LAST_DIR_KEY, lastDir.getAbsolutePath());
    }
    else
    {
      Settings.getInstance().setLastSqlDir(lastDir.getAbsolutePath());
    }
  }

  private static String getProfileDir(MainWindow win)
  {
    if (win == null) return null;
    ConnectionProfile profile = win.getCurrentProfile();
    if (profile == null) return null;
    return profile.getDefaultDirectory();
  }

  public static File getLastSQLDir(MainWindow window)
  {
    SqlPanel currentPanel = window == null ? null : window.getCurrentSqlPanel();
    File lastDir = null;
    String profileDir = getProfileDir(window);

    if (GuiSettings.getFollowFileDirectory() && currentPanel != null && currentPanel.hasFileLoaded())
    {
      WbFile f = new WbFile(currentPanel.getCurrentFileName());
      if (f.getParent() != null)
      {
        lastDir = f.getParentFile();
      }
    }
    else if (StringUtil.isNonBlank(profileDir))
    {
      File f = new File(profileDir);
      if (f.exists())
      {
        lastDir = f;
      }
    }
    else
    {
      lastDir = new File(Settings.getInstance().getLastSqlDir());
      if (Settings.getInstance().getStoreScriptDirInWksp())
      {
        WbProperties props = window == null ? null : window.getToolProperties(TOOLNAME);
        String dirname = props == null ? null : props.getProperty(LAST_DIR_KEY, null);
        if (StringUtil.isNonBlank(dirname))
        {
          lastDir = new File(dirname);
        }
      }
    }

    if (lastDir == null)
    {
      lastDir = GuiSettings.getDefaultFileDir();
    }
    return lastDir;
  }
}
