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
package workbench.db.hsqldb;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class HsqlTableSourceBuilderTest
  extends WbTestCase
{

  public HsqlTableSourceBuilderTest()
  {
    super("HsqlTableSourceBuilderTest");
  }

  @Test
  public void testReadTableOptions()
      throws Exception
  {
    WbConnection conn = getTestUtil().getHSQLConnection("systemperiod");
    TestUtil.executeScript(conn,
      "create table test_versioning \n" +
      "(\n" +
      "  id integer primary key, \n" +
      "  some_value varchar(100),\n" +
      "  valid_from timestamp GENERATED ALWAYS AS ROW START,\n" +
      "  valid_until timestamp GENERATED ALWAYS AS ROW END,\n" +
      "  period for system_time (valid_from, valid_until)\n" +
      ");\n" +
      "commit;\n");

    TableIdentifier tbl = conn.getMetadata().findObject(new TableIdentifier("TEST_VERSIONING"));
    String source = tbl.getSource(conn).toString();
    System.out.println(source);
    assertTrue(source.contains("PERIOD FOR SYSTEM_TIME (VALID_FROM, VALID_UNTIL)"));
  }

}
