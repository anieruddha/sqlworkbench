/*
 * WbConnectTest.java
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
package workbench.sql.wbcommands;

import java.util.Collection;

import workbench.AppArguments;
import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;

import workbench.sql.BatchRunner;

import workbench.util.ArgumentParser;
import workbench.util.FileUtil;
import workbench.util.WbFile;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbConnectTest
  extends WbTestCase
{

  public WbConnectTest()
  {
    super("WbConnectTest");
  }

  @Test
  public void testConnectionRequired()
  {
    WbConnect cmd = new WbConnect();
    assertFalse(cmd.isConnectionRequired());
  }

  @Test
  public void testExecute()
    throws Exception
  {
    String script = "CREATE TABLE person ( nr integer primary key, firstname varchar(20), lastname varchar(20));\n" +
      "commit;\n " +
      "insert into person (nr, firstname, lastname) values (1, 'Arthur', 'Dent');\n " +
      "insert into person (nr, firstname, lastname) values (2, 'Zaphod', 'Beeblebrox');\n " +
      "insert into person (nr, firstname, lastname) values (3, 'Ford', 'Prefect');\n " +
      "insert into person (nr, firstname, lastname) values (4, 'Tricia', 'McMillian');\n " +
      "commit;";

    TestUtil util = getTestUtil();
    util.emptyBaseDirectory();

    try
    {
      WbConnection con1 = util.getConnection("connect1");
      TestUtil.executeScript(con1, script);

      WbConnection con2 = util.getConnection("connect2");
      TestUtil.executeScript(con2, script);
      TestUtil.executeScript(con2, "delete from person where nr in (3,4);\ncommit;\n");

      String batch =
        "WbConnect -profile='connect1';\n" +
        "WbExport -sourceTable=person -type=text -file=person1.txt;\n" +
        "WbConnect -profile='connect2';\n" +
        "WbExport -sourceTable=person -type=text -file=person2.txt;\n";

      WbFile sqlscript = new WbFile(util.getBaseDir(), "connect-test.sql");
      FileUtil.writeString(sqlscript, batch);

      ArgumentParser parser = new AppArguments();
      parser.parse("-script='" + sqlscript.getFullPath() + "'");

      BatchRunner runner = BatchRunner.createBatchRunner(parser);
      runner.execute();
      assertTrue(runner.isSuccess());

      WbFile p1 = new WbFile(util.getBaseDir(), "person1.txt");
      assertTrue(p1.exists());

      WbFile p2 = new WbFile(util.getBaseDir(), "person2.txt");
      assertTrue(p2.exists());

      Collection<String> lines1 = TestUtil.readLines(p1);
      assertNotNull(lines1);
      assertEquals("Wrong number of lines", 5, lines1.size());

      Collection<String> lines2 = TestUtil.readLines(p2);
      assertNotNull(lines2);
      assertEquals("Wrong number of lines", 3, lines2.size());

      assertTrue(p1.delete());
      assertTrue(p2.delete());
    }
    finally
    {
      ConnectionMgr.getInstance().disconnectAll();
      ConnectionMgr.getInstance().clearProfiles();
      util.emptyBaseDirectory();
    }
  }

}
