/*
 * HtmlUtil.java
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

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Thomas Kellerer
 */
public class HtmlUtil
{

  public static String escapeXML(CharSequence s)
  {
    return escapeXML(s, true);
  }

  @SuppressWarnings("fallthrough")
  public static String escapeXML(CharSequence s, boolean replaceSingleQuotes)
  {
    if (s == null) return StringUtil.EMPTY_STRING;
    StringBuilder sb = new StringBuilder(s.length() + 100);
    int n = s.length();
    for (int i = 0; i < n; i++)
    {
      char c = s.charAt(i);
      switch (c)
      {
        case '<':
          sb.append("&lt;");
          break;
        case '>':
          sb.append("&gt;");
          break;
        case '&':
          sb.append("&amp;");
          break;
        case '"':
          sb.append("&quot;");
          break;
        case '\'':
          if (replaceSingleQuotes)
          {
            sb.append("&apos;");
            break;
          }
          // single quotes should not be replaced
          // in that case the fall through to the default is intended
        default:
          sb.append(c);
          break;
      }
    }
    return sb.toString();
  }

  public static String escapeHTML(String s)
  {
    if (s == null) return null;
    StringBuilder sb = new StringBuilder(s.length() + 100);
    int n = s.length();
    for (int i = 0; i < n; i++)
    {
      char c = s.charAt(i);
      switch (c)
      {
        case '<': sb.append("&lt;"); break;
        case '>': sb.append("&gt;"); break;
        case '&': sb.append("&amp;"); break;
        case '"': sb.append("&quot;"); break;
        case '\'': sb.append("&apos;"); break;
        case '\u00e0': sb.append("&agrave;");break;
        case '\u00c0': sb.append("&Agrave;");break;
        case '\u00e2': sb.append("&acirc;");break;
        case '\u00c2': sb.append("&Acirc;");break;
        case '\u00e4': sb.append("&auml;");break;
        case '\u00c4': sb.append("&Auml;");break;
        case '\u00e5': sb.append("&aring;");break;
        case '\u00c5': sb.append("&Aring;");break;
        case '\u00e6': sb.append("&aelig;");break;
        case '\u00c6': sb.append("&AElig;");break;
        case '\u00e7': sb.append("&ccedil;");break;
        case '\u00c7': sb.append("&Ccedil;");break;
        case '\u00e9': sb.append("&eacute;");break;
        case '\u00c9': sb.append("&Eacute;");break;
        case '\u00e8': sb.append("&egrave;");break;
        case '\u00c8': sb.append("&Egrave;");break;
        case '\u00ea': sb.append("&ecirc;");break;
        case '\u00ca': sb.append("&Ecirc;");break;
        case '\u00eb': sb.append("&euml;");break;
        case '\u00cb': sb.append("&Euml;");break;
        case '\u00ef': sb.append("&iuml;");break;
        case '\u00cf': sb.append("&Iuml;");break;
        case '\u00f4': sb.append("&ocirc;");break;
        case '\u00d4': sb.append("&Ocirc;");break;
        case '\u00f6': sb.append("&ouml;");break;
        case '\u00d6': sb.append("&Ouml;");break;
        case '\u00f8': sb.append("&oslash;");break;
        case '\u00d8': sb.append("&Oslash;");break;
        case '\u00df': sb.append("&szlig;");break;
        case '\u00f9': sb.append("&ugrave;");break;
        case '\u00d9': sb.append("&Ugrave;");break;
        case '\u00fb': sb.append("&ucirc;");break;
        case '\u00db': sb.append("&Ucirc;");break;
        case '\u00fc': sb.append("&uuml;");break;
        case '\u00dc': sb.append("&Uuml;");break;
        case '\u00ae': sb.append("&reg;");break;
        case '\u00a9': sb.append("&copy;");break;
        case '\u20ac': sb.append("&euro;"); break;

        default:  sb.append(c); break;
      }
    }
    return sb.toString();
  }

  private static final int MIN_ESCAPE = 2;
  private static final int MAX_ESCAPE = 6;

  // From https://stackoverflow.com/a/24575417
  public static final String unescapeHTML(String input)
  {
    StringWriter writer = null;
    int len = input.length();
    int i = 1;
    int st = 0;

    while (true)
    {
      // look for '&'
      while (i < len && input.charAt(i - 1) != '&')
      {
        i++;
      }
      if (i >= len) break;

      // found '&', look for ';'
      int j = i;
      while (j < len && j < i + MAX_ESCAPE + 1 && input.charAt(j) != ';')
      {
        j++;
      }

      if (j == len || j < i + MIN_ESCAPE || j == i + MAX_ESCAPE + 1)
      {
        i++;
        continue;
      }

      // found escape
      if (input.charAt(i) == '#')
      {
        // numeric escape
        int k = i + 1;
        int radix = 10;

        final char firstChar = input.charAt(k);
        if (firstChar == 'x' || firstChar == 'X')
        {
          k++;
          radix = 16;
        }

        try
        {
          int entityValue = Integer.parseInt(input.substring(k, j), radix);

          if (writer == null)
          {
            writer = new StringWriter(input.length());
          }
          writer.append(input.substring(st, i - 1));

          if (entityValue > 0xFFFF)
          {
            final char[] chrs = Character.toChars(entityValue);
            writer.write(chrs[0]);
            writer.write(chrs[1]);
          }
          else
          {
            writer.write(entityValue);
          }

        }
        catch (NumberFormatException ex)
        {
          i++;
          continue;
        }
      }
      else
      {
        String name = input.substring(i, j);
        CharSequence value = ESCAPES.get(name);
        if (value == null)
        {
          i++;
          continue;
        }

        if (writer == null)
        {
          writer = new StringWriter(input.length());
        }
        writer.append(input.substring(st, i - 1));
        writer.append(value);
      }
      // skip escape
      st = j + 1;
      i = st;
    }

    if (writer != null)
    {
      writer.append(input.substring(st, len));
      return writer.toString();
    }
    return input;
  }

    private static final Map<String, String> ESCAPES = new HashMap<String, String>()
    {{
       put("quot"  , "\""); // " - double-quote
       put("amp"   , "&"); // & - ampersand
       put("lt"    , "<"); // < - less-than
       put("gt"    , ">"); // > - greater-than
       put("nbsp"  , "\u00A0"); // non-breaking space
       put("iexcl" , "\u00A1"); // inverted exclamation mark
       put("cent"  , "\u00A2"); // cent sign
       put("pound" , "\u00A3"); // pound sign
       put("curren", "\u00A4"); // currency sign
       put("yen"   , "\u00A5"); // yen sign = yuan sign
       put("brvbar", "\u00A6"); // broken bar = broken vertical bar
       put("sect"  , "\u00A7"); // section sign
       put("uml"   , "\u00A8"); // diaeresis = spacing diaeresis
       put("copy"  , "\u00A9"); // © - copyright sign
       put("ordf"  , "\u00AA"); // feminine ordinal indicator
       put("laquo" , "\u00AB"); // left-pointing double angle quotation mark = left pointing guillemet
       put("not"   , "\u00AC"); // not sign
       put("shy"   , "\u00AD"); // soft hyphen = discretionary hyphen
       put("reg"   , "\u00AE"); // ® - registered trademark sign
       put("macr"  , "\u00AF"); // macron = spacing macron = overline = APL overbar
       put("deg"   , "\u00B0"); // degree sign
       put("plusmn", "\u00B1"); // plus-minus sign = plus-or-minus sign
       put("sup2"  , "\u00B2"); // superscript two = superscript digit two = squared
       put("sup3"  , "\u00B3"); // superscript three = superscript digit three = cubed
       put("acute" , "\u00B4"); // acute accent = spacing acute
       put("micro" , "\u00B5"); // micro sign
       put("para"  , "\u00B6"); // pilcrow sign = paragraph sign
       put("middot", "\u00B7"); // middle dot = Georgian comma = Greek middle dot
       put("cedil" , "\u00B8"); // cedilla = spacing cedilla
       put("sup1"  , "\u00B9"); // superscript one = superscript digit one
       put("ordm"  , "\u00BA"); // masculine ordinal indicator
       put("raquo" , "\u00BB"); // right-pointing double angle quotation mark = right pointing guillemet
       put("frac14", "\u00BC"); // vulgar fraction one quarter = fraction one quarter
       put("frac12", "\u00BD"); // vulgar fraction one half = fraction one half
       put("frac34", "\u00BE"); // vulgar fraction three quarters = fraction three quarters
       put("iquest", "\u00BF"); // inverted question mark = turned question mark
       put("Agrave", "\u00C0"); // uppercase A, grave accent
       put("Aacute", "\u00C1"); // uppercase A, acute accent
       put("Acirc" , "\u00C2"); // uppercase A, circumflex accent
       put("Atilde", "\u00C3"); // uppercase A, tilde
       put("Auml"  , "\u00C4"); // uppercase A, umlaut
       put("Aring" , "\u00C5"); // uppercase A, ring
       put("AElig" , "\u00C6"); // uppercase AE
       put("Ccedil", "\u00C7"); // uppercase C, cedilla
       put("Egrave", "\u00C8"); // uppercase E, grave accent
       put("Eacute", "\u00C9"); // uppercase E, acute accent
       put("Ecirc" , "\u00CA"); // uppercase E, circumflex accent
       put("Euml"  , "\u00CB"); // uppercase E, umlaut
       put("Igrave", "\u00CC"); // uppercase I, grave accent
       put("Iacute", "\u00CD"); // uppercase I, acute accent
       put("Icirc" , "\u00CE"); // uppercase I, circumflex accent
       put("Iuml"  , "\u00CF"); // uppercase I, umlaut
       put("ETH"   , "\u00D0"); // uppercase Eth, Icelandic
       put("Ntilde", "\u00D1"); // uppercase N, tilde
       put("Ograve", "\u00D2"); // uppercase O, grave accent
       put("Oacute", "\u00D3"); // uppercase O, acute accent
       put("Ocirc" , "\u00D4"); // uppercase O, circumflex accent
       put("Otilde", "\u00D5"); // uppercase O, tilde
       put("Ouml"  , "\u00D6"); // uppercase O, umlaut
       put("times" , "\u00D7"); // multiplication sign
       put("Oslash", "\u00D8"); // uppercase O, slash
       put("Ugrave", "\u00D9"); // uppercase U, grave accent
       put("Uacute", "\u00DA"); // uppercase U, acute accent
       put("Ucirc" , "\u00DB"); // uppercase U, circumflex accent
       put("Uuml"  , "\u00DC"); // uppercase U, umlaut
       put("Yacute", "\u00DD"); // uppercase Y, acute accent
       put("THORN" , "\u00DE"); // uppercase THORN, Icelandic
       put("szlig" , "\u00DF"); // lowercase sharps, German
       put("agrave", "\u00E0"); // lowercase a, grave accent
       put("aacute", "\u00E1"); // lowercase a, acute accent
       put("acirc" , "\u00E2"); // lowercase a, circumflex accent
       put("atilde", "\u00E3"); // lowercase a, tilde
       put("auml"  , "\u00E4"); // lowercase a, umlaut
       put("aring" , "\u00E5"); // lowercase a, ring
       put("aelig" , "\u00E6"); // lowercase ae
       put("ccedil", "\u00E7"); // lowercase c, cedilla
       put("egrave", "\u00E8"); // lowercase e, grave accent
       put("eacute", "\u00E9"); // lowercase e, acute accent
       put("ecirc" , "\u00EA"); // lowercase e, circumflex accent
       put("euml"  , "\u00EB"); // lowercase e, umlaut
       put("igrave", "\u00EC"); // lowercase i, grave accent
       put("iacute", "\u00ED"); // lowercase i, acute accent
       put("icirc" , "\u00EE"); // lowercase i, circumflex accent
       put("iuml"  , "\u00EF"); // lowercase i, umlaut
       put("eth"   , "\u00F0"); // lowercase eth, Icelandic
       put("ntilde", "\u00F1"); // lowercase n, tilde
       put("ograve", "\u00F2"); // lowercase o, grave accent
       put("oacute", "\u00F3"); // lowercase o, acute accent
       put("ocirc" , "\u00F4"); // lowercase o, circumflex accent
       put("otilde", "\u00F5"); // lowercase o, tilde
       put("ouml"  , "\u00F6"); // lowercase o, umlaut
       put("divide", "\u00F7"); // division sign
       put("oslash", "\u00F8"); // lowercase o, slash
       put("ugrave", "\u00F9"); // lowercase u, grave accent
       put("uacute", "\u00FA"); // lowercase u, acute accent
       put("ucirc" , "\u00FB"); // lowercase u, circumflex accent
       put("uuml"  , "\u00FC"); // lowercase u, umlaut
       put("yacute", "\u00FD"); // lowercase y, acute accent
       put("thorn" , "\u00FE"); // lowercase thorn, Icelandic
       put("yuml"  , "\u00FF"); // lowercase y, umlaut
  }};

  public static String cleanHTML(String input)
  {
    if (input == null) return input;
    return input.replaceAll("\\<.*?\\>", "");
  }

  public static String convertToMultiline(String orig)
  {
    return orig.replaceAll(StringUtil.REGEX_CRLF, "<br>");
  }

}
