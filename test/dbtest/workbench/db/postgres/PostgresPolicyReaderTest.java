/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.postgres;

import java.sql.SQLException;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.JdbcUtils;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.sql.parser.ScriptParser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresPolicyReaderTest
  extends WbTestCase
{

  private final static String TESTID = "poltest";

  public PostgresPolicyReaderTest()
  {
    super("PostgresPolicyReaderTest");
  }

	@Before
	public void setUpClass()
		throws Exception
	{
		PostgresTestUtil.initTestCase(TESTID);
	}

	@After
	public void tearDownClass()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase();
	}

  /**
   * Test of getTablePolicies method, of class PostgresPolicyReader.
   */
  @Test
  public void testGetTablePolicies() throws SQLException
  {
		WbConnection conn = PostgresTestUtil.getPostgresConnection();
    if (!JdbcUtils.hasMinimumServerVersion(conn, "10")) return;
    assertNotNull(conn);
    TestUtil.executeScript(conn,
      "create table " + TESTID + ".ptest (id integer, some_data text);\n" +
      "create policy ptest_select on " + TESTID + ".ptest\n" +
      "  for select\n" +
      "  using (id > 0);\n" +
      "create policy ptest_update on " + TESTID + ".ptest \n" +
      "  as restrictive  \n" +
      "  for update \n" +
      "  with check (some_data <> 'foo');\n" +
      "commit;"
    );

    TableIdentifier tbl = conn.getMetadata().findTable(new TableIdentifier(TESTID, "ptest"));
    String sql = tbl.getSource(conn).toString();

    System.out.println(sql);
    ScriptParser parser = ScriptParser.createScriptParser(conn);
    parser.setScript(sql);
    int size = parser.getSize();
    assertEquals(4, size);

    String alter = parser.getCommand(3);
    assertEquals("ALTER TABLE poltest.ptest DISABLE ROW LEVEL SECURITY", alter.trim());

    String select = parser.getCommand(1);
    System.out.println(select);
    assertEquals("CREATE POLICY ptest_select ON poltest.ptest\n" +
                 "  AS PERMISSIVE\n" +
                 "  FOR SELECT\n" +
                 "  USING (id > 0)", select.trim());

    String update = parser.getCommand(2);
    assertEquals("CREATE POLICY ptest_update ON poltest.ptest\n" +
                 "  AS RESTRICTIVE\n" +
                 "  FOR UPDATE\n" +
                 "  WITH CHECK (some_data <> 'foo'::text)", update.trim());

    TestUtil.executeScript(conn,
      "alter table ptest enable row level security;\n" +
      "commit;"
    );

    tbl = conn.getMetadata().findTable(new TableIdentifier(TESTID, "ptest"));
    sql = tbl.getSource(conn).toString();
    parser.setScript(sql);
    size = parser.getSize();
    assertEquals(4, size);

    alter = parser.getCommand(3);
    assertEquals("ALTER TABLE poltest.ptest ENABLE ROW LEVEL SECURITY", alter.trim());
  }

}
