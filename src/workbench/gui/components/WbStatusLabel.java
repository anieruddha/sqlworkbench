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
package workbench.gui.components;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.JLabel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import workbench.interfaces.StatusBar;
import workbench.resource.IconMgr;

import workbench.gui.WbSwingUtilities;

import workbench.storage.RowActionMonitor;

/**
 *
 * @author Thomas Kellerer
 */
public class WbStatusLabel
  extends JLabel
  implements StatusBar
{
  private static final Border DEFAULT_BORDER = new EmptyBorder(1, 1, 0, 1);

  public WbStatusLabel(String text)
  {
    super(text);
    setBorder(DEFAULT_BORDER);
    initSize();
  }

  public WbStatusLabel()
  {
    this(DEFAULT_BORDER);
  }

  public WbStatusLabel(Border border)
  {
    super();
    setBorder(border);
    initSize();
  }

  @Override
  public void setFont(Font f)
  {
    super.setFont(f);
    initSize();
  }

  private void initSize()
  {
    Font f = getFont();
    FontMetrics fm = null;
    if (f != null) fm = getFontMetrics(f);

    int height;
    int width;
    int borderHeight = 6;

    if (fm != null)
    {
      height = (int)(fm.getHeight() * 1.2) + borderHeight;
      width = fm.charWidth('W');
      height = Math.max(22, height);
      width = width * 10;
    }
    else
    {
      int size = (int)(IconMgr.getInstance().getSizeForLabel() * 1.2) + borderHeight;
      width = size;
      height = size;
    }
    Dimension d = new Dimension(width, height);
    setMinimumSize(d);
    setPreferredSize(d);
  }

  public RowActionMonitor getMonitor()
  {
    return new GenericRowMonitor(this);
  }

  @Override
  public void setStatusMessage(String message, int duration)
  {
    setStatusMessage(message);
  }

  @Override
  public void doRepaint()
  {
    repaint();
  }

  @Override
  public void setStatusMessage(final String message)
  {
    WbSwingUtilities.invoke(() ->
    {
      setText(message);
    });
  }

  @Override
  public void clearStatusMessage()
  {
    WbSwingUtilities.invoke(() ->
    {
      setText("");
    });
  }

}
