/*
 * WbDateFormatter.java
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

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.exporter.InfinityLiterals;


/**
 *
 * @author Thomas Kellerer
 */
public class WbDateFormatter
{
  private String pattern;
  private String patternWithoutTZ;
  private DateTimeFormatter formatter;
  private DateTimeFormatter formatterWithoutTimeZone;

  // copied from the PostgreSQL driver
  public static final long DATE_POSITIVE_INFINITY = 9223372036825200000l;
  public static final long DATE_NEGATIVE_INFINITY = -9223372036832400000l;

  private InfinityLiterals infinityLiterals = InfinityLiterals.PG_LITERALS;

  private boolean illegalDateAsNull;
  private boolean containsTimeFields;

  private final String timeFields = "ahKkHmsSAnNVzOXxZ";
  private Locale localeToUse;

  private Pattern timezonePatterns = Pattern.compile("( ){0,1}[zV]+");
  private Pattern offsetPatterns = Pattern.compile("( ){0,1}[XxZO]+");

  // true if the pattern contains an offset pattern (O,x,X,Z)
  private boolean containsTZOffset;

  // true if the pattern contains a timezone pattern (V,z)
  private boolean containsTimeZone;

  public WbDateFormatter(String pattern)
  {
    applyPattern(pattern, false);
  }

  public WbDateFormatter(String pattern, Locale locale)
  {
    localeToUse = locale;
    applyPattern(pattern, false);
  }

  public WbDateFormatter(String pattern, boolean allowVariableLengthFractions)
  {
    applyPattern(pattern, allowVariableLengthFractions);
  }

  public WbDateFormatter()
  {
    applyPattern(StringUtil.ISO_DATE_FORMAT);
  }

  public void setLocale(Locale locale)
  {
    localeToUse = locale;
  }

  public void setIllegalDateIsNull(boolean flag)
  {
    illegalDateAsNull = flag;
    if (illegalDateAsNull)
    {
      formatter = formatter.withResolverStyle(ResolverStyle.STRICT);
    }
    else
    {
      formatter = formatter.withResolverStyle(ResolverStyle.SMART);
    }
  }

  public void applyPattern(String pattern)
  {
    applyPattern(pattern, false);
  }

  /**
   * Returns true if the supplied pattern contains a time zone of offset pattern.
   */
  public boolean patternContainesTimeZoneInformation()
  {
    return containsTimeZone || containsTZOffset;
  }

  public void applyPattern(String newPattern, boolean allowVariableLengthFraction)
  {
    formatter = createFormatter(newPattern, allowVariableLengthFraction);
    pattern = newPattern;
    patternWithoutTZ = null;
    containsTimeFields = checkForTimeFields();
    formatterWithoutTimeZone = null;

    containsTimeZone = containsTimezonePattern(newPattern);
    containsTZOffset = containsOffsetPattern(newPattern);

    if (patternContainesTimeZoneInformation())
    {
      // create a second formatter without any timezone to be able to support timestamps with and without time zone
      Matcher m = timezonePatterns.matcher(newPattern);
      newPattern = m.replaceAll("");
      m = offsetPatterns.matcher(newPattern);
      newPattern = m.replaceAll("");

      patternWithoutTZ = newPattern;
      formatterWithoutTimeZone = createFormatter(newPattern, allowVariableLengthFraction);
    }
  }

  public DateTimeFormatter createFormatter(String pattern, boolean allowVariableLengthFraction)
  {
    DateTimeFormatterBuilder builder = null;

    if (allowVariableLengthFraction)
    {
      Pattern p = Pattern.compile("\\.S{1,6}");
      Matcher matcher = p.matcher(pattern);
      // Make any millisecond/microsecond definition optional
      // so that inputs with a variable length of milli/microseconds can be parsed
      if (matcher.find())
      {
        int start = matcher.start();
        int end = matcher.end();
        // remove the .SSSSS from the pattern so that we can re-add it with a variable length using appendFraction
        String patternStart = pattern.substring(0, start);
        String patternEnd = pattern.substring(end);
        builder = new DateTimeFormatterBuilder().appendPattern(patternStart);
        int len = end - start;
        builder.appendFraction(ChronoField.MICRO_OF_SECOND, 0, len - 1, true);
        if (patternEnd != null)
        {
          builder.appendPattern(patternEnd);
        }
      }
    }

    if (builder == null)
    {
      builder = new DateTimeFormatterBuilder().appendPattern(pattern);
    }

    DateTimeFormatter dtf = null;
    if (localeToUse != null)
    {
      dtf = builder.toFormatter(localeToUse);
    }
    else
    {
      dtf = builder.toFormatter();
    }
    DateTimeFormatter format = dtf.withResolverStyle(ResolverStyle.SMART);
    return format;
  }

  private boolean containsTimezonePattern(String toCheck)
  {
    return timezonePatterns.matcher(toCheck).find();
  }

  private boolean containsOffsetPattern(String toCheck)
  {
    return offsetPatterns.matcher(toCheck).find();
  }

  private boolean checkForTimeFields()
  {
    for (int i=0; i < timeFields.length(); i++)
    {
      if (pattern.indexOf(timeFields.charAt(i)) > -1) return true;
    }
    return false;
  }

  public void setInfinityLiterals(InfinityLiterals literals)
  {
    this.infinityLiterals = literals;
  }

  public String formatTime(LocalTime time)
  {
    if (time == null) return "";

    return formatter.format(time);
  }

  public String formatTime(java.sql.Time time)
  {
    if (time == null) return "";

    return formatter.format(time.toLocalTime());
  }

  public String formatUtilDate(java.util.Date date)
  {
    if (date == null) return "";

    if (date instanceof java.sql.Date)
    {
      return formatDate((java.sql.Date)date);
    }

    String result = getInfinityValue(date.getTime());
    if (result != null)
    {
      return result;
    }

    LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    return formatter.format(ldt);
  }

  public String formatDate(java.sql.Date date)
  {
    if (date == null) return "";

    String result = getInfinityValue(date.getTime());
    if (result != null)
    {
      return result;
    }

    // If the pattern for a java.sql.Date contains time fields DateTimeFormatter will
    // throw an exception.
    // So we convert the java.sql.Date to a java.util.Date as a fallback.
    // It's unlikely the java.sql.Date instance will have a proper time value,
    // but at least the date part will be formatted the way the user expects it
    if (containsTimeFields)
    {
      java.util.Date ud = new java.util.Date(date.getTime());
      LocalDateTime ldt = LocalDateTime.ofInstant(ud.toInstant(), ZoneId.systemDefault());
      return formatter.format(ldt);
    }
    return formatter.format(date.toLocalDate());
  }

  private String getInfinityValue(long dt)
  {
    if (infinityLiterals != null)
    {
      if (dt == DATE_POSITIVE_INFINITY)
      {
        return infinityLiterals.getPositiveInfinity();
      }
      else if (dt == DATE_NEGATIVE_INFINITY)
      {
        return infinityLiterals.getNegativeInfinity();
      }
    }
    return null;
  }

  private String getInfinityFromYear(int year)
  {
    if (infinityLiterals != null)
    {
      if (year == LocalDate.MAX.getYear())
      {
        return infinityLiterals.getPositiveInfinity();
      }
      if (year == LocalDate.MIN.getYear())
      {
        return infinityLiterals.getNegativeInfinity();
      }
    }
    return null;
  }

  public String formatDate(java.time.LocalDate ts)
  {
    if (ts == null) return "";

    String result = getInfinityFromYear(ts.getYear());
    if (result != null)
    {
      return result;
    }

    if (formatterWithoutTimeZone != null)
    {
      return formatterWithoutTimeZone.format(ts);
    }
    return formatter.format(ts);
  }

  public String formatTimestamp(java.sql.Timestamp ts)
  {
    if (ts == null) return "";

    String result = getInfinityValue(ts.getTime());
    if (result != null)
    {
      return result;
    }
    LocalDateTime ldt = ts.toLocalDateTime();

    if (formatterWithoutTimeZone != null)
    {
      return formatterWithoutTimeZone.format(ldt);
    }
    return formatter.format(ldt);
  }

  public String formatTimestamp(java.time.LocalDateTime ts)
  {
    if (ts == null) return "";

    String result = getInfinityFromYear(ts.getYear());
    if (result != null)
    {
      return result;
    }

    if (formatterWithoutTimeZone != null)
    {
      return formatterWithoutTimeZone.format(ts);
    }
    return formatter.format(ts);
  }

  public String formatTimestamp(java.time.ZonedDateTime ts)
  {
    if (ts == null) return "";

    String result = getInfinityFromYear(ts.getYear());
    if (result != null)
    {
      return result;
    }

    return formatter.format(ts);
  }

  public String formatTimestamp(java.time.OffsetDateTime ts)
  {
    if (ts == null) return "";

    if (infinityLiterals != null)
    {
      if (ts.equals(OffsetDateTime.MAX))
      {
        return infinityLiterals.getPositiveInfinity();
      }
      if (ts.equals(OffsetDateTime.MIN))
      {
        return infinityLiterals.getNegativeInfinity();
      }
    }
    return formatter.format(ts);
  }

  public static boolean isTimestampValue(Object value)
  {
    return value instanceof java.sql.Timestamp ||
           value instanceof LocalDateTime ||
           value instanceof OffsetDateTime ||
           value instanceof ZonedDateTime;
  }
  public static boolean isDateValue(Object value)
  {
    return value instanceof java.sql.Date ||
           value instanceof LocalDate ||
           value instanceof java.util.Date;
  }
  public static boolean isTimeValue(Object value)
  {
    return value instanceof java.sql.Time ||
           value instanceof LocalTime;
  }

  public static boolean isDateTimeValue(Object value)
  {
    return isTimestampValue(value) || isDateValue(value) || isTimeValue(value);
  }

  public String formatDateTimeValue(Object value)
  {
    if (value == null) return "";

    // this test MUST be before the test for java.util.Date!
    if (value instanceof java.sql.Timestamp)
    {
      return formatTimestamp((java.sql.Timestamp)value);
    }

    if (value instanceof LocalDateTime)
    {
      return formatTimestamp((LocalDateTime)value);
    }

    if (value instanceof OffsetDateTime)
    {
      return formatTimestamp((OffsetDateTime)value);
    }

    if (value instanceof ZonedDateTime)
    {
      return formatTimestamp((ZonedDateTime)value);
    }

    if (value instanceof LocalTime)
    {
      return formatTime((LocalTime)value);
    }

    if (value instanceof LocalDate)
    {
      return formatDate((LocalDate)value);
    }

    if (value instanceof java.sql.Time)
    {
      return formatTime((java.sql.Time)value);
    }

    // this test MUST be before the test for java.util.Date!
    if (value instanceof java.sql.Date)
    {
      return formatDate((java.sql.Date)value);
    }

    if (value instanceof java.util.Date)
    {
      return formatUtilDate((java.util.Date)value);
    }

    // shouldn't happen
    LogMgr.logError(new CallerInfo(){}, "formatDateTimeValue() called with an instance that is not a date/time value", new Exception("Backtrace"));
    return value.toString();
  }

  public java.sql.Time parseTimeQuitely(String source)
  {
    try
    {
      return parseTime(source);
    }
    catch (ParseException ex)
    {
      return null;
    }
  }

  public java.sql.Time parseTime(String source)
    throws ParseException
  {
    LocalTime lt = LocalTime.parse(source, formatter);
    return java.sql.Time.valueOf(lt);
  }

  public java.sql.Date parseDateQuietely(String source)
  {
    source = StringUtil.trimToNull(source);
    if (source == null) return null;

    try
    {
      return parseDate(source);
    }
    catch (DateTimeParseException ex)
    {
      return null;
    }
  }

  public java.sql.Date parseDate(String source)
    throws DateTimeParseException
  {
    source = StringUtil.trimToNull(source);
    if (source == null) return null;

    if (infinityLiterals != null)
    {
      if (source.equalsIgnoreCase(infinityLiterals.getPositiveInfinity()))
      {
        return new java.sql.Date(DATE_POSITIVE_INFINITY);
      }
      if (source.equalsIgnoreCase(infinityLiterals.getNegativeInfinity()))
      {
        return new java.sql.Date(DATE_NEGATIVE_INFINITY);
      }
    }

    try
    {
      LocalDate ld = LocalDate.parse(source, formatter);
      return java.sql.Date.valueOf(ld);
    }
    catch (DateTimeParseException ex)
    {
      if (illegalDateAsNull) return null;
      throw ex;
    }
  }

  public java.sql.Timestamp parseTimestampQuietly(String source)
  {
    try
    {
      return parseTimestamp(source);
    }
    catch (DateTimeParseException ex)
    {
      return null;
    }
  }

  public java.time.temporal.Temporal parseTimestampTZ(String source)
    throws DateTimeParseException
  {
    if (source == null) return null;

    if (!containsTimeFields)
    {
      // a format mask that does not include time values cannot be parsed using ZonedDateTime
      // it must be done through LocalDate
      LocalDate ld = LocalDate.parse(source, formatter);
      return java.time.ZonedDateTime.of(ld, LocalTime.MIDNIGHT, ZoneId.systemDefault());
    }

    if (infinityLiterals != null)
    {
      if (source.trim().equalsIgnoreCase(infinityLiterals.getPositiveInfinity()))
      {
        return java.time.ZonedDateTime.of(LocalDateTime.MAX, ZoneId.ofOffset("", ZoneOffset.UTC));
      }

      if (source.trim().equalsIgnoreCase(infinityLiterals.getNegativeInfinity()))
      {
        return java.time.ZonedDateTime.of(LocalDateTime.MIN, ZoneId.ofOffset("", ZoneOffset.UTC));
      }
    }

    try
    {
      // Using the TemporalAccessor is more robust then parsing the string directly
      // Using this, we can detect if parts are missing. This also seems to be a lot
      // faster then using formatter.parseBest();
      TemporalAccessor acc = formatter.parse(source);
      ZoneId zoneId = TemporalQueries.zoneId().queryFrom(acc);
      ZoneOffset offset = TemporalQueries.offset().queryFrom(acc);
      LocalDate ld = TemporalQueries.localDate().queryFrom(acc);
      LocalTime lt = TemporalQueries.localTime().queryFrom(acc);

      if (zoneId != null)
      {
        return ZonedDateTime.of(ld, lt, zoneId);
      }
      if (offset != null)
      {
        return OffsetDateTime.of(ld, lt, offset);
      }
      // Should not
      return LocalDateTime.of(ld, lt);
    }
    catch (DateTimeParseException ex)
    {
      if (illegalDateAsNull) return null;
      throw ex;
    }
  }

  public java.sql.Timestamp parseTimestamp(String source)
    throws DateTimeParseException
  {
    if (source == null) return null;

    if (!containsTimeFields)
    {
      // a format mask that does not include time values cannot be parsed using LocalDateTime
      // it must be done through LocalDate
      java.sql.Date dt = parseDate(source);
      return new java.sql.Timestamp(dt.getTime());
    }

    if (infinityLiterals != null)
    {
      if (source.trim().equalsIgnoreCase(infinityLiterals.getPositiveInfinity()))
      {
        return new java.sql.Timestamp(DATE_POSITIVE_INFINITY);
      }

      if (source.trim().equalsIgnoreCase(infinityLiterals.getNegativeInfinity()))
      {
        return new java.sql.Timestamp(DATE_NEGATIVE_INFINITY);
      }
    }

    try
    {
      LocalDateTime ldt = LocalDateTime.parse(source, formatter);
      return java.sql.Timestamp.valueOf(ldt);
    }
    catch (DateTimeParseException ex)
    {
      if (illegalDateAsNull) return null;
      throw ex;
    }
  }

  public String getPatternWithoutTimeZone()
  {
    if (this.patternWithoutTZ != null)
    {
      return this.patternWithoutTZ;
    }
    return toPattern();
  }

  public String toPattern()
  {
    return pattern;
  }

  public static String getDisplayValue(Object value)
  {
    if (value == null) return "";

    if (value instanceof java.sql.Date)
    {
      String format = Settings.getInstance().getDefaultDateFormat();
      WbDateFormatter formatter = new WbDateFormatter(format);
      return formatter.formatDate((java.sql.Date) value);
    }

    if (value instanceof java.sql.Timestamp)
    {
      String format = Settings.getInstance().getDefaultTimestampFormat();
      WbDateFormatter formatter = new WbDateFormatter(format);
      return formatter.formatTimestamp((java.sql.Timestamp) value);
    }

    if (value instanceof ZonedDateTime)
    {
      String format = Settings.getInstance().getDefaultTimestampFormat();
      WbDateFormatter formatter = new WbDateFormatter(format);
      return formatter.formatTimestamp((java.time.ZonedDateTime) value);
    }

    if (value instanceof OffsetDateTime)
    {
      String format = Settings.getInstance().getDefaultTimestampFormat();
      WbDateFormatter formatter = new WbDateFormatter(format);
      return formatter.formatTimestamp((OffsetDateTime) value);
    }

    if (value instanceof LocalDate)
    {
      String format = Settings.getInstance().getDefaultDateFormat();
      WbDateFormatter formatter = new WbDateFormatter(format);
      return formatter.formatDate((LocalDate) value);
    }

    if (value instanceof LocalDateTime)
    {
      String format = Settings.getInstance().getDefaultTimestampFormat();
      WbDateFormatter formatter = new WbDateFormatter(format);
      return formatter.formatTimestamp((LocalDateTime) value);
    }

    if (value instanceof java.util.Date)
    {
      long time = ((java.util.Date)value).getTime();
      if (time == DATE_POSITIVE_INFINITY)
      {
        return InfinityLiterals.PG_POSITIVE_LITERAL;
      }
      if (time == WbDateFormatter.DATE_NEGATIVE_INFINITY)
      {
        return InfinityLiterals.PG_NEGATIVE_LITERAL;
      }
    }

    return value.toString();
  }

  public static ZoneOffset getSystemDefaultOffset()
  {
    Instant instant = Instant.now();
    ZoneId systemZone = ZoneId.systemDefault();
    return systemZone.getRules().getOffset(instant);
  }

}
