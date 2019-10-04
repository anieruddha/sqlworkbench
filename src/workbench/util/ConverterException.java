/*
 * ConverterException.java
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

import workbench.resource.ResourceMgr;

/**
 * @author Thomas Kellerer
 */
public class ConverterException
  extends Exception
{

  private String value;
  private int dataType;
  public ConverterException(String msg)
  {
    super(msg);
  }

  public ConverterException(Object input, int type,  Exception cause)
  {
    super("Could not convert [" + input + "] for datatype " + SqlUtil.getTypeName(type), cause);
    this.value = (input == null ? "" : input.toString());
    this.dataType = type;
  }

  @Override
  public String getLocalizedMessage()
  {
    String msg = null;
    if (this.getCause() == null)
    {
      msg = getMessage();
    }
    else
    {
      msg = getCause().getMessage();
    }
    if (msg == null)
    {
      msg = ResourceMgr.getFormattedString("MsgConvertErrorDetails", value, SqlUtil.getTypeName(dataType));
    }
    return msg;
  }
}
