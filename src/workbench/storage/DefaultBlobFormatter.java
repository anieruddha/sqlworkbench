/*
 * DefaultBlobFormatter.java
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
package workbench.storage;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Base64;

import workbench.util.FileUtil;
import workbench.util.NumberStringCache;
import workbench.util.StringUtil;


/**
 * @author Thomas Kellerer
 */
public class DefaultBlobFormatter
  implements BlobLiteralFormatter
{
  private String prefix;
  private String suffix;
  private boolean upperCase = false;
  private BlobLiteralType literalType = BlobLiteralType.hex;

  public void setLiteralType(BlobLiteralType type)
  {
    this.literalType = (type == null ? BlobLiteralType.hex : type);
  }

  public void setUseUpperCase(boolean flag)
  {
    this.upperCase = flag;
  }

  public void setPrefix(String p)
  {
    this.prefix = p;
  }

  public void setSuffix(String s)
  {
    this.suffix = s;
  }

  @Override
  public String getBlobLiteral(Object value)
    throws SQLException
  {
    if (value == null) return null;

    int addSpace = (prefix != null ? prefix.length() : 0);
    addSpace += (suffix != null ? suffix.length() : 0);

    StringBuilder result = null;

    if (value instanceof byte[])
    {
      byte[] buffer = (byte[])value;
      result = convertBytes(buffer);
    }
    else if (value instanceof Blob)
    {
      Blob b = (Blob)value;
      int len = (int)b.length();
      result = new StringBuilder(len * 2 + addSpace);
      if (prefix != null) result.append(prefix);
      for (int i = 0; i < len; i++)
      {
        byte[] byteBuffer = b.getBytes(i, 1);
        appendArray(result, byteBuffer);
      }
    }
    else if (value instanceof InputStream)
    {
      InputStream in = (InputStream)value;
      try
      {
        byte[] data = FileUtil.readBytes(in);
        result = convertBytes(data);
      }
      catch (IOException io)
      {
        throw new SQLException("Could not read BLOB data", io);
      }
    }
    else
    {
      String s = value.toString();
      result = new StringBuilder(s.length() + addSpace);
      if (prefix != null) result.append(prefix);
      result.append(s);
    }
    if (suffix != null) result.append(suffix);
    return result.toString();
  }

  private StringBuilder convertBytes(byte[] data)
  {
    StringBuilder result = new StringBuilder(data.length * 2);
    if (prefix != null) result.append(prefix);
    appendArray(result, data);
    return result;
  }

  private void appendArray(StringBuilder result, byte[] buffer)
  {
    switch (literalType)
    {
      case base64:
        Base64.Encoder encoder = java.util.Base64.getEncoder();
        result.append(encoder.encodeToString(buffer));
        break;
      case uuid:
        appendUUID(result, buffer);
        break;
      case octal:
        for (int i = 0; i < buffer.length; i++)
        {
          int c = buffer[i] & 0xFF;
          result.append("\\");
          CharSequence s = StringUtil.getOctalString(c);
          if (upperCase)
          {
            result.append(s.toString().toUpperCase());
          }
          else
          {
            result.append(s);
          }
        }
        break;
      default:
        appendHexArray(result, buffer);
    }
  }

  private void appendUUID(StringBuilder result, byte[] buffer)
  {
    char[] hexChars = upperCase ? NumberStringCache.HEX_ARRAY_UPPER : NumberStringCache.HEX_ARRAY_LOWER;

    for (int i = 0; i < buffer.length; i++)
    {
      int v = buffer[i] & 0xFF;
      result.append(hexChars[v >>> 4]);
      result.append(hexChars[v & 0x0F]);
      if (i == 3 || i == 5 || i == 7 || i == 9)
      {
        result.append('-');
      }
    }
  }

  private void appendHexArray(StringBuilder result, byte[] buffer)
  {
    char[] hexChars = upperCase ? NumberStringCache.HEX_ARRAY_UPPER : NumberStringCache.HEX_ARRAY_LOWER;

    for (int j = 0; j < buffer.length; j++)
    {
      int v = buffer[j] & 0xFF;
      result.append(hexChars[v >>> 4]);
      result.append(hexChars[v & 0x0F]);
    }
  }

  @Override
  public BlobLiteralType getType()
  {
    return literalType;
  }

}
