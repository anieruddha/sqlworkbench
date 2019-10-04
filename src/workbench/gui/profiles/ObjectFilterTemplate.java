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
package workbench.gui.profiles;

import java.util.Objects;

import workbench.db.ObjectNameFilter;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ObjectFilterTemplate
{
  private String name;
  private ObjectNameFilter filter;

  public ObjectFilterTemplate(String templateName, ObjectNameFilter templateFilter)
  {
    this.name = templateName;
    this.filter = templateFilter;
  }

  public String getName()
  {
    return name;
  }

  public ObjectNameFilter getFilter()
  {
    return filter;
  }

  @Override
  public String toString()
  {
    return name;
  }

  @Override
  public int hashCode()
  {
    int hash = 3;
    hash = 67 * hash + Objects.hashCode(this.name);
    return hash;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final ObjectFilterTemplate other = (ObjectFilterTemplate)obj;
    return StringUtil.equalStringIgnoreCase(this.name, other.name);
  }

}
