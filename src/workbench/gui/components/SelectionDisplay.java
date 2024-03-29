/*
 * SelectionDisplay.java
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
package workbench.gui.components;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumnModel;

import workbench.interfaces.TextSelectionListener;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;
import workbench.gui.sql.EditorPanel;

import workbench.util.StringUtil;
import workbench.util.WbNumberFormatter;

/**
 *
 * @author Thomas Kellerer
 */
public class SelectionDisplay
	extends JLabel
  implements TextSelectionListener
{
	private JTable table;
	private Border activeBorder = new CompoundBorder(new DividerBorder(DividerBorder.LEFT), new EmptyBorder(0, 3, 0, 3));
	private ListSelectionListener rowListener;
	private ListSelectionListener columnListener;
	private WbNumberFormatter formatter;

	public SelectionDisplay()
	{
		rowListener = this::rowSelectionChanged;
		columnListener = this::columnSelectionChanged;
		setBorder(WbSwingUtilities.EMPTY_BORDER);
		formatter = Settings.getInstance().createDefaultDecimalFormatter(2);
	}

	public void removeClient(JTable client)
	{
		if (client != null)
		{
			ListSelectionModel rowModel = client.getSelectionModel();
			if (rowModel != null)
			{
				rowModel.removeListSelectionListener(rowListener);
			}
			TableColumnModel col = client.getColumnModel();
			ListSelectionModel colModel = (col != null ? col.getSelectionModel() : null);
			if (colModel != null)
			{
				colModel.removeListSelectionListener(columnListener);
			}
			setText(StringUtil.EMPTY_STRING);
		}
	}

	public void setClient(JTable client)
	{
		removeClient(table);

		table = client;
		if (client != null)
		{
			ListSelectionModel rowModel = client.getSelectionModel();
			if (rowModel != null)
			{
				rowModel.addListSelectionListener(rowListener);
			}
			TableColumnModel col = client.getColumnModel();
			ListSelectionModel colModel = (col != null ? col.getSelectionModel() : null);
			if (colModel != null)
			{
				colModel.addListSelectionListener(columnListener);
			}
		}
	}

  public void setTextClient(EditorPanel editor)
  {
    if (editor != null)
    {
      editor.addSelectionListener(this);
    }
  }

  public void removeTextClient(EditorPanel editor)
  {
    if (editor != null)
    {
      editor.removeSelectionListener(this);
    }
  }

  @Override
  public void selectionChanged(int start, int end)
  {
    int length = end - start;
    if (length > 1)
    {
      this.setText(length + " characters selected");
    }
    else
    {
      setText("");
    }
  }

	protected void columnSelectionChanged(ListSelectionEvent e)
	{
		showSelection();
	}

	protected void rowSelectionChanged(ListSelectionEvent e)
	{
		showSelection();
	}

	@Override
	public void setText(String text)
	{
		super.setText(text);
		if (StringUtil.isEmptyString(text))
		{
			setBorder(WbSwingUtilities.EMPTY_BORDER);
		}
		else
		{
			setBorder(activeBorder);
		}
	}

	protected void showSelection()
	{
		if (table == null)
		{
			setText(StringUtil.EMPTY_STRING);
			return;
		}

		int cols[] = table.getSelectedColumns();
		double sum = 0;
		boolean numbers = false;

		boolean showSum = cols != null && cols.length > 0 && table.getColumnSelectionAllowed();
		if (table.getSelectedRowCount() == 1 && cols.length == 1)
		{
			showSum = false;
		}

		if (showSum)
		{
			int rows[] = table.getSelectedRows();
			for (int i=0; i < rows.length; i++)
			{
				for (int c=0; c < cols.length; c++)
				{
					Object o = table.getValueAt(rows[i], cols[c]);
					if (o instanceof Number)
					{
						sum += ((Number)o).doubleValue();
						numbers = true;
					}
				}
			}
		}

		String display = null;

		if (numbers)
		{
			display = ResourceMgr.getFormattedString("MsgSelectSum", formatter.format(sum));
		}
		else
		{
			int rows = table.getSelectedRowCount();
			if (rows > 0)
			{
				display = ResourceMgr.getFormattedString("MsgRowsSelected", rows);
			}
		}

		if (display == null)
		{
			setText(StringUtil.EMPTY_STRING);
		}
		else
		{
			setText(display);
		}
	}
}
