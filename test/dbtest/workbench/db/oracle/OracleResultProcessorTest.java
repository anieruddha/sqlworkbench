/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2013, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
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
package workbench.db.oracle;

import java.sql.ResultSet;
import java.sql.Statement;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.JdbcUtils;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.sql.DelimiterDefinition;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;

import workbench.util.SqlUtil;

import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleResultProcessorTest
  extends WbTestCase
{

	public OracleResultProcessorTest()
	{
		super("OracleResultProcessorTest");
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

  @Test
	public void testImplicitResults()
		throws Exception
	{
		OracleTestUtil.initTestCase();
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull("Oracle not available", con);
    if (!JdbcUtils.hasMinimumServerVersion(con, "12.0")) return;

    Statement stmt = null;
    ResultSet rs = null;

    String sql =
      "declare\n" +
      "  c1 SYS_REFCURSOR;  \n" +
      "  c2 SYS_REFCURSOR;  \n" +
      "BEGIN\n" +
      "  OPEN c1 FOR \n" +
      "  select 42 as id, 'Arthur' as name from dual;\n" +
      "  DBMS_SQL.RETURN_RESULT(c1);\n" +
      "\n" +
      "  OPEN c2 FOR \n" +
      "  select 24 as id, 'Zaphod' as first_name, 'Beeblebrox' as last_name from dual;\n" +
      "  DBMS_SQL.RETURN_RESULT(c2);\n" +
      "END;";


    try
    {
      stmt = con.createStatement();

      StatementRunner runner = new StatementRunner();
      runner.setConnection(con);
      StatementRunnerResult result = runner.runStatement(sql);
      assertTrue(result.isSuccess());
      assertEquals(2, result.getDataStores().size());
      DataStore ds = result.getDataStores().get(0);
      assertEquals(2, ds.getColumnCount());
      assertEquals(1, ds.getRowCount());
      assertEquals(42, ds.getValueAsInt(0, 0, -1));
      assertEquals("Arthur", ds.getValueAsString(0, 1));

      ds = result.getDataStores().get(1);
      assertEquals(3, ds.getColumnCount());
      assertEquals(1, ds.getRowCount());
      assertEquals(24, ds.getValueAsInt(0, 0, -1));
      assertEquals("Zaphod", ds.getValueAsString(0, 1));
      assertEquals("Beeblebrox", ds.getValueAsString(0, 2));
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
  }

  @Test
	public void testEmbeddedResult()
		throws Exception
	{
		OracleTestUtil.initTestCase();
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull("Oracle not available", con);

		String sql =
      "create or replace function get_numbers(p_start number, p_end number)\n" +
      "    return sys_refcursor\n" +
      "as\n" +
      "    l_stmt   varchar2(32767);\n" +
      "    l_col    integer := 0;\n" +
      "    l_result sys_refcursor;\n" +
      "begin\n" +
      "    l_stmt := 'select ';\n" +
      "    for idx in p_start..p_end loop\n" +
      "        if l_col > 0 then \n" +
      "          l_stmt := l_stmt || ', ';\n" +
      "        end if;\n" +
      "        l_stmt := l_stmt || to_char(p_start + l_col);\n" +
      "        l_col := l_col + 1;\n" +
      "        l_stmt := l_stmt || ' as col_' || to_char(l_col);\n" +
      "    end loop;\n" +
      "    l_stmt := l_stmt || ' from dual';\n" +
      "    open l_result for l_stmt;\n" +
      "    return l_result;\n" +
      "end;\n" +
      "/";
		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER);

    Statement stmt = null;
    ResultSet rs = null;

    try
    {
      stmt = con.createStatement();

      StatementRunner runner = new StatementRunner();
      runner.setConnection(con);
      StatementRunnerResult result = runner.runStatement("select get_numbers(1,5) from dual");
      assertTrue(result.isSuccess());
      assertEquals(1, result.getDataStores().size());
      DataStore ds = result.getDataStores().get(0);
      assertEquals(5, ds.getColumnCount());

      for (int i=0; i < 5; i++)
      {
        assertEquals("COL_" + (i + 1), ds.getColumnName(i));
      }
    }
    finally
    {
      TestUtil.executeScript(con, "drop function get_numbers");
      SqlUtil.closeAll(rs, stmt);
    }
  }

}
