/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2018 Thomas Kellerer.
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
package workbench.db.oracle;

import java.sql.ResultSet;
import java.sql.Statement;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;
import workbench.db.exporter.RowDataConverter;
import workbench.db.exporter.TextRowDataConverter;

import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.reader.OracleRowDataReader;
import workbench.storage.reader.RowDataReader;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleRowDataConverterTest
  extends WbTestCase
{
  public OracleRowDataConverterTest()
  {
    super("OracleCompletionTest");
  }

  @BeforeClass
  public static void setUpClass()
    throws Exception
  {
    OracleTestUtil.initTestCase();

    WbConnection con = OracleTestUtil.getOracleConnection();
    assertNotNull(con);

    String sql =
      "CREATE TABLE ts_test (ts_value timestamp(6));\n" +
      "insert into ts_test (ts_value) values (timestamp '0001-01-01 00:00:00');\n" +
      "commit;\n";
    TestUtil.executeScript(con, sql, true);
  }

  @AfterClass
  public static void tearDownClass()
    throws Exception
  {
    OracleTestUtil.cleanUpTestCase();
  }

  @Test
  public void testTimestampFormatter()
    throws Exception
  {
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull(con);

    RowDataConverter converter = new TextRowDataConverter();
    converter.setDefaultTimestampFormat("yyyy-MM-dd HH:mm:ss");

    try (Statement stmt = con.createStatement();
      ResultSet rs = stmt.executeQuery("select * from ts_test");)
    {
      ResultInfo info = new ResultInfo(rs.getMetaData(), con);
      RowDataReader reader = new OracleRowDataReader(info, con);
      converter.setResultInfo(info);
      rs.next();
      RowData row = reader.read(rs, true);
      StringBuilder text = converter.convertRowData(row, 0);
      assertNotNull(text);
      assertEquals("0001-01-01 00:00:00", text.toString().trim());
    }
  }
}
