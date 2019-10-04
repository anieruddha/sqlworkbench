/*
 * WbScrollPane.java
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

import java.awt.Color;
import java.awt.Component;

import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;


/**
 *
 * @author  Thomas Kellerer
 */
public class WbScrollPane
	extends JScrollPane
{
	private static boolean useCustomizedBorder = true;

	public WbScrollPane()
	{
		super();
		this.initDefaults();
	}

	public WbScrollPane(Component view)
	{
		super(view);
		this.initDefaults();
	}

	public WbScrollPane(Component view, int vsbPolicy, int hsbPolicy)
	{
		super(view, vsbPolicy, hsbPolicy);
		this.initDefaults();
	}

	public WbScrollPane(int vsbPolicy, int hsbPolicy)
	{
		super(vsbPolicy, hsbPolicy);
		this.initDefaults();
	}

	private void initDefaults()
	{
    setDoubleBuffered(true);
		if (useCustomizedBorder)
		{
			try
			{
        // With some Linux distributions (Debian) creating this border during
        // initialization fails. So if we can't create our own border
        // we simply skip this for the future
        Color cl = UIManager.getColor("Label.background").darker();
				Border myBorder = new LineBorder(cl, 1);
  			this.setBorder(myBorder);
			}
			catch (Throwable e)
			{
				useCustomizedBorder = false;
			}
		}
	}

}
