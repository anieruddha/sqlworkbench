/*
 * WbSplitPaneUI.java
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

import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;


/**
 * WB's own SplitPaneUI in order to be able to control the Divider
 *
 * @author Thomas Kellerer
 */
public class WbSplitPaneUI
	extends BasicSplitPaneUI
{

	@Override
	public BasicSplitPaneDivider createDefaultDivider()
	{
		return new WbSplitPaneDivider(this);
	}

	public void setOneTouchTooltip(String tip)
	{
		if (divider instanceof WbSplitPaneDivider)
		{
			((WbSplitPaneDivider)divider).setOneTouchTooltip(tip);
		}
	}
}

