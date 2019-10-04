/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2019, Thomas Kellerer
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

package workbench.db.mssql;

import java.sql.SQLException;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ConnectionMgr;
import workbench.db.DropType;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerTableSourceBuilderTest
	extends WbTestCase
{

	public SqlServerTableSourceBuilderTest()
	{
		super("SqlServerTableSource");
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		WbConnection con = SQLServerTestUtil.getSQLServerConnection();
		Assume.assumeNotNull("No connection available", con);
		SQLServerTestUtil.dropAllObjects(con);
		ConnectionMgr.getInstance().disconnect(con);
	}

	@Test
	public void testGetPkSource()
		throws SQLException
	{
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
    assertNotNull(conn);
		String sql =
			"create table foo \n" +
			"( \n" +
			"  id integer not null, \n" +
			"  some_col integer, \n" +
			"  primary key nonclustered (id) \n" +
			");\n" +
			"commit;";
		TestUtil.executeScript(conn, sql);
		TableIdentifier tbl = conn.getMetadata().findTable(new TableIdentifier("foo"));
		SqlServerTableSourceBuilder builder = new SqlServerTableSourceBuilder(conn);
		String source = builder.getTableSource(tbl, DropType.none, false);
		assertTrue(source.contains("PRIMARY KEY NONCLUSTERED (id)"));

		tbl.setUseInlinePK(true);
		source = builder.getTableSource(tbl, DropType.none, false);
		assertTrue(source.contains("PRIMARY KEY NONCLUSTERED (id)"));
	}

	@Test
	public void testComputedColumns()
		throws SQLException
	{
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
    assertNotNull(conn);
		String sql =
				"create table sales \n" +
				"( \n" +
				"   pieces integer, \n" +
				"   single_price numeric(19,2), \n" +
				"   total_price as (pieces * single_price), \n" +
				"   avg_price as (single_price / pieces) persisted \n" +
				")";
		TestUtil.executeScript(conn, sql);
		TableIdentifier tbl = conn.getMetadata().findTable(new TableIdentifier("sales"));
		SqlServerTableSourceBuilder builder = new SqlServerTableSourceBuilder(conn);
		String source = builder.getTableSource(tbl, DropType.none, false);
		assertTrue(source.contains("AS ([pieces]*[single_price])"));
		assertTrue(source.contains("AS ([single_price]/[pieces]) PERSISTED"));
	}

}
