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
package workbench.sql;

import workbench.resource.GuiSettings;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OptimizeRowHeightAnnotation
	extends WbAnnotation
{
  public static final String MAX_LINES = "lines";
	public static final String ANNOTATION = "WbOptimizeRowHeight";

	public OptimizeRowHeightAnnotation()
	{
		super(ANNOTATION);
	}

  public int getMaxLines()
  {
    String val = getValue();
    int lines = GuiSettings.getAutRowHeightMaxLines();
    if (StringUtil.isNonBlank(val))
    {
      String[] elements = val.split("=");
      if (elements != null && elements.length == 2)
      {
        lines = StringUtil.getIntValue(elements[1], lines);
      }
    }
    return lines;
  }

  @Override
  public boolean requiresValue()
  {
    return false;
  }

}
