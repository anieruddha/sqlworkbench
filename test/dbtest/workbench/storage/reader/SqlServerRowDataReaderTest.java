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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import workbench.WbTestCase;

import workbench.db.WbConnection;
import workbench.db.mssql.SQLServerTestUtil;

import workbench.storage.ResultInfo;
import workbench.storage.RowData;

import workbench.util.SqlUtil;

import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerRowDataReaderTest
  extends WbTestCase
{
  public SqlServerRowDataReaderTest()
  {
    super("SqlServerRowDataReaderTest");
  }

  @Test
  public void testReader()
    throws Exception
  {
		WbConnection con = SQLServerTestUtil.getSQLServerConnection();
		Assume.assumeNotNull(con);

    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      stmt = con.createStatement();
      rs = stmt.executeQuery(
        "select cast('2019-04-05 19:20:21' as datetime) as ts,\n" +
        "       cast('2019-04-05 19:20:21.000+00:00' as DateTimeOffset) as dto, \n" +
        "       cast('2019-04-05' as date)");
      ResultInfo info = new ResultInfo(rs.getMetaData(), con);
      SqlServerRowDataReader reader = new SqlServerRowDataReader(info, con, true);
      rs.next();
      RowData row = reader.read(rs, false);
      assertNotNull(row);
      Object ldt = row.getValue(0);
      assertTrue(ldt instanceof LocalDateTime);
      assertEquals(LocalDateTime.of(2019,4,5,19,20,21), (LocalDateTime)ldt);

      Object dt = row.getValue(1);
      assertTrue(dt instanceof OffsetDateTime);
      OffsetDateTime odt = OffsetDateTime.of(2019, 04, 05, 19, 20, 21, 0, ZoneOffset.ofHours(0));
      assertEquals(odt, (OffsetDateTime)dt);

      Object d = row.getValue(2);
      assertTrue(d instanceof LocalDate);
      assertEquals(LocalDate.of(2019, 4, 5), (LocalDate)d);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
      con.shutdown();
    }

  }

}
