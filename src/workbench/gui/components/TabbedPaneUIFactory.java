/*
 * TabbedPaneUIFactory.java
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

import java.awt.Insets;

import javax.swing.JTabbedPane;
import javax.swing.LookAndFeel;
import workbench.gui.WbUIManager;
import javax.swing.plaf.TabbedPaneUI;

import workbench.gui.WbUIManager;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;

/**
 *
 * @author  Thomas Kellerer
 */
public class TabbedPaneUIFactory
{
	private static final Insets TOP_INSETS = new Insets(2,1,1,1);
	private static final Insets BOTTOM_INSETS = new Insets(1,1,2,1);
	private static final Insets LEFT_INSETS = new Insets(1,3,1,1);
	private static final Insets RIGHT_INSETS = new Insets(1,1,1,3);

	static Insets getBorderLessInsets(int tabPlacement)
	{
		switch (tabPlacement)
		{
			case JTabbedPane.TOP:
				return (Insets)TOP_INSETS.clone();
			case JTabbedPane.BOTTOM:
				return (Insets)BOTTOM_INSETS.clone();
			case JTabbedPane.LEFT:
				return (Insets)LEFT_INSETS.clone();
			case JTabbedPane.RIGHT:
				return (Insets)RIGHT_INSETS.clone();
			default:
        return WbSwingUtilities.getEmptyInsets();
		}
	}

	public static String getTabbedPaneUIClass()
	{
		if (!Settings.getInstance().getBoolProperty("workbench.gui.replacetabbedpane", true))
		{
			return null;
		}

		LookAndFeel lnf = WbUIManager.getLookAndFeel();
		String lnfClass = lnf.getClass().getName();

		if (lnfClass.equals("javax.swing.plaf.metal.MetalLookAndFeel"))
		{
			return "workbench.gui.components.BorderLessMetalTabbedPaneUI";
		}
		else if (lnfClass.startsWith("com.sun.java.swing.plaf.windows.Windows"))
		{
			return "workbench.gui.components.BorderLessWindowsTabbedPaneUI";
		}
		else if (lnfClass.equals("com.sun.java.swing.plaf.motif.MotifLookAndFeel"))
		{
			return "workbench.gui.components.BorderLessMotifTabbedPaneUI";
		}
    else if (lnfClass.contains("WebLookAndFeel"))
    {
      return "com.alee.laf.tabbedpane.WebTabbedPaneUI";
    }
		return null;
	}

	public static TabbedPaneUI getBorderLessUI()
	{
		String uiClass = getTabbedPaneUIClass();
		if (uiClass == null) return null;
		return getClassInstance(uiClass);
	}

	private static TabbedPaneUI getClassInstance(String className)
	{
		TabbedPaneUI ui = null;
		try
		{
			Class cls = Class.forName(className);
			ui = (TabbedPaneUI)cls.newInstance();
		}
		catch (Exception e)
		{
			JTabbedPane pane = new JTabbedPane();
			ui = (TabbedPaneUI)WbUIManager.getUI(pane);
			WbUIManager.getDefaults().put("TabbedPaneUI", ui.getClass().getName());
		}
		return ui;
	}
}
