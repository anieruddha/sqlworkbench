/*
 * FontZoomer.java
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
package workbench.gui.fontzoom;

import java.awt.Font;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JComponent;

import workbench.resource.GuiSettings;

import workbench.gui.actions.WbAction;

/**
 *
 * @author Thomas Kellerer
 */
public class FontZoomer
	implements MouseWheelListener
{
	private JComponent client;
	private Font originalFont;
  private final double increasePercent;

	public FontZoomer(JComponent toZoom)
	{
		client = toZoom;
    increasePercent = GuiSettings.getFontZoomPercentage();
	}

	public void resetFontZoom()
	{
		if (originalFont != null)
		{
			client.setFont(originalFont);
		}
		originalFont = null;
	}

	public void increaseFontSize()
	{
		applyFontScale( 1d + (increasePercent / 100));
	}

	public void decreaseFontSize()
	{
		applyFontScale(1d - (increasePercent / 100));
	}

	private void applyFontScale(double scale)
	{
		Font f = client.getFont();
		if (f == null)
		{
			return;
		}

		if (originalFont == null)
		{
			originalFont = f;
		}
		Font newFont = f.deriveFont((float) (f.getSize() * scale));
		client.setFont(newFont);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL && WbAction.isCtrlPressed(e.getModifiers()))
		{
			if (e.getWheelRotation() > 0)
			{
				decreaseFontSize();
			}
			else
			{
				increaseFontSize();
			}
		}
	}
}
