/*
 * OracleRowDataReaderTest.java
 *
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.storage.reader;

import java.sql.ResultSet;
import java.sql.Statement;
import java.time.OffsetDateTime;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;
import workbench.db.oracle.OracleTestUtil;

import workbench.util.SqlUtil;

import org.junit.AfterClass;
import org.junit.Test;

import workbench.storage.ResultInfo;
import workbench.storage.RowData;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleRowDataReaderTest
  extends WbTestCase
{

	public OracleRowDataReaderTest()
	{
    super("OracleRowDataReaderTest");
	}

  @AfterClass
  public static void tearDown()
    throws Exception
  {
    OracleTestUtil.cleanUpTestCase();
  }

  @Test
  public void testReadData()
    throws Exception
  {
    WbConnection conn = OracleTestUtil.getOracleConnection();
    assertNotNull(conn);
    TestUtil.executeScript(conn,
      "create table dt_test\n" +
      "(\n" +
      "  id integer, \n" +
      "  dt date, \n" +
      "  ts timestamp, " +
      "  tstz timestamp with time zone, \n" +
      "  tsltz timestamp with local time zone\n" +
      ");\n" +
      "insert into dt_test values \n" +
      "(1, date '206-01-01', timestamp '2016-01-01 14:15:16', timestamp '2016-01-01 14:15:16', timestamp '2016-01-01 14:15:16');\n" +
      "commit;");

    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      stmt = conn.createStatement();
      rs = stmt.executeQuery("select * from dt_test");
      ResultInfo info = new ResultInfo(rs.getMetaData(), conn);
      OracleRowDataReader reader = new OracleRowDataReader(info, conn, true);
      rs.next();
      RowData row = reader.read(rs, false);
      assertNotNull(row);
      assertEquals(1, ((Number)row.getValue(0)).intValue());
      assertTrue(row.getValue(1) instanceof java.sql.Timestamp);
      assertTrue(row.getValue(2) instanceof java.sql.Timestamp);
      assertTrue(row.getValue(3) instanceof OffsetDateTime);
      assertTrue(row.getValue(4) instanceof java.sql.Timestamp);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
  }

}
