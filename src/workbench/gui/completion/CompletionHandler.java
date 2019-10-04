/*
 * CompletionHandler.java
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
package workbench.gui.completion;
import java.awt.Color;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import workbench.interfaces.StatusBar;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;

import workbench.db.WbConnection;

import workbench.gui.editor.JEditTextArea;

import workbench.sql.parser.ScriptParser;

import workbench.util.CollectionUtil;
import workbench.util.SqlParsingUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 * Handle the auto completion for tables and columns
 * @author  Thomas Kellerer
 */
public class CompletionHandler
	implements ListModel
{
	private JEditTextArea editor;
	protected List elements = Collections.EMPTY_LIST;
	protected List filteredElements;

	protected WbConnection dbConnection;
	private JLabel header;
	private List<ListDataListener> listeners;
	private CompletionPopup window;
	protected StatusBar statusBar;
	private String currentWord;
	private boolean highlightNotNulls;

	public CompletionHandler()
	{
		header = new JLabel(ResourceMgr.getString("LblCompletionListTables"));
		header.setForeground(Color.BLUE);
		header.setBorder(BorderFactory.createEmptyBorder(0,2,0,2));
	}

	public void setStatusBar(StatusBar bar)
	{
		this.statusBar = bar;
	}

	public void setEditor(JEditTextArea ed)
	{
		this.editor = ed;
	}

	public void setConnection(WbConnection conn)
	{
		this.dbConnection = conn;
	}

	protected void showPopup()
	{
		filteredElements = null;
		try
		{
			statusBar.setStatusMessage(ResourceMgr.getString("MsgCompletionRetrievingObjects"));
			if (this.updateSelectionList())
			{
				this.window.showPopup(currentWord, highlightNotNulls);
			}
		}
		catch (Throwable th)
		{
			LogMgr.logError("CompletionHandler.showPopup()", "Error retrieving completion objects", th);
			statusBar.clearStatusMessage();
		}
	}

	public void cancelPopup()
	{
		if (this.window != null) this.window.cancelPopup();
	}

	public void showCompletionPopup()
	{
		if (this.window == null)
		{
			this.window = new CompletionPopup(editor, header, this);
		}

		// if this is not done in a separate thread
		// the status bar will not be updated...
		WbThread t = new WbThread("Completion")
		{
			@Override
			public void run()
			{
				showPopup();
			}
		};
		t.start();
	}

	private boolean updateSelectionList()
	{
		boolean result = false;
		highlightNotNulls = false;
		ScriptParser parser = ScriptParser.createScriptParser(dbConnection);
    parser.setScript(editor.getText());
		int cursorPos = this.editor.getCaretPosition();

		int index = parser.getCommandIndexAtCursorPos(cursorPos);
		int commandCursorPos = parser.getIndexInCommand(index, cursorPos);
		String sql = (index > -1 ? parser.getCommand(index, false) : null);

    if (LogMgr.isDebugEnabled())
    {
      StringBuilder debugString = new StringBuilder(sql == null ? editor.getText() : sql);
      if (commandCursorPos > -1)
      {
        debugString.insert(commandCursorPos, "^|^");
      }
      else
      {
        debugString.append("\nNo command found at cursor position: " + cursorPos + ", commandIndex: " + index + ", cursor index in command: " + commandCursorPos);
      }
      LogMgr.logDebug("CompletionHandler.updateSelectionList()", "Completion invoked for statement:\n" + debugString.toString());
    }

    if (sql == null)
    {
      LogMgr.logWarning("CompletionHandler.updateSelectionList()", "No SQL found!");
      showNoObjectsFoundMessage();
      return false;
    }

		try
		{
      long start = System.currentTimeMillis();
			StatementContext ctx = new StatementContext(this.dbConnection, sql, commandCursorPos);

			if (ctx.isStatementSupported())
			{
				BaseAnalyzer analyzer = ctx.getAnalyzer();
				currentWord = editor.getWordLeftOfCursor(analyzer.getWordDelimiters());

				boolean selectWord = (analyzer.getOverwriteCurrentWord() && StringUtil.isNonBlank(currentWord));
				if (StringUtil.isNonBlank(currentWord) && analyzer.isWbParam() && currentWord.charAt(0) == '-')
				{
					currentWord = currentWord.substring(1);
				}
        window.allowMultiSelection(ctx.getAnalyzer().allowMultiSelection());
				window.selectCurrentWordInEditor(selectWord);

				this.elements = ctx.getData();

        long duration = System.currentTimeMillis() - start;
				LogMgr.logDebug("CompletionHandler.updateSelectionList()",
					"Auto-completion invoked for " + analyzer.getSqlVerb() +
						", analyzer: " + analyzer.getClass().getSimpleName() +
						", context: " + analyzer.contextToString() +
						", currentSchema: " + analyzer.getSchemaForTableList() +
						", element count: " + elements.size() +
            " (" + duration + "ms)");

				this.header.setText(ctx.getTitle());
				this.window.setContext(ctx);

				Set<String> dml = CollectionUtil.caseInsensitiveSet("insert", "update", "merge");
				highlightNotNulls = dml.contains(analyzer.getSqlVerb());

				result = getSize() > 0;
				if (result)
				{
					statusBar.clearStatusMessage();
					fireDataChanged();
				}
				else
				{
					showNoObjectsFoundMessage();
				}
			}
			else
			{
				Toolkit.getDefaultToolkit().beep();
				showFailedMessage(sql);
				result = false;
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("CompletionHandler.updateSelectionList()", "Error retrieving objects", e);
			result = false;
			showNoObjectsFoundMessage();
		}
		return result;
	}

	public void resetFilter()
	{
		filteredElements =null;
		fireDataChanged();
	}

	public synchronized int filterElements(String value)
	{
		if (StringUtil.isBlank(value)) return 0;
		filteredElements = null;
		if (getSize() == 0) return 0;

		try
		{
			boolean partialMatch = GuiSettings.getPartialCompletionSearch();
			List filter = new ArrayList(getSize());
			value = value.toLowerCase();
			for (int i=0; i < getSize(); i++)
			{
				Object o = elements.get(i);
				if (o == null) continue;
				String element = o.toString().toLowerCase();
				if (partialMatch)
				{
					if (element.contains(value)) filter.add(o);
				}
				else
				{
					if (element.startsWith(value)) filter.add(o);
				}
			}
			if (filter.size() > 0)
			{
				filteredElements = filter;
			}
			fireDataChanged();
			return getSize();
		}
		catch (Exception e)
		{
			LogMgr.logError("CompletionHandler.filterElements()", "Error when applying filter", e);
			return -1;
		}
	}

	private synchronized List getElementList()
	{
		if (filteredElements != null)
		{
			return filteredElements;
		}
		return elements == null ? Collections.emptyList() : elements;
	}

	private void showNoObjectsFoundMessage()
	{
		String msg = ResourceMgr.getString("MsgCompletionNothingFound");
		statusBar.setStatusMessage(msg, 2500);
	}

	private void showFailedMessage(String sql)
	{
		String verb = SqlParsingUtil.getInstance(dbConnection).getSqlVerb(sql);
		String msg = "'" + verb + "' " + ResourceMgr.getString("MsgCompletionNotSupported");
		statusBar.setStatusMessage(msg, 2500);
	}

	private void fireDataChanged()
	{
		if (this.listeners == null) return;
		ListDataEvent evt = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, getSize() - 1);
		for (ListDataListener l : this.listeners)
		{
			l.contentsChanged(evt);
		}
	}

	/**
	 * Implementation of the ListModel interface
	 */
	@Override
	public Object getElementAt(int index)
	{
		return getElementList().get(index);
	}

	/**
	 * Implementation of the ListModel interface
	 */
	@Override
	public int getSize()
	{
		return getElementList().size();
	}

	/**
	 * Implementation of the ListModel interface
	 */
	@Override
	public void addListDataListener(ListDataListener listDataListener)
	{
		if (this.listeners == null) this.listeners = new ArrayList<>();
		this.listeners.add(listDataListener);
	}

	/**
	 * Implementation of the ListModel interface
	 */
	@Override
	public void removeListDataListener(ListDataListener listDataListener)
	{
		if (this.listeners == null) return;
		this.listeners.remove(listDataListener);
	}

}
