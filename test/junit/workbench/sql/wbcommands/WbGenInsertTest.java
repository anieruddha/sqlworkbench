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
package workbench.sql.wbcommands;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;

import workbench.sql.StatementRunnerResult;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbGenInsertTest
  extends WbTestCase
{

  public WbGenInsertTest()
  {
    super("GenerateInsert");
  }

  @After
  public void tearDown()
  {
    ConnectionMgr.getInstance().disconnectAll();
  }

  @Test
  public void testExecute()
    throws Exception
  {
    TestUtil util = getTestUtil();
    WbConnection conn = util.getConnection();

    String sql =
      "create table customer (cust_id integer not null primary key);\n" +
      "create table orders (order_id integer not null primary key, cust_id integer not null references customer);\n" +
      "create table order_item (item_id integer not null primary key, order_id integer not null references orders, currency_id integer not null);\n" +
      "commit;\n";
    TestUtil.executeScript(conn, sql);

    WbGenInsert genInsert = new WbGenInsert();
    genInsert.setConnection(conn);
    StatementRunnerResult result = genInsert.execute(genInsert.getVerb() + " -tables=customer,orders,order_item -fullInsert=true");
    assertTrue(result.isSuccess());
    String script = result.getMessages().toString();
    System.out.println(script);
    String expected =
      "INSERT INTO CUSTOMER\n" +
      "  (CUST_ID)\n" +
      "VALUES\n" +
      "  (CUST_ID_value);\n" +
      "\n" +
      "INSERT INTO ORDERS\n" +
      "  (ORDER_ID, CUST_ID)\n" +
      "VALUES\n" +
      "  (ORDER_ID_value, CUST_ID_value);\n" +
      "\n" +
      "INSERT INTO ORDER_ITEM\n" +
      "  (ITEM_ID, ORDER_ID, CURRENCY_ID)\n" +
      "VALUES\n" +
      "  (ITEM_ID_value, ORDER_ID_value, CURRENCY_ID_value);";
     assertEquals(expected, script.trim());
  }

  @Test
  public void testIsWbCommand()
  {
    WbGenInsert genInsert = new WbGenInsert();
    assertTrue(genInsert.isWbCommand());
  }

}
