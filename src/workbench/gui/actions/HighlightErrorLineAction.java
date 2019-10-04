/*
 * HighlightCurrentStatement.java
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
package workbench.gui.actions;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 * Toggle highlighting of the the error line (or statement).
 *
 * @author Thomas Kellerer
 */
public class HighlightErrorLineAction
  extends CheckBoxAction
  implements PropertyChangeListener
{

  public HighlightErrorLineAction()
  {
    super("LblHiliteErr", GuiSettings.PROPERTY_HILITE_ERROR_LINE);
    this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
    Settings.getInstance().addPropertyChangeListener(this, GuiSettings.PROPERTY_HILITE_ERROR_LINE);
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt)
  {
    this.setSwitchedOn(Settings.getInstance().getBoolProperty(GuiSettings.PROPERTY_HILITE_ERROR_LINE, false));
  }

  @Override
  public boolean useInToolbar()
  {
    return false;
  }
}
