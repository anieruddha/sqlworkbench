/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2019, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
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
package workbench.gui.bookmarks;

import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import workbench.interfaces.MainPanel;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;

import workbench.gui.MainWindow;

import workbench.storage.DataStore;

import workbench.util.NumberStringCache;
import workbench.util.WbThread;

import static java.util.stream.Collectors.*;

/**
 *
 * @author Thomas Kellerer
 */
public class BookmarkManager
{
  // Maps the ID of a MainWindow to bookmarks defined for each tab.
  // each BookmarkGroup represents the bookmarks from a single editor tab
  private final Map<String, Map<String, BookmarkGroup>> bookmarks = new HashMap<>();

  private BookmarkManager()
  {
  }

  public static BookmarkManager getInstance()
  {
    return InstanceHolder.INSTANCE;
  }

  private static class InstanceHolder
  {
    private static final BookmarkManager INSTANCE = new BookmarkManager();
  }

  public synchronized void clearBookmarksForWindow(String windowId)
  {
    bookmarks.remove(windowId);
    LogMgr.logDebug("BookmarkManager.clearBookmarksForWindow()", "Removed all bookmarks for window: " + windowId);
  }

  public synchronized void clearBookmarksForPanel(String windowId, String panelId)
  {
    Map<String, BookmarkGroup> windowBookmarks = bookmarks.get(windowId);
    if (windowBookmarks != null)
    {
      windowBookmarks.remove(panelId);
    }
  }

  public void updateBookmarks(MainWindow win)
  {
    long start = System.currentTimeMillis();
    int count = -1;
    synchronized (this)
    {
      count = win.getTabCount();
      for (int i = 0; i < count; i++)
      {
        win.getPanel(i).filter(MainPanel::supportsBookmarks).ifPresent(panel -> updateBookmarks(win, panel));
      }
    }
    long end = System.currentTimeMillis();
    LogMgr.logDebug("BookmarkManager.updateBookmarks()", "Parsing bookmarks for " + count + " tabs took: " + (end - start) + "ms");
  }

  private BookmarkGroup updateBookmarks(MainWindow win, MainPanel panel)
  {
    BookmarkGroup updated = null;

    synchronized (this)
    {
      Map<String, BookmarkGroup> windowBookmarks = bookmarks.computeIfAbsent(win.getWindowId(), key -> new HashMap<>());

      final BookmarkGroup group = windowBookmarks.get(panel.getId());

      long modified = 0;

      if (group != null)
      {
        modified = group.creationTime();
      }

      if (group == null || panel.isModifiedAfter(modified))
      {
        List<NamedScriptLocation> panelBookmarks = panel.getBookmarks();
        // if getBoomarks() returns null, the panel does not support bookmarks
        // (this is essentially only the DbExplorerPanel)
        if (panelBookmarks != null)
        {
          updated = new BookmarkGroup(panelBookmarks, panel.getId());
          String title = panel.getTabTitle();
          if (GuiSettings.getShowTabIndex() && title.equals(ResourceMgr.getDefaultTabLabel()))
          {
            int index = win.getIndexForPanel(Optional.ofNullable(panel));
            title += " " + NumberStringCache.getNumberString(index + 1);
          }
          updated.setName(title);
          windowBookmarks.put(updated.getGroupId(), updated);
        }
      }
    }
    return updated;
  }

  private Optional<Map<String, BookmarkGroup>> getBookMarkGroup(MainWindow window)
  {
     return Optional.ofNullable(window)
        .map(MainWindow::getWindowId)
        .map(bookmarks::get);
  }

  public synchronized List<String> getTabs(MainWindow window)
  {
     return getBookMarkGroup(window)
            .map(Map::entrySet)
            .map(Set::stream)
            .map(s -> s.filter(e -> !e.getValue().getBookmarks().isEmpty())
                       .map(Entry::getKey)
                       .collect(toList()))
            .orElse(Collections.emptyList());
  }

  public DataStore getAllBookmarks(MainWindow window)
  {
    return getBookmarks(window, null);
  }

  public DataStore getBookmarksForTab(MainWindow window, String tabId)
  {
    return getBookmarks(window, tabId);
  }

  private synchronized DataStore getBookmarks(MainWindow window, String tabId)
  {
    DataStore result = createDataStore();

    final Optional<Map<String, BookmarkGroup>> bgOpt = getBookMarkGroup(window);

    if (!bgOpt.isPresent()) return result;

    for (BookmarkGroup group : bgOpt.get().values())
    {
      if (tabId != null && !tabId.equals(group.getGroupId())) continue;

      for (NamedScriptLocation loc : group.getBookmarks())
      {
        final int row = result.addRow();
        result.setValue(row, 0, loc.getName());
        result.setValue(row, 1, group.getName());
        result.setValue(row, 2, loc.getLineNumber());
        result.getRow(row).setUserObject(loc);
      }
    }

    return result;
  }

  private DataStore createDataStore()
  {
    String[] columns = new String[]
    {
      ResourceMgr.getPlainString("LblBookName"), ResourceMgr.getPlainString("LblBookPanel"), ResourceMgr.getPlainString("LblBookLine")
    };
    int[] types = new int[]
    {
      Types.VARCHAR, Types.VARCHAR, Types.INTEGER
    };

    DataStore ds = new DataStore(columns, types);
    return ds;
  }

  /**
   * Updates the list of bookmarks for the passed panel.
   * <p>
   * If the text in the panel hasn't changed since the bookmarks were parsed the last time, no parsing will take place.
   * <p>
   * To force a re-parsing of the panel's bookmarks use the <tt>clearList</tt> parameter.
   *
   * @param win       the window to which the panel belongs
   * @param panel     the panel
   * @param clearList if true any existing bookmarks will be deleted before updating the panel's bookmark
   *
   * @see #updateInBackground(workbench.gui.MainWindow, workbench.interfaces.MainPanel, boolean)
   */
  public void updateInBackground(final MainWindow win, final MainPanel panel, boolean clearList)
  {
    if (win == null) return;
    if (panel == null) return;
    if (!panel.supportsBookmarks()) return;

    if (clearList)
    {
      clearBookmarksForPanel(win.getWindowId(), panel.getId());
    }

    new WbThread(() -> {
       Instant start = Instant.now();
       BookmarkGroup updated = updateBookmarks(win, panel);
       Duration duration = Duration.between(start, Instant.now());

       if (updated != null)
       {
          LogMgr.logDebug("BookmarkManager.updateInBackground()",
             "Panel '" + panel.getTabTitle() + "' was updated in " + duration + " (" + updated.getBookmarks().size() + " bookmarks)");
       }
    }, "Update bookmarks for " + panel.getId()).start();
  }

  /**
   * Updates all bookmarks that are "dirty" in the background.
   *
   * @param win the MainWindow for which the bookmarks should be updated.
   */
  public void updateInBackground(final MainWindow win)
  {
    new WbThread(() -> updateBookmarks(win), "Update bookmarks for all tabs").start();
  }

  public void reset()
  {
    bookmarks.clear();
  }
}
