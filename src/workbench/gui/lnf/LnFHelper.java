/*
 * LnFHelper.java
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
package workbench.gui.lnf;

import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.lang.reflect.Method;
import java.util.Set;

import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import workbench.gui.WbUIManager;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.Settings;

import workbench.gui.components.TabbedPaneUIFactory;

import workbench.util.CollectionUtil;
import workbench.util.PlatformHelper;
import workbench.util.StringUtil;


/**
 * Initialize some gui elements during startup.
 *
 * @author Thomas Kellerer
 */
public class LnFHelper
{
	public static final String MENU_FONT_KEY = "MenuItem.font";
	public static final String LABEL_FONT_KEY = "Label.font";
	public static final String TREE_FONT_KEY = "Tree.font";

	private boolean isWindowsClassic;

	// Font properties that are automatically scaled by Java
	private final Set<String> noScale = CollectionUtil.treeSet(
		"Menu.font",
		"MenuBar.font",
		"MenuItem.font",
		"PopupMenu.font",
		"CheckBoxMenuItem.font");

	private final Set<String> fontProperties = CollectionUtil.treeSet(
		"Button.font",
		"CheckBox.font",
		"CheckBoxMenuItem.font",
		"ColorChooser.font",
		"ComboBox.font",
		"EditorPane.font",
		"FileChooser.font",
		LABEL_FONT_KEY,
		"List.font",
		"Menu.font",
		"MenuBar.font",
		MENU_FONT_KEY,
		"OptionPane.font",
		"Panel.font",
		"PasswordField.font",
		"PopupMenu.font",
		"ProgressBar.font",
		"RadioButton.font",
		"RadioButtonMenuItem.font",
		"ScrollPane.font",
		"Slider.font",
		"Spinner.font",
		"TabbedPane.font",
		"TextArea.font",
		"TextField.font",
		"TextPane.font",
		"TitledBorder.font",
		"ToggleButton.font",
		"ToolBar.font",
		"ToolTip.font",
		TREE_FONT_KEY,
		"ViewPort.font");

	public boolean isWindowsClassic()
	{
		return isWindowsClassic;
	}

	public static int getMenuFontHeight()
	{
		return getFontHeight(MENU_FONT_KEY);
	}

	public static int getLabelFontHeight()
	{
		return getFontHeight(LABEL_FONT_KEY);
	}

	private static int getFontHeight(String key)
	{
		UIDefaults def = WbUIManager.getDefaults();
		double factor = Toolkit.getDefaultToolkit().getScreenResolution() / 72.0;
		Font font = def.getFont(key);
		if (font == null) return 18;
		return (int)Math.ceil((double)font.getSize() * factor);
	}

	public void initUI()
	{
		initializeLookAndFeel();

		Settings settings = Settings.getInstance();
		UIDefaults def = WbUIManager.getDefaults();

		Font stdFont = settings.getStandardFont();
		if (stdFont != null)
		{
			for (String property : fontProperties)
			{
				def.put(property, stdFont);
			}
		}
		else if (isWindowsLookAndFeel())
		{
			// The default Windows look and feel does not scale the fonts properly
			scaleDefaultFonts();
		}

    if (isWebLaf())
    {
      initializeWebLaf();
    }

		Font dataFont = settings.getDataFont();
		if (dataFont != null)
		{
			def.put("Table.font", dataFont);
			def.put("TableHeader.font", dataFont);
		}

		String cls = TabbedPaneUIFactory.getTabbedPaneUIClass();
		if (cls != null) def.put("TabbedPaneUI", cls);

		if (settings.getBoolProperty("workbench.gui.adjustgridcolor", true))
		{
			Color c = settings.getColor("workbench.table.gridcolor", new Color(215,215,215));
			def.put("Table.gridColor", c);
		}

    def.put("Button.showMnemonics", Boolean.valueOf(GuiSettings.getShowMnemonics()));
    WbUIManager.put("Synthetica.extendedFileChooser.rememberLastDirectory", false);
	}

  public static boolean isWebLaf()
  {
		String lnf = WbUIManager.getLookAndFeel().getClass().getName();
		return lnf.contains("WebLookAndFeel");
  }

  public static boolean isGTKLookAndFeel()
  {
		String lnf = WbUIManager.getLookAndFeel().getClass().getName();
		return lnf.contains("GTKLookAndFeel");
  }

	public static boolean isWindowsLookAndFeel()
	{
		String lnf = WbUIManager.getLookAndFeel().getClass().getName();
		return lnf.contains("plaf.windows");
	}

  public static boolean isNonStandardLookAndFeel()
  {
    String lnf = WbUIManager.getLookAndFeel().getClass().getName();
    return (lnf.startsWith("com.sun.java") == false && lnf.startsWith("javax.swing.plaf") == false);
  }

	private void scaleDefaultFonts()
	{
		FontScaler scaler = new FontScaler();
		scaler.logSettings();
		if (!Settings.getInstance().getScaleFonts()) return;

    LogMgr.logInfo(new CallerInfo(){}, "Scaling default fonts by: " + scaler.getScaleFactor());

		UIDefaults def = WbUIManager.getDefaults();

    // when the user configures a scale factor, don't check the menu fonts
    boolean checkJavaFonts = Settings.getInstance().getScaleFactor() < 0;

		for (String property : fontProperties)
		{
      if (checkJavaFonts && noScale.contains(property)) continue;
      Font base = def.getFont(property);
      if (base != null)
      {
        Font scaled = scaler.scaleFont(base);
        def.put(property, scaled);
      }
		}
	}

	public static boolean isJGoodies()
	{
		String lnf = WbUIManager.getLookAndFeel().getClass().getName();
		return lnf.startsWith("com.jgoodies.looks.plastic");
	}

	protected void initializeLookAndFeel()
	{
		String className = GuiSettings.getLookAndFeelClass();
		try
		{
			if (StringUtil.isEmptyString(className))
			{
				className = WbUIManager.getSystemLookAndFeelClassName();
			}
			LnFManager mgr = new LnFManager();
			LnFDefinition def = mgr.findLookAndFeel(className);

			if (def == null)
			{
        LogMgr.logError(new CallerInfo(){}, "Specified Look & Feel " + className + " not available!", null);
				setSystemLnF();
			}
			else
			{
        // Fix for bug: https://bugs.java.com/bugdatabase/view_bug.do?bug_id=8179014
        // under Windows 10 with the "Creators Update"
        if (className.contains(".plaf.windows.") && Settings.getInstance().getBoolProperty("workbench.gui.fix.filechooser.bug", false))
        {
          WbUIManager.put("FileChooser.useSystemExtensionHiding", false);
        }
				WbUIManager.put("FileChooser.useSystemIcons", Boolean.TRUE);

				// I hate the bold menu font in the Metal LnF
				WbUIManager.put("swing.boldMetal", Boolean.FALSE);

				// Remove Synthetica's own window decorations
				WbUIManager.put("Synthetica.window.decoration", Boolean.FALSE);

				// Remove the extra icons for read only text fields and
				// the "search bar" in the main menu for the Substance Look & Feel
				System.setProperty("substancelaf.noExtraElements", "");

        if (className.startsWith("org.jb2011.lnf.beautyeye"))
        {
          WbUIManager.put("RootPane.setupButtonVisible", false);
        }

				LnFLoader loader = new LnFLoader(def);
				LookAndFeel lnf = loader.getLookAndFeel();

				WbUIManager.setLookAndFeel(lnf);
				PlatformHelper.installGtkPopupBugWorkaround();
			}
		}
		catch (Throwable e)
		{
      LogMgr.logError(new CallerInfo(){}, "Could not set look and feel to [" + className + "]. Look and feel will be ignored", e);
			setSystemLnF();
		}

		checkWindowsClassic(WbUIManager.getLookAndFeel().getClass().getName());
	}

  private void initializeWebLaf()
  {
    try
    {
      LookAndFeel lookAndFeel = WbUIManager.getLookAndFeel();
      Method init = lookAndFeel.getClass().getMethod("initializeManagers");
      init.invoke(null, (Object[])null);

      WbUIManager.getDefaults().put("ToolBarUI", "com.alee.laf.toolbar.WebToolBarUI");
      WbUIManager.getDefaults().put("TabbedPaneUI", "com.alee.laf.toolbar.WebTabbedPaneUI");
      WbUIManager.getDefaults().put("SplitPaneUI", "com.alee.laf.splitpane.WebSplitPaneUI");
    }
    catch (Throwable th)
    {
      LogMgr.logWarning(new CallerInfo(){}, "Could not initialize WebLaf", th);
    }
  }

	private void setSystemLnF()
	{
		try
		{
			WbUIManager.setLookAndFeel(WbUIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception ex)
		{
			// should not ahppen
		}
	}

	private void checkWindowsClassic(String clsname)
	{
		try
		{
			if (clsname.contains("com.sun.java.swing.plaf.windows"))
			{
				String osVersion = System.getProperty("os.version", "1.0");
				Float version = Float.valueOf(osVersion);
				if (version <= 5.0)
				{
					isWindowsClassic = true;
				}
				else
				{
					isWindowsClassic = clsname.contains("WindowsClassicLookAndFeel");
					if (!isWindowsClassic)
					{
						Toolkit toolkit = Toolkit.getDefaultToolkit();
						Boolean themeActive = (Boolean) toolkit.getDesktopProperty("win.xpstyle.themeActive");
						if (themeActive != null)
						{
							isWindowsClassic = !themeActive;
						}
						else
						{
							isWindowsClassic = true;
						}
					}
				}
			}
		}
		catch (Throwable e)
		{
			isWindowsClassic = false;
		}

	}

}
