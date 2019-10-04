/*
 * DbmsOutputTest.java
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
package workbench.db.oracle;

import java.sql.Statement;

import workbench.WbTestCase;

import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class DbmsOutputTest
	extends WbTestCase
{
	public DbmsOutputTest()
	{
		super("DbmsOutputTest");
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testOutput()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull(con);

		Statement stmt = null;
		try
		{
			stmt = con.createStatement();
			DbmsOutput output = new DbmsOutput(con.getSqlConnection());
			output.enable(-1);
			stmt.execute("begin\n dbms_output.put_line('Hello, World'); end;");
			String out = output.getResult();
			assertEquals("Hello, World", out.trim());

      stmt.execute(
        "begin\n " +
        "  dbms_output.put_line('Line 1 with some text');\n " +
        "  dbms_output.put_line('');\n " +
        "  dbms_output.put_line('And line two with some more text');\n " +
        "end;");
			out = output.getResult();
			assertEquals("Line 1 with some text\n\nAnd line two with some more text", out.trim());

			output.disable();
			stmt.execute("begin\n dbms_output.put_line('Hello, World'); end;");
			out = output.getResult();
			assertTrue(StringUtil.isEmptyString(out));

			output.close();
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
	}
}
