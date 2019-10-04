/*
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
package workbench.storage;

import java.sql.ResultSet;
import java.sql.Statement;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;
import workbench.db.postgres.PostgresTestUtil;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 *
 * @author Thomas Kellerer
 */
public class PgResultColumnMetaDataTest
  extends WbTestCase
{

	private static final String TEST_ID = "colmeta";

	public PgResultColumnMetaDataTest()
	{
		super(TEST_ID);
	}

  @BeforeClass
  public static void setUpClass()
    throws Exception
  {
    PostgresTestUtil.initTestCase(TEST_ID);
  }

  @AfterClass
  public static void tearDownClass()
    throws Exception
  {
    PostgresTestUtil.cleanUpTestCase();
  }

  @Test
  public void retrieveColumnRemarks()
    throws Exception
  {
		WbConnection con = PostgresTestUtil.getPostgresConnection();
    assertNotNull(con);

    String script =
      "create table t1 (id integer primary key, some_data text);\n" +
      "create table t2 (id integer primary key, t1_id integer);\n" +
      "\n" +
      "comment on column t1.id is 'The PK';\n" +
      "comment on column t1.some_data is 'Some T1 data';\n" +
      "comment on column t2.id is 'The other PK';\n" +
      "comment on column t2.t1_id is 'The FK column';";

    TestUtil.executeScript(con, script);

    String sql =
      "select x1.id, some_data, x2.id as id2, t1_id\n" +
      "from t1 as x1\n" +
      "  join t2 as x2 on x1.id = x2.t1_id;";

    try (Statement stmt = con.createStatement();
         ResultSet rs = stmt.executeQuery(sql);)
    {
      ResultInfo info = new ResultInfo(rs.getMetaData(), con);
      ResultColumnMetaData meta = new ResultColumnMetaData(sql, con);
      meta.retrieveColumnRemarks(info);
      assertEquals("The PK", info.getColumn(0).getComment());
      assertEquals("Some T1 data", info.getColumn(1).getComment());
      assertEquals("The other PK", info.getColumn(2).getComment());
      assertEquals("The FK column", info.getColumn(3).getComment());

    }
  }
}
