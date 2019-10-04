/*
 * BlobDecoder.java
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

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import workbench.db.exporter.BlobMode;

import workbench.storage.BlobLiteralType;

import static workbench.db.exporter.BlobMode.*;

/**
 *
 * @author Thomas Kellerer
 */
public class BlobDecoder
{
  private File baseDir;
  private static final Pattern NON_HEX = Pattern.compile("[^0-9a-f]", Pattern.CASE_INSENSITIVE);

  public BlobDecoder()
  {
  }

  public void setBaseDir(File dir)
  {
    baseDir = dir;
  }

  public Object decodeBlob(String value, BlobMode mode)
    throws IOException
  {
    if (StringUtil.isEmptyString(value)) return null;

    switch (mode)
    {
      case SaveToFile:
        File bfile = new File(value.trim());
        if (!bfile.isAbsolute() && baseDir != null)
        {
          bfile = new File(value.trim());
        }
        return bfile;

      case Base64:
        return decodeBase64(value);

      case AnsiLiteral:
        return decodeHex(value);

      case UUID:
        return decodeUUID(value);
    }
    return value;
  }

  public byte[] decodeString(String value, BlobLiteralType type)
    throws IOException
  {
    if (StringUtil.isEmptyString(value)) return null;
    
    if (type != null)
    {
      switch (type)
      {
        case base64:
          return decodeBase64(value);
        case octal:
          return decodeOctal(value);
        case hex:
          return decodeHex(value);
        case uuid:
          return decodeUUID(value);
        default:
          break;
      }
    }
    throw new IllegalArgumentException("BlobLiteralType " + type + " not supported");
  }

  private byte[] decodeUUID(String value)
  {
    if (value == null || value.isEmpty()) return null;

    String hexValue = NON_HEX.matcher(value).replaceAll("");
    if (hexValue.isEmpty()) return null;

    if (hexValue.length() != 32)
    {
      throw new IllegalArgumentException("'" + value + "' is not a valid UUID string");
    }
    return plainHexToByte(hexValue);
  }

  private byte[] decodeOctal(String value)
    throws IOException
  {
    byte[] result = new byte[value.length() / 4];
    for (int i = 0; i < result.length; i++)
    {
      String digit = value.substring((i*4)+1, (i*4)+ 4);
      byte b = (byte)Integer.parseInt(digit, 8);
      result[i] = b;
    }
    return result;
  }

  private byte[] decodeHex(String value)
  {
    int offset = 0;
    int len = value.length();
    if (value.startsWith("0x") || value.startsWith("0X"))
    {
      offset = 2;
    }
    else if (value.toLowerCase().startsWith("x'"))
    {
      // ANSI BLOB literal X'ff...00'
      offset = 2;
      len --;
    }

    byte[] result = new byte[(len - offset) / 2];

    // I am not re-using plainHexToByte to avoid a substring() call here
    for (int i = 0; i < result.length; i++)
    {
      int pos = (i*2)+offset;
      result[i] = (byte) ((Character.digit(value.charAt(pos), 16) << 4) + Character.digit(value.charAt(pos+1), 16));
    }
    return result;
  }

  private byte[] plainHexToByte(String hexValue)
  {
    int len = hexValue.length();
    byte[] result = new byte[len / 2];
    for (int i = 0; i < len; i += 2)
    {
      result[i/2] = (byte)((Character.digit(hexValue.charAt(i), 16) << 4) + Character.digit(hexValue.charAt(i + 1), 16));
    }
    return result;
  }

  private byte[] decodeBase64(String value)
  {
    java.util.Base64.Decoder decoder = java.util.Base64.getDecoder();
    return decoder.decode(value);
  }

}
