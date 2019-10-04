/*
 * ElementInfo.java
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
package workbench.util;

/**
 *
 * @author Thomas Kellerer
 */
public class ElementInfo
{
  private String elementValue;
  private int startInStatement;
  private int endInStatement;

  public ElementInfo(String value, int startPos, int endPos)
  {
    this.elementValue = StringUtil.isBlank(value) ? null : value;
    this.startInStatement = startPos;
    this.endInStatement = endPos;
  }

  public String getElementValue()
  {
    return elementValue;
  }

  public int getEndPosition()
  {
    return endInStatement;
  }

  public int getStartPosition()
  {
    return startInStatement;
  }

  public void setOffset(int offset)
  {
    startInStatement += offset;
    endInStatement += offset;
  }

}
