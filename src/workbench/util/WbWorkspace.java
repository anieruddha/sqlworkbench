/*
 * WbWorkspace.java
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

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.gui.sql.PanelType;
import workbench.gui.sql.SqlHistory;

/**
 *
 * @author  Thomas Kellerer
 */
public class WbWorkspace
  implements Closeable
{
  public static final String TAB_PROP_PREFIX = "tab";

  private static final String VARIABLES_FILENAME = "variables.properties";
  private static final String TABINFO_FILENAME = "tabs.properties";
  private static final String TOOL_ENTRY_PREFIX = "toolprop_";

  private static final String CURSOR_POS_PROP = ".file.cursorpos";
  private static final String ENCODING_PROP = ".encoding";
  private static final String FILENAME_PROP = ".filename";

  private enum WorkspaceState
  {
    closed,
    reading,
    writing;
  }

  private ZipOutputStream zout;
  private ZipFile archive;

  private WorkspaceState state = WorkspaceState.closed;
  private int tabCount = -1;

  private WbProperties tabInfo = new WbProperties(0);
  private Map<String, WbProperties> toolProperties = new HashMap<>();
  private WbProperties variables = new WbProperties(0);
  private Map<Integer, SqlHistory> historyEntries = new HashMap<>();
  private String filename;
  private String loadError;

  public WbWorkspace(String archiveName)
  {
    if (archiveName == null) throw new NullPointerException("Filename cannot be null");
    this.filename = archiveName;
  }

  /**
   * Opens the workspace for writing.
   *
   * Nothing will be saved.
   *
   * To actually save the workspace content {@link #save()} needs to be called.
   *
   * If the workspace was already open, it is closed.
   * This will reset all tab specific properties.
   *
   * @throws IOException
   * @see #save()
   */
  public void openForWriting()
    throws IOException
  {
    close();

    File f = new File(filename);
    OutputStream out = new BufferedOutputStream(new FileOutputStream(f), 64*1024);
    zout = new ZipOutputStream(out);
    zout.setLevel(Settings.getInstance().getIntProperty("workbench.workspace.compression", 9));
    zout.setComment("SQL Workbench/J Workspace file");

    state = WorkspaceState.writing;
  }

  public String getLoadError()
  {
    return loadError;
  }

  public boolean isOpenForReading()
  {
    return this.state == WorkspaceState.reading;
  }

  public boolean isOpen()
  {
    return this.state != WorkspaceState.closed;
  }

  /**
   * Opens the workspace for reading.
   *
   * This will automatically load all properties stored in the workspace, but not the panel statements.
   *
   * If the workspace was already open, it is closed and all internal properties are discarded.
   *
   * @throws IOException
   * @see #readHistoryData(int, workbench.gui.sql.SqlHistory)
   */
  public boolean openForReading()
    throws IOException
  {
    close();
    clear();
    loadError = null;

    try
    {
      this.zout = null;
      this.archive = new ZipFile(filename);

      ZipEntry entry = archive.getEntry(TABINFO_FILENAME);
      long size = (entry != null ? entry.getSize() : 0);
      if (size <= 0)
      {
        // Old definition of tabs for builds before 103.2
        entry = archive.getEntry("tabinfo.properties");
      }

      readTabInfo(entry);
      readToolProperties();
      readVariables();

      tabCount = calculateTabCount();

      state = WorkspaceState.reading;
      return true;
    }
    catch (Throwable th)
    {
      LogMgr.logDebug(new CallerInfo(){}, "Could not open workspace file " + filename, th);
      loadError = th.getMessage();
      state = WorkspaceState.closed;
    }
    return false;
  }

  public void setFilename(String archiveName)
  {
    if (archiveName == null) throw new NullPointerException("Filename cannot be null");
    if (state != WorkspaceState.closed)
    {
      LogMgr.logError(new CallerInfo(){}, "setFilename() called although workspace is not closed!", new Exception("Backtrace"));
    }
    filename = archiveName;
  }

  public String getFilename()
  {
    return filename;
  }

  public Map<String, WbProperties> getToolProperties()
  {
    return toolProperties;
  }

  public void setEntryCount(int count)
  {
    tabInfo.setProperty("tab.total.count", count);
  }

  public void addHistoryEntry(int index, SqlHistory history)
  {
    this.historyEntries.put(index, history);
  }

  public WbProperties getVariables()
  {
    WbProperties props = new WbProperties(0);
    props.putAll(variables);
    return props;
  }

  public void setVariables(Properties newVars)
  {
    variables.clear();
    if (newVars != null)
    {
      variables.putAll(newVars);
    }
  }

  /**
   * Returns the number of available tab entries.
   *
   * Calling this method is only valid if {@link #openForReading() } has been called before.
   *
   * @return the number of tabs stored in this workspace or -1 if the workspace was not opened properly
   * @see #openForReading()
   */
  public int getEntryCount()
  {
    if (state != WorkspaceState.reading)
    {
      return -1;
    }
    //if (state != WorkspaceState.reading) throw new IllegalStateException("Workspace is not open for reading. Entry count is not available");
    return tabCount;
  }

  public PanelType getPanelType(int index)
  {
    String type = tabInfo.getProperty(TAB_PROP_PREFIX + index + ".type", "sqlPanel");
    try
    {
      return PanelType.valueOf(type);
    }
    catch (Exception e)
    {
      return PanelType.sqlPanel;
    }
  }

  /**
   *
   * @param anIndex
   * @param history
   * @throws IOException
   */
  public void readHistoryData(int anIndex, SqlHistory history)
    throws IOException
  {
    if (state != WorkspaceState.reading) throw new IllegalStateException("Workspace is not open for reading. Entry count is not available");

    ZipEntry e = this.archive.getEntry("WbStatements" + (anIndex + 1) + ".txt");
    if (e != null)
    {
      InputStream in = this.archive.getInputStream(e);
      history.readFromStream(in);
    }
  }

  public void flush()
  {
    if (zout != null)
    {
      try
      {
        zout.flush();
      }
      catch (Exception ex)
      {
        // ignore
      }
    }
  }

  public void save()
    throws IOException
  {
    if (this.zout != null)
    {
      saveTabInfo();
      saveToolProperties();
      saveVariables();
      saveHistory();
      historyEntries.clear();
    }
  }

  @Override
  public void close()
    throws IOException
  {
    FileUtil.closeQuietely(zout);
    FileUtil.closeQuietely(archive);
    zout = null;
    archive = null;
    state = WorkspaceState.closed;
  }

  public WbProperties getSettings()
  {
    return this.tabInfo;
  }

  public void prepareForSaving()
  {
    tabInfo.clear();
    historyEntries.clear();
  }

  private void clear()
  {
    toolProperties.clear();
    variables.clear();
    tabInfo.clear();
    historyEntries.clear();
  }

  private void readVariables()
  {
    if (archive == null) return;

    variables.clear();

    try
    {
 			ZipEntry entry = archive.getEntry(VARIABLES_FILENAME);
      if (entry != null && entry.getSize() > 0)
      {
        InputStream in = this.archive.getInputStream(entry);
        variables.load(in);
      }
    }
    catch (Exception ex)
    {
      LogMgr.logError("WbWorkspace.readVariables()", "Could not read variables file", ex);
    }
  }

  private void readToolProperties()
  {
    toolProperties.clear();
    Enumeration<? extends ZipEntry> entries = archive.entries();
    while (entries.hasMoreElements())
    {
      ZipEntry entry = entries.nextElement();
      String name = entry.getName();
      if (name.startsWith(TOOL_ENTRY_PREFIX))
      {
        WbFile f = new WbFile(name.substring(TOOL_ENTRY_PREFIX.length()));
        String toolkey = f.getFileName();
        WbProperties props = readProperties(entry);
        toolProperties.put(toolkey, props);
      }
    }
  }

  private int calculateTabCount()
  {
    // new property that stores the total count of tabs
    int count = tabInfo.getIntProperty("tab.total.count", -1);
    if (count > 0) return count;

    // Old tabinfo.properties format
    boolean found = true;
    int index = 0;
    while (found)
    {
      if (tabInfo.containsKey(TAB_PROP_PREFIX + index + ".maxrows") ||
          tabInfo.containsKey(TAB_PROP_PREFIX + index + ".title") ||
          tabInfo.containsKey(TAB_PROP_PREFIX + index + ".append.results"))
      {
        tabInfo.setProperty(TAB_PROP_PREFIX + index + ".type", PanelType.sqlPanel.toString());
        index ++;
      }
      else if (tabInfo.containsKey(TAB_PROP_PREFIX + index + ".type"))
      {
        index ++;
      }
      else
      {
        found = false;
      }
    }

    int dbExplorer = this.tabInfo.getIntProperty("dbexplorer.visible", 0);

    // now add the missing .type entries for the DbExplorer panels
    for (int i=0; i < dbExplorer; i++)
    {
      tabInfo.setProperty(TAB_PROP_PREFIX + index + ".type", PanelType.dbExplorer.toString());
      index ++;
    }
    return index;
  }

  private void saveVariables()
    throws IOException
  {
    if (CollectionUtil.isEmpty(variables)) return;

    try
    {
      ZipEntry entry = new ZipEntry(VARIABLES_FILENAME);
      this.zout.putNextEntry(entry);
      variables.save(this.zout);
    }
    catch (IOException ex)
    {
      LogMgr.logError("WbWorkspace.saveVariables()", "Could not write variables", ex);
      throw ex;
    }
    finally
    {
      zout.closeEntry();
    }
  }

  private void saveToolProperties()
    throws IOException
  {
    if (CollectionUtil.isEmpty(this.toolProperties)) return;
    try
    {
      for (Map.Entry<String, WbProperties> propEntry : toolProperties.entrySet())
      {
        ZipEntry entry = new ZipEntry(TOOL_ENTRY_PREFIX + propEntry.getKey() + ".properties");
        zout.putNextEntry(entry);
        propEntry.getValue().save(zout);
      }
    }
    catch (IOException ex)
    {
      LogMgr.logError("WbWorkspace.saveToolProperties()", "Could not write variables", ex);
      throw ex;
    }
    finally
    {
      zout.closeEntry();
    }
  }

  private void saveHistory()
    throws IOException
  {
    for (Map.Entry<Integer, SqlHistory> historyEntry : historyEntries.entrySet())
    {
      if (historyEntry.getValue() != null && historyEntry.getKey() != null)
      {
        try
        {
          int index = historyEntry.getKey();
          ZipEntry entry = new ZipEntry("WbStatements" + (index + 1) + ".txt");
          this.zout.putNextEntry(entry);
          historyEntry.getValue().writeToStream(zout);
        }
        catch (IOException ex)
        {
          LogMgr.logError("WbWorkspace.saveHistory()", "Could not history for tab index: " + historyEntry.getKey(), ex);
          throw ex;
        }
        finally
        {
          zout.closeEntry();
        }
      }
    }
  }

  private void saveTabInfo()
    throws IOException
  {
    if (CollectionUtil.isEmpty(tabInfo)) return;

    try
    {
      ZipEntry entry = new ZipEntry(TABINFO_FILENAME);
      this.zout.putNextEntry(entry);
      tabInfo.save(this.zout);
    }
    catch (IOException ex)
    {
      LogMgr.logError("WbWorkspace.saveToolProperties()", "Could not write variables", ex);
      throw ex;
    }
    finally
    {
      zout.closeEntry();
    }
  }

  private void readTabInfo(ZipEntry entry)
  {
    this.tabInfo = readProperties(entry);
  }

  private WbProperties readProperties(ZipEntry entry)
  {
    WbProperties props = new WbProperties(null, 1);
    if (entry == null)
    {
      return props;
    }

    try
    {
      InputStream in = this.archive.getInputStream(entry);
      props.load(in);
    }
    catch (Exception e)
    {
      LogMgr.logError("WbWorkspace.readProperties()", "Could not read property file: " + entry.getName(), e);
    }
    return props;
  }

  public void setSelectedTab(int anIndex)
  {
    this.tabInfo.setProperty("tab.selected", Integer.toString(anIndex));
  }

  public int getSelectedTab()
  {
    return StringUtil.getIntValue(this.tabInfo.getProperty("tab.selected", "0"));
  }

  public boolean isSelectedTabExplorer()
  {
    int index = getSelectedTab();
    return PanelType.dbExplorer == this.getPanelType(index);
  }

  public void setTabTitle(int index, String name)
  {
    String key = TAB_PROP_PREFIX + index + ".title";
    String encoded = StringUtil.escapeText(name, CharacterRange.RANGE_7BIT);
    this.tabInfo.setProperty(key, encoded);
  }

  public String getTabTitle(int index)
  {
    if (this.tabInfo == null) return null;
    String key = TAB_PROP_PREFIX + index + ".title";
    String value = (String)this.tabInfo.get(key);
    return StringUtil.decodeUnicode(value);
  }

  public int getExternalFileCursorPos(int tabIndex)
  {
    if (this.tabInfo == null) return -1;
    String key = TAB_PROP_PREFIX + tabIndex + CURSOR_POS_PROP;
    String value = (String)this.tabInfo.get(key);
    if (value == null) return -1;
    int result = -1;
    try
    {
      result = Integer.parseInt(value);
    }
    catch (Exception e)
    {
      result = -1;
    }

    return result;
  }

  public void setQueryTimeout(int index, int timeout)
  {
    String key = TAB_PROP_PREFIX + index + ".timeout";
    this.tabInfo.setProperty(key, Integer.toString(timeout));
  }

  public int getQueryTimeout(int index)
  {
    if (this.tabInfo == null) return 0;
    String key = TAB_PROP_PREFIX + index + ".timeout";
    String value = (String)this.tabInfo.get(key);
    if (value == null) return 0;
    int result = 0;
    try
    {
      result = Integer.parseInt(value);
    }
    catch (Exception e)
    {
      result = 0;
    }
    return result;
  }

  public void setMaxRows(int index, int numRows)
  {
    String key = TAB_PROP_PREFIX + index + ".maxrows";
    this.tabInfo.setProperty(key, Integer.toString(numRows));
  }

  public int getMaxRows(int tabIndex)
  {
    if (this.tabInfo == null) return 0;
    String key = TAB_PROP_PREFIX + tabIndex + ".maxrows";
    String value = (String)this.tabInfo.get(key);
    if (value == null) return 0;
    int result = 0;
    try
    {
      result = Integer.parseInt(value);
    }
    catch (Exception e)
    {
      result = 0;
    }
    return result;
  }

  public String getExternalFileName(int tabIndex)
  {
    if (this.tabInfo == null) return null;
    String key = TAB_PROP_PREFIX + tabIndex + FILENAME_PROP;
    String value = (String)this.tabInfo.get(key);
    return StringUtil.decodeUnicode(value);
  }

  public String getExternalFileEncoding(int tabIndex)
  {
    if (this.tabInfo == null) return null;
    String key = TAB_PROP_PREFIX + tabIndex + ENCODING_PROP;
    String value = (String)this.tabInfo.get(key);
    if (StringUtil.isEmptyString(value)) return Settings.getInstance().getDefaultEncoding();
    return value;
  }

  public void setExternalFileCursorPos(int tabIndex, int cursor)
  {
    String key = TAB_PROP_PREFIX + tabIndex + CURSOR_POS_PROP;
    this.tabInfo.setProperty(key, Integer.toString(cursor));
  }

  public void setExternalFileName(int tabIndex, String filename)
  {
    String key = TAB_PROP_PREFIX + tabIndex + FILENAME_PROP;
    String encoded = StringUtil.escapeText(filename, CharacterRange.RANGE_7BIT);
    this.tabInfo.setProperty(key, encoded);
  }

  public void setExternalFileEncoding(int tabIndex, String encoding)
  {
    if (encoding == null) return;
    String key = TAB_PROP_PREFIX + tabIndex + ENCODING_PROP;
    this.tabInfo.setProperty(key, encoding);
  }

  @Override
  public String toString()
  {
    return filename;
  }

}
