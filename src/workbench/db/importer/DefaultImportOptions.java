/*
 * DefaultImportOptions.java
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
package workbench.db.importer;

import workbench.resource.Settings;

/**
 * @author Thomas Kellerer
 */
public class DefaultImportOptions
  implements ImportOptions
{

  @Override
  public String getEncoding()
  {
    return "UTF-8";
  }

  @Override
  public String getDateFormat()
  {
    return Settings.getInstance().getDefaultDateFormat();
  }

  @Override
  public String getTimestampFormat()
  {
    return Settings.getInstance().getDefaultTimestampFormat();
  }

  @Override
  public void setEncoding(String enc)
  {
  }

  @Override
  public void setDateFormat(String format)
  {
  }

  @Override
  public void setTimestampFormat(String format)
  {
  }

  @Override
  public void setMode(String mode)
  {
  }

  @Override
  public String getMode()
  {
    return "insert";
  }
}
