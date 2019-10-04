/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2018 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
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
package workbench.sql.parser;

import workbench.WbTestCase;

import workbench.sql.DelimiterDefinition;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresScriptParserTest
  extends WbTestCase
{

  @Test
  @Ignore(value = "Not yet implemented")
  public void testDollarQuoting()
  {
    String sql = "select format($ddl$drop index %I;$ddl$, index_name) from pg_indexes";
    ScriptParser parser = new ScriptParser(ParserType.Postgres);
    parser.setScript(sql);
    int count = parser.getStatementCount();
    assertEquals(1, count);
    assertTrue(parser.getCommand(0).startsWith("select"));
  }

  @Test
  public void testNestedDollarQuoting()
  {
    String sql =
      "CREATE OR REPLACE FUNCTION outer()  \n" +
      "  RETURNS void  \n" +
      "AS  \n" +
      "$outer$ \n" +
      "DECLARE  \n" +
      "  s text \n" +
      "BEGIN \n" +
      "  CREATE OR REPLACE FUNCTION inner()  \n" +
      "    RETURNS text AS $inner$ \n" +
      "  BEGIN \n" +
      "    RETURN 'inner' \n" +
      "  END \n" +
      "  $inner$ language plpgsql \n" +
      " \n" +
      "  SELECT inner() INTO s \n" +
      "  RAISE NOTICE '%', s \n" +
      " \n" +
      "  DROP FUNCTION inner() \n" +
      "END \n" +
      "$outer$  \n" +
      "language plpgsql;\n" +
      "commit;\n";

    ScriptParser parser = new ScriptParser(ParserType.Postgres);
    parser.setScript(sql);
    int count = parser.getStatementCount();
    assertEquals(2, count);
    assertTrue(parser.getCommand(0).startsWith("CREATE"));
    assertTrue(parser.getCommand(1).equals("commit"));
  }

  @Test
  public void testPgCopy()
  {
    String script =
      "truncate table foo;\n" +
      "copy foo from stdin;\n" +
      "1,foo\n" +
      "2,bar\n" +
      "\\.\n" +
      "commit;";
    ScriptParser parser = new ScriptParser(ParserType.Postgres);
    parser.setScript(script);
    int count = parser.getStatementCount();
    assertEquals(3, count);
//    System.out.println(parser.getCommand(1));
    assertEquals("truncate table foo", parser.getCommand(0));
    assertEquals("commit", parser.getCommand(2));
  }

  @Test
  public void testPgScript()
  {
    String script =
      "create table one (id integer)\n" +
      "/?\n" +
      "\n" +
      "create table two (id integer)\n" +
      "/?\n" +
      "\n" +
      "do $$ \n" +
      "declare \n" +
      "    selectrow record; \n" +
      "begin \n" +
      "  for selectrow in \n" +
      "      select 'ALTER TABLE '|| t.table_name || ' ADD COLUMN foo integer NULL' as script\n" +
      "      from information_schema.tables t \n" +
      "      where t.table_schema = 'stuff' \n" +
      "  loop \n" +
      "    execute selectrow.script;\n" +
      "  end loop\n" +
      "end;\n" +
      "$$\n" +
      "/?\n";

    ScriptParser parser = new ScriptParser(script, ParserType.Postgres);
    parser.setAlternateDelimiter(new DelimiterDefinition("/?"));
    int count = parser.getStatementCount();
    assertEquals(3, count);
    assertEquals("create table one (id integer)", parser.getCommand(0));
    assertEquals("create table two (id integer)", parser.getCommand(1));
//		System.out.println(parser.getCommand(2));
    assertFalse(parser.getCommand(2).contains("/?"));
    assertTrue(parser.getCommand(2).trim().endsWith("$$"));

    script =
      "create table one (id integer)\n" +
      "GO\n" +
      "\n" +
      "create table two (id integer)\n" +
      "GO\n" +
      "\n" +
      "do $$ \n" +
      "declare \n" +
      "    selectrow record; \n" +
      "begin \n" +
      "  for selectrow in \n" +
      "      select 'ALTER TABLE '|| t.table_name || ' ADD COLUMN foo integer NULL' as script\n" +
      "      from information_schema.tables t \n" +
      "      where t.table_schema = 'stuff' \n" +
      "  loop \n" +
      "    execute selectrow.script;\n" +
      "  end loop\n" +
      "end;\n" +
      "$$\n" +
      "GO\n";

    parser = new ScriptParser(script, ParserType.Postgres);
    parser.setAlternateDelimiter(DelimiterDefinition.DEFAULT_MS_DELIMITER);
    count = parser.getStatementCount();
    assertEquals(3, count);
    assertEquals("create table one (id integer)", parser.getCommand(0));
    assertEquals("create table two (id integer)", parser.getCommand(1));
    assertFalse(parser.getCommand(2).contains("GO"));
    assertTrue(parser.getCommand(2).trim().endsWith("$$"));
  }

}
