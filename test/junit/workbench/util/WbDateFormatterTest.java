/*
 * WbDateFormatterTest.java
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

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import workbench.db.exporter.InfinityLiterals;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbDateFormatterTest
{

	public WbDateFormatterTest()
	{
	}

  @Test
  public void testInfinityDate()
  {
		WbDateFormatter formatter = new WbDateFormatter("yyyy-MM-dd");
    assertEquals(InfinityLiterals.PG_POSITIVE_LITERAL, formatter.formatDate(LocalDate.MAX));
    assertEquals(InfinityLiterals.PG_NEGATIVE_LITERAL, formatter.formatDate(LocalDate.MIN));

    assertEquals(InfinityLiterals.PG_POSITIVE_LITERAL, formatter.formatDateTimeValue(LocalDateTime.MAX));
    assertEquals(InfinityLiterals.PG_NEGATIVE_LITERAL, formatter.formatDateTimeValue(LocalDateTime.MIN));

    java.sql.Date dt = formatter.parseDate(InfinityLiterals.PG_POSITIVE_LITERAL);
    assertNotNull(dt);
    assertEquals(dt, new java.sql.Date(WbDateFormatter.DATE_POSITIVE_INFINITY));
  }

  @Test
  public void testDayName()
  {
		WbDateFormatter formatter = new WbDateFormatter();
    formatter.setLocale(Locale.GERMAN);
    formatter.applyPattern("EE'.', dd.MM.yy");
    LocalDate dt = formatter.parseDate("Mo., 12.06.17").toLocalDate();
    assertNotNull(dt);
    assertEquals(2017, dt.getYear());
    assertEquals(6, dt.getMonthValue());
    assertEquals(12, dt.getDayOfMonth());
  }

	@Test
	public void testFormat()
	{
		Calendar cal = Calendar.getInstance();
		cal.set(2012, 0, 1);

		WbDateFormatter formatter = new WbDateFormatter("yyyy-MM-dd");
		String result = formatter.formatUtilDate(cal.getTime());
		assertEquals("2012-01-01", result);

		result = formatter.formatUtilDate(new Date(WbDateFormatter.DATE_POSITIVE_INFINITY));
		assertEquals(InfinityLiterals.PG_POSITIVE_LITERAL, result);
	}

	@Test
	public void testParse()
		throws Exception
	{
		String source = "2012-01-01";
		WbDateFormatter formatter = new WbDateFormatter("yyyy-MM-dd");

    LocalDate expected = LocalDate.of(2012, Month.JANUARY, 1);
		LocalDate result = formatter.parseDate(source).toLocalDate();
		assertEquals(expected, result);
		java.sql.Date infinity = new java.sql.Date(WbDateFormatter.DATE_POSITIVE_INFINITY);
		assertEquals(infinity, formatter.parseDate(InfinityLiterals.PG_POSITIVE_LITERAL));

    LocalDateTime ts = formatter.parseTimestamp(source).toLocalDateTime();
    assertEquals(expected.atTime(0, 0, 0), ts);
	}

  @Test
  public void testParseTZ()
  {
    WbDateFormatter formatter = new WbDateFormatter("yyyy-MM-dd HH:mm:ss Z");
    Temporal tz = formatter.parseTimestampTZ("2017-01-01 04:00:00 +0200");
    assertTrue(tz instanceof OffsetDateTime);
    OffsetDateTime odt = OffsetDateTime.of(2017, 1, 1, 4, 0, 0, 0, ZoneOffset.of("+0200"));
    assertEquals(odt, tz);

    formatter.applyPattern("yyyy-MM-dd HH:mm:ss VV");
    tz = formatter.parseTimestampTZ("2017-01-01 04:00:00 " + ZoneId.systemDefault().toString());
    assertTrue(tz instanceof ZonedDateTime);
    System.out.println(tz);
    ZonedDateTime zdt = ZonedDateTime.of(2017, 1, 1, 4, 0, 0, 0, ZoneId.systemDefault());
    assertEquals(zdt, tz);
  }

  @Test
  public void testTimestampTZ()
  {
    WbDateFormatter formatter = new WbDateFormatter("dd.MM.yyyy HH:mm:ss Z");
    LocalDateTime ldt = LocalDateTime.of(2017, Month.APRIL, 1, 0, 0);
    OffsetDateTime odt = OffsetDateTime.of(ldt, ZoneOffset.UTC);
    ZonedDateTime zdt = odt.atZoneSameInstant(ZoneOffset.of("+0200"));
    String formatted = formatter.formatTimestamp(zdt);
    assertEquals("01.04.2017 02:00:00 +0200", formatted);

    formatter = new WbDateFormatter("dd.MM.yyyy HH:mm:ss");
    formatted = formatter.formatTimestamp(zdt);
    assertEquals("01.04.2017 02:00:00", formatted);
  }

  @Test
  public void testTimestamp()
  {
    WbDateFormatter format = new WbDateFormatter("dd.MM.yyyy HH:mm:ss Z");
    Timestamp ts = Timestamp.valueOf("2015-03-27 20:21:22.123456");
    assertEquals("27.03.2015 20:21:22", format.formatTimestamp(ts));

    format.applyPattern("dd.MM.yyyy HH:mm:ss.SSS");
    assertEquals("27.03.2015 20:21:22.123", format.formatTimestamp(ts));

    format.applyPattern("dd.MM.yyyy HH:mm:ss.SSSSSS");
    assertEquals("27.03.2015 20:21:22.123456", format.formatTimestamp(ts));

    format.applyPattern("SSSSSS dd.MM.yyyy HH:mm:ss");
    assertEquals("123456 27.03.2015 20:21:22", format.formatTimestamp(ts));

    format.applyPattern("SSS dd.MM.yyyy HH:mm:ss");
    assertEquals("123 27.03.2015 20:21:22", format.formatTimestamp(ts));

    format.applyPattern("dd.MM.yyyy HH:mm:ss.SSS");
    ts = Timestamp.valueOf("2015-03-27 20:21:22.123789");
    assertEquals("27.03.2015 20:21:22.123", format.formatTimestamp(ts));

    format.applyPattern("dd.MM.yyyy HH:mm:ss.SSSSSSSSS");
    ts = Timestamp.valueOf("2015-03-27 20:21:22.123456789");
    assertEquals("27.03.2015 20:21:22.123456789", format.formatTimestamp(ts));

    format.applyPattern("dd.MM.yyyy HH:mm:ss.SSSSSS");

    ts = Timestamp.valueOf("2015-03-27 20:21:22.123");
    assertEquals("27.03.2015 20:21:22.123000", format.formatTimestamp(ts));

    ts = Timestamp.valueOf("2015-03-27 20:21:22.0001");
    assertEquals("27.03.2015 20:21:22.000100", format.formatTimestamp(ts));

    format.applyPattern("dd.MM.yyyy HH:mm:ss.SSSSSS");
    ts = Timestamp.valueOf("2015-03-27 20:21:22");
    assertEquals("27.03.2015 20:21:22.000000", format.formatTimestamp(ts));

    format.applyPattern("'TIMESTAMP '''yyyy-MM-dd HH:mm:ss.SSSSSS''", true);
    ts = Timestamp.valueOf("2015-03-27 20:21:22.123456");
    assertEquals("TIMESTAMP '2015-03-27 20:21:22.123456'", format.formatTimestamp(ts));

    format.applyPattern("yyyy-MM-dd HH:mm:ss.SSSSSS", true);
    assertEquals("2015-03-27 20:21:22.123456", format.formatTimestamp(ts));

    format.applyPattern("dd.MM.yyyy HH:mm:ss.SSSSSSSSS");
    ts = Timestamp.valueOf("2015-03-27 20:21:22.789");
    assertEquals("27.03.2015 20:21:22.789000000", format.formatTimestamp(ts));
  }

	@Test
	public void testGetDisplayValue()
	{
		Calendar cal = Calendar.getInstance();
		cal.set(2012, 0, 1);
		String result = WbDateFormatter.getDisplayValue("hello");
		assertEquals("hello", result);

		result = WbDateFormatter.getDisplayValue(new Date(WbDateFormatter.DATE_POSITIVE_INFINITY));
		assertEquals(InfinityLiterals.PG_POSITIVE_LITERAL, result);

		result = WbDateFormatter.getDisplayValue(Integer.valueOf(42));
		assertEquals("42", result);
	}
}
