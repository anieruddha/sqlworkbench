/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2019 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
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
package workbench.storage.reader;

import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import workbench.WbTestCase;

import workbench.db.WbConnection;
import workbench.db.postgres.PostgresTestUtil;

import workbench.storage.ResultInfo;
import workbench.storage.RowData;

import workbench.util.SqlUtil;
import workbench.util.WbDateFormatter;

import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresRowDataReaderTest
  extends WbTestCase
{

  public PostgresRowDataReaderTest()
  {
    super("PgRowDataReader");
  }

  @Test
  public void testReader()
    throws Exception
  {
    WbConnection con = PostgresTestUtil.getPostgresConnection();
    Assume.assumeNotNull(con);

    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      stmt = con.createStatement();
      rs = stmt.executeQuery(
        "select '2019-04-05 19:20:21'::timestamp as ts,\n" +
        "       '2019-04-05 19:20:21'::timestamptz as tstz, \n" +
        "       '2019-04-05'::date dt, \n" +
        "       '18:19:20'::time as t, \n" +
        "       '18:19:20'::timetz as tz, \n" +
        "       'infinity'::timestamptz as tz_inf, \n" +
        "       '-infinity'::timestamptz as tz_minf, \n" +
        "       'infinity'::timestamp as t_inf, \n" +
        "       '-infinity'::timestamp as t_minf");
      ResultInfo info = new ResultInfo(rs.getMetaData(), con);
      PostgresRowDataReader reader = new PostgresRowDataReader(info, con);
      rs.next();
      RowData row = reader.read(rs, false);
      assertNotNull(row);
      Object ldt = row.getValue(0);
      assertTrue(ldt instanceof LocalDateTime);
      assertEquals(LocalDateTime.of(2019, 4, 5, 19, 20, 21), (LocalDateTime)ldt);

      Object dt = row.getValue(1);
      assertTrue(dt instanceof ZonedDateTime);
      ZonedDateTime odt = ZonedDateTime.of(2019, 04, 05, 19, 20, 21, 0, ZoneId.systemDefault());
      assertEquals(odt, (ZonedDateTime)dt);

      Object d = row.getValue(2);
      assertTrue(d instanceof LocalDate);
      assertEquals(LocalDate.of(2019, 4, 5), (LocalDate)d);

      WbDateFormatter wdt = new WbDateFormatter();

      Object inf = row.getValue(5);
      assertTrue(inf instanceof ZonedDateTime);
      String f = wdt.formatDateTimeValue(inf);
      assertEquals("infinity", f);

      Object inf2 = row.getValue(6);
      assertTrue(inf2 instanceof ZonedDateTime);
      f = f = wdt.formatDateTimeValue(inf2);
      assertEquals("-infinity", f);

      Object inf3 = row.getValue(7);
      assertTrue(inf3 instanceof LocalDateTime);
      assertEquals((LocalDateTime)inf3, LocalDateTime.MAX);
      f = f = wdt.formatDateTimeValue(inf3);
      assertEquals("infinity", f);

      Object inf4 = row.getValue(8);
      assertTrue(inf4 instanceof LocalDateTime);
      assertEquals((LocalDateTime)inf4, LocalDateTime.MIN);
      f = f = wdt.formatDateTimeValue(inf4);
      assertEquals("-infinity", f);

    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
      con.shutdown();
    }
  }

}
