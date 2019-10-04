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
package workbench.gui.sql;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import workbench.gui.MainWindow;
import workbench.gui.PanelReloader;
import workbench.gui.components.RowHeightOptimizer;
import workbench.gui.components.WbMenu;
import workbench.gui.components.WbTable;
import workbench.gui.macros.MacroMenuBuilder;

import workbench.storage.DataStore;

import workbench.sql.KeepResultAnnotation;
import workbench.sql.MacroAnnotation;
import workbench.sql.OptimizeRowHeightAnnotation;
import workbench.sql.RefreshAnnotation;
import workbench.sql.ScrollAnnotation;
import workbench.sql.WbAnnotation;
import workbench.sql.macros.MacroManager;
import workbench.sql.macros.MacroStorage;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class TableAnnotationProcessor
{
	public void handleAnnotations(PanelReloader reloader, DwPanel panel, AutomaticRefreshMgr refreshMgr)
	{
    if (panel == null) return;
    WbTable tbl = panel.getTable();
    if (tbl == null) return;

		DataStore ds = tbl.getDataStore();
		if (ds == null) return;

		String sql = ds.getGeneratingSql();
		if (StringUtil.isEmptyString(sql)) return;

		List<WbAnnotation> annotations = WbAnnotation.readAllAnnotations(sql,
                                                  new ScrollAnnotation(), new MacroAnnotation(),
                                                  new RefreshAnnotation(), new OptimizeRowHeightAnnotation(),
                                                  new KeepResultAnnotation());

		List<MacroAnnotation> macros = new ArrayList<>();

    boolean optimizeRowHeight = false;
    int rowHeightLines = -1;
		boolean scrollToEnd = false;
		int line = -1;

    MainWindow main = (MainWindow) SwingUtilities.getWindowAncestor(tbl);

    MacroStorage macroMgr = MacroManager.getInstance().getMacros(main.getMacroClientId());

		for (WbAnnotation annotation : annotations)
		{
			if (annotation.is(ScrollAnnotation.ANNOTATION))
			{
				String scrollValue = annotation.getValue();
				if (scrollValue != null)
				{
					scrollToEnd = ScrollAnnotation.scrollToEnd(scrollValue);
					line = ScrollAnnotation.scrollToLine(scrollValue);
				}
			}
      else if (annotation.is(OptimizeRowHeightAnnotation.ANNOTATION))
      {
        optimizeRowHeight = true;
        rowHeightLines = ((OptimizeRowHeightAnnotation)annotation).getMaxLines();
      }
      else if (refreshMgr != null && annotation.is(RefreshAnnotation.ANNOTATION))
      {
        String interval = annotation.getValue();
        int milliSeconds = AutomaticRefreshMgr.parseInterval(interval);
        refreshMgr.addRefresh(reloader, panel, milliSeconds);
      }
      else if (annotation.is(MacroAnnotation.ANNOTATION))
			{
				MacroAnnotation macro = (MacroAnnotation)annotation;
        String macroName = macro.getMacroName();
        if (macroName != null && macroMgr.getMacro(macroName) != null)
        {
          macros.add(macro);
        }
			}
      else if (annotation.is(KeepResultAnnotation.ANNOTATION))
      {
        panel.setLocked(true);
      }
		}

		if (macros.size() > 0)
		{
			try
			{
				MacroMenuBuilder builder = new MacroMenuBuilder();
				WbMenu menu = builder.buildDataMacroMenu(main, tbl, macros);
				tbl.addMacroMenu(menu);
			}
			catch (Exception ex)
			{
				// ignore
			}
		}

    if (optimizeRowHeight)
    {
      RowHeightOptimizer optimizer = new RowHeightOptimizer(tbl);
      optimizer.optimizeAllRows(rowHeightLines);
    }

		if (scrollToEnd)
		{
			tbl.scrollToRow(tbl.getRowCount() - 1);
		}
		else if (line > 0)
		{
			tbl.scrollToRow(line - 1);
		}
	}
}
