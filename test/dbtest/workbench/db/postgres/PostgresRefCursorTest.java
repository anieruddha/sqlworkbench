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
package workbench.db.postgres;

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.JdbcUtils;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.sql.commands.SelectCommand;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresRefCursorTest
  extends WbTestCase
{

  public PostgresRefCursorTest()
  {
    super("PostgresRefCursorTest");
  }

  @BeforeClass
  public static void setUp()
    throws Exception
  {
    WbConnection con = PostgresTestUtil.getPostgresConnection();
    assertNotNull(con);

    PostgresTestUtil.initTestCase("test_refcursor");

    TestUtil.executeScript(con,
      "create table person (id integer, first_name varchar(50), last_name varchar(50));\n" +
      "insert into person (id, first_name, last_name) values (1, 'Arthur', 'Dent');\n" +
      "insert into person (id, first_name, last_name) values (2, 'Ford', 'Prefect');\n" +
      "commit;\n" +
      "CREATE OR REPLACE FUNCTION refcursorfunc1() \n" +
      "  RETURNS refcursor \n" +
      "  LANGUAGE plpgsql \n" +
      "AS \n" +
      "$body$ \n" +
      "DECLARE  \n" +
      "    c1 refcursor;  \n" +
      " BEGIN  \n" +
      "    OPEN c1 FOR SELECT * FROM person ORDER BY id; \n" +
      "    RETURN c1;  \n" +
      " END \n" +
      "$body$;\n" +
      "\n" +
      "CREATE OR REPLACE FUNCTION refcursorfunc2() \n" +
      "  RETURNS setof refcursor \n" +
      "  LANGUAGE plpgsql \n" +
      "AS \n" +
      "$body$ \n" +
      "DECLARE  \n" +
      "    c1 refcursor;  \n" +
      "    c2 refcursor;  \n" +
      "BEGIN  \n" +
      "    OPEN c1 FOR SELECT * FROM person where id = 1;\n" +
      "    RETURN NEXT c1;  \n" +
      "    OPEN c2 FOR SELECT * FROM person where id = 2;\n" +
      "    RETURN NEXT c2;  \n" +
      "END \n" +
      "$body$;\n" +
      "commit;"
    );
  }

  @AfterClass
  public static void tearDown()
    throws Exception
  {
    PostgresTestUtil.cleanUpTestCase();
  }

  @Test
  public void testSingleRefCursor()
    throws Exception
  {
    WbConnection con = PostgresTestUtil.getPostgresConnection();
    assertNotNull(con);
    // RefCursors only work with autocommit off!
    con.setAutoCommit(false);

    StatementRunner runner = getTestUtil().createConnectedStatementRunner(con);
    SelectCommand select = new SelectCommand();
    select.setStatementRunner(runner);
    select.setConnection(con);
    StatementRunnerResult result = select.execute("select * from refcursorfunc1()");
    assertTrue(result.isSuccess());
    List<DataStore> data = result.getDataStores();
    assertNotNull(data);
    assertEquals(1, data.size());
    DataStore ds = data.get(0);
    assertEquals(2, ds.getRowCount());
    assertEquals(1, ds.getValueAsInt(0, 0, -1));
    assertEquals("Arthur", ds.getValueAsString(0, 1));
    assertEquals(2, ds.getValueAsInt(1, 0, -1));
  }

  @Test
  public void testSimpleMutlipeRefCursor()
    throws Exception
  {

    WbConnection con = PostgresTestUtil.getPostgresConnection();
    assertNotNull(con);
    // RefCursors only work with autocommit off!
    con.setAutoCommit(false);

    StatementRunner runner = getTestUtil().createConnectedStatementRunner(con);
    SelectCommand select = new SelectCommand();
    select.setStatementRunner(runner);
    select.setConnection(con);
    StatementRunnerResult result = select.execute("select * from refcursorfunc2()");
    assertTrue(result.isSuccess());
    List<DataStore> data = result.getDataStores();
    assertNotNull(data);
    assertEquals(2, data.size());

    DataStore p1 = data.get(0);
    assertEquals(1, p1.getRowCount());
    assertEquals(1, p1.getValueAsInt(0, 0, -1));
    assertEquals("Arthur", p1.getValueAsString(0, 1));

    DataStore p2 = data.get(1);

    assertEquals(2, p2.getValueAsInt(0, 0, -1));
  }

  @Test
  public void testPg11Call()
    throws Exception
  {
    WbConnection con = PostgresTestUtil.getPostgresConnection();
    if (!JdbcUtils.hasMinimumServerVersion(con, "11.0")) return;

    String proc =
      "create procedure doit(p_one inout refcursor, p_two inout refcursor)\n" +
      "as\n" +
      "$$\n" +
      "begin\n" +
      "  open p_one for select * from (values (1,'Heart Of Gold'),(2,'Spaceship Titanic') ) as t(sid, ship);\n" +
      "  open p_two for select * from (values (42,'Arthur'),(43,'Marvin') ) as t(pid, name);\n" +
      "end;\n" +
      "\n" +
      "$$\n" +
      "language plpgsql;";

    TestUtil.executeScript(con, proc);

    // RefCursors only work with autocommit off!
    con.setAutoCommit(false);

    StatementRunner runner = getTestUtil().createConnectedStatementRunner(con);

    StatementRunnerResult result = runner.runStatement("call doit(null, null)");
    assertNotNull(result);
    assertTrue(result.isSuccess());
    List<DataStore> data = result.getDataStores();
    assertNotNull(data);
    assertEquals(2, data.size());

    DataStore ships = data.get(0);
    assertNotNull(ships);
    assertEquals(2, ships.getRowCount());
    assertEquals(1, ships.getValueAsInt(0, 0, -1));
    assertEquals("Heart Of Gold", ships.getValueAsString(0, 1));

    DataStore person = data.get(1);
    assertNotNull(person);
    assertEquals(2, person.getRowCount());
    assertEquals(42, person.getValueAsInt(0, 0, -1));
    assertEquals("Arthur", person.getValueAsString(0, 1));
  }
}
