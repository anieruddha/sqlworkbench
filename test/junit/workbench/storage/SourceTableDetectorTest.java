/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2019, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
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
package workbench.storage;


import workbench.WbTestCase;

import workbench.db.ColumnIdentifier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SourceTableDetectorTest
	extends WbTestCase
{

	public SourceTableDetectorTest()
	{
		super("SourceTableDetectorTest");
	}

	@Before
	public void setUp()
	{
	}

	@After
	public void tearDown()
	{
	}

  @Test
  public void testColumnsWithoutPrefix()
  {
    String sql =
      "SELECT some_column,\n" +
      "       some_id,\n" +
      "       other_id\n" +
      "FROM some_table\n" +
      "  JOIN other_table ON some_table.some_id = other_table.other_id \n" +
      "WHERE 1=1\n" +
      "AND some_table.xid = 42\n" +
      "--group by 1\n" +
      "order by some_id\n" +
      ";";

		ColumnIdentifier[] columns = new ColumnIdentifier[] { new ColumnIdentifier("some_column"), new ColumnIdentifier("some_id"), new ColumnIdentifier("other_id") };
		ResultInfo result = new ResultInfo(columns);
		SourceTableDetector detector = new SourceTableDetector();
		detector.checkColumnTables(sql, result, null);
    assertFalse(result.isColumnTableDetected());
  }

	@Test
	public void testCheckColumnTables()
	{
		String sql =
			"select f.fid, f.fdata, b.bdata, b.fid \n" +
			"from foo f \n" +
			"  join bar b on b.fid = f.fid";

		ColumnIdentifier[] columns = new ColumnIdentifier[] { new ColumnIdentifier("fid"), new ColumnIdentifier("fdata"), new ColumnIdentifier("bdata"), new ColumnIdentifier("fid") };

		ResultInfo result = new ResultInfo(columns);
		SourceTableDetector detector = new SourceTableDetector();
		detector.checkColumnTables(sql, result, null);

		assertTrue(result.isColumnTableDetected());
		assertEquals("foo", result.getColumn(0).getSourceTableName());
		assertEquals("foo", result.getColumn(1).getSourceTableName());
		assertEquals("bar", result.getColumn(2).getSourceTableName());
		assertEquals("bar", result.getColumn(3).getSourceTableName());

		sql =
			"select f.fid, f.fdata, b.bdata, b.bid \n" +
			"from foo f \n" +
			"  join bar b on b.bid = f.fid";

		columns = new ColumnIdentifier[] { new ColumnIdentifier("fid"), new ColumnIdentifier("fdata"), new ColumnIdentifier("bdata"), new ColumnIdentifier("bid") };

		result = new ResultInfo(columns);
		detector.checkColumnTables(sql, result, null);
		assertTrue(result.isColumnTableDetected());
		assertEquals("foo", result.getColumn(0).getSourceTableName());
		assertEquals("foo", result.getColumn(1).getSourceTableName());
		assertEquals("bar", result.getColumn(2).getSourceTableName());
		assertEquals("bar", result.getColumn(3).getSourceTableName());
	}

  @Test
  public void testFQN()
  {
		String sql =
			"select foo.fid, b.bdata \n" +
			"from public.foo \n" +
			"  join bar b on b.fid = f.fid";

		ColumnIdentifier[] columns = new ColumnIdentifier[] { new ColumnIdentifier("fid"), new ColumnIdentifier("bdata") };

		ResultInfo result = new ResultInfo(columns);
		SourceTableDetector detector = new SourceTableDetector();
		detector.checkColumnTables(sql, result, null);

		assertTrue(result.isColumnTableDetected());
		assertEquals("public.foo", result.getColumn(0).getSourceTableName());
		assertEquals("bar", result.getColumn(1).getSourceTableName());
  }
}
