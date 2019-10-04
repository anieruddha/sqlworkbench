/*
 * CompletionListRenderer.java
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

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import workbench.resource.GuiSettings;

import workbench.db.ColumnIdentifier;
import workbench.db.DbObject;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A ListCellRenderer for the completion popup.
 *
 * If a ColumnIdentifier is displayed, and that is a PK column, the name will be displayed in bold face.
 *
 * For instances of DbObject, the object's comment (remark) is shown as a tooltip.
 *
 * @author Thomas Kellerer
 */
public class CompletionListRenderer
	extends DefaultListCellRenderer
{
	private boolean showNotNulls;
  private boolean showColumnDataTypes = true;

  public CompletionListRenderer()
  {
    this.showColumnDataTypes = GuiSettings.showColumnDataTypesInCompletion();
  }

	public void setShowNotNulls(boolean flag)
	{
		this.showNotNulls = flag;
	}

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
		Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		if (value instanceof ColumnIdentifier)
		{
			ColumnIdentifier col = (ColumnIdentifier)value;
			String colname = SqlUtil.removeObjectQuotes(col.getColumnName());

			if (col.isPkColumn())
			{
				colname = "<b>" + colname + "</b>";
			}
			else if (showNotNulls && !col.isNullable())
			{
				colname = "<span style='color:red'>" + colname + "</span>";
			}

      if (showColumnDataTypes && col.getDbmsType() != null)
      {
        colname = colname + " - <tt>" + col.getDbmsType() + "</tt>";
      }
			setText("<html>" + colname + "</html>");
		}

    if (value instanceof DbObject)
		{
			DbObject dbo = (DbObject)value;
			String type = null;

			if (dbo instanceof ColumnIdentifier)
			{
				ColumnIdentifier col = ((ColumnIdentifier)dbo);
				type = col.getDbmsType();
				if (!col.isNullable())
				{
					type += " (NN)";
				}
			}
			else
			{
				type = dbo.getObjectType();
			}
			String tooltip = null;
			String comment = dbo.getComment();

			if (StringUtil.isBlank(comment) && StringUtil.isNonBlank(type))
			{
				tooltip = "<html><tt>" + type + "</tt></html>";
			}
			else if (StringUtil.isNonBlank(type))
			{
				tooltip = "<html><tt>" + type + "</tt><br><i>" + comment + "</i></html>";
			}
			setToolTipText(tooltip);
		}
		else if (value instanceof TooltipElement)
		{
			setToolTipText(((TooltipElement)value).getTooltip());
		}
		else
		{
			setToolTipText(null);
		}
		return c;
	}

}
