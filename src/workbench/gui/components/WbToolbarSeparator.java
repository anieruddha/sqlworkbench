/*
 * WbToolbarSeparator.java
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

import java.awt.Dimension;

import javax.swing.JPanel;

import workbench.resource.IconMgr;


/**
 *
 * @author  Thomas Kellerer
 */
public class WbToolbarSeparator
	extends JPanel
{

	public WbToolbarSeparator()
	{
		super();
    int size= IconMgr.getInstance().getToolbarIconSize();
		Dimension d = new Dimension((int)(size * 0.3), (int)(size * 1.25));

		setOpaque(false);
		this.setPreferredSize(d);
		this.setPreferredSize(d);
		this.setMaximumSize(d);
		this.setBorder(new DividerBorder(DividerBorder.VERTICAL_MIDDLE));
	}

}
