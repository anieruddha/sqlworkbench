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

package workbench.db.postgres;


import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;

import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresRangeTypeReaderTest
	extends WbTestCase
{

	public PostgresRangeTypeReaderTest()
	{
		super("RangeTypeTest");
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase();
	}


	@Test
	public void testRetrieveRangeTypes()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		assertNotNull(con);

		String sql =
			"create type timerange as range (subtype=time);\n" +
			"commit;";
		TestUtil.executeScript(con, sql);

		PostgresRangeTypeReader reader = new PostgresRangeTypeReader();
		List<PgRangeType> types = reader.getRangeTypes(con, null, null);
		assertEquals(1, types.size());
		PgRangeType type = types.get(0);
		assertEquals("timerange", type.getObjectName());
		CharSequence source = type.getSource(con);
		assertNotNull(source);
		assertEquals("CREATE TYPE timerange AS RANGE\n(\n  SUBTYPE = time without time zone\n);", source.toString());
	}

}
