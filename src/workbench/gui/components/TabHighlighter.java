/*
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
import java.awt.Graphics;

/**
 *
 * @author Thomas Kellerer
 */
public class TabHighlighter
{
  private final Color selectedHighlight;
  private final int thickness;
  private final GuiPosition location;

  public TabHighlighter(Color highlight, int thickness, GuiPosition pos)
  {
    this.selectedHighlight = highlight;
    this.thickness = thickness;
    this.location = pos;
  }

  public void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected)
  {
    if (isSelected && selectedHighlight != null)
    {
      int thick = getThickness(h);
      g.setColor(selectedHighlight);
      switch (location)
      {
        case top:
          g.fillRect(x, y, w - 1, thick);
          break;
        case left:
          g.fillRect(x, y, thick, y + h);
          break;
        case right:
          g.fillRect(x + (w - thick), y, thick, h);
          break;
        case bottom:
          g.fillRect(x, y + h - thick, w - 1, thick);
          break;
      }
    }
  }

  private int getThickness(int height)
  {
    if (this.thickness > 0)
    {
      return this.thickness;
    }
    return (int)(height * 0.1);
  }
}
