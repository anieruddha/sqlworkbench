/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2017 Thomas Kellerer.
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
package workbench.sql.lexer;

import java.io.IOException;

import workbench.WbTestCase;

import org.junit.Test;

import static org.junit.Assert.*;


/**
 *
 * @author Thomas Kellerer
 */
public class PostgresLexerTest
  extends WbTestCase
{

  public PostgresLexerTest()
  {
  }

  @Test
  public void embeddedDoubleQuotes()
  {
    PostgresLexer lexer = new PostgresLexer("from \"foo\"\"bar\"");
    SQLToken token = lexer.getNextToken(false, false);
    token = lexer.getNextToken(false, false);
    assertNotNull(token);
    assertEquals("\"foo\"\"bar\"", token.getText());
    assertTrue(token.isIdentifier());

    lexer.setInput("from \"foobar\" as x");
    token = lexer.getNextToken(false, false); // from
    token = lexer.getNextToken(false, false);
    assertNotNull(token);
    assertEquals("\"foobar\"", token.getText());
    assertTrue(token.isIdentifier());

    token = lexer.getNextToken(false, false);
    assertNotNull(token);
    assertEquals("as", token.getText());
    token = lexer.getNextToken(false, false);
    assertNotNull(token);
    assertEquals("x", token.getText());

    lexer.setInput("from \";\" as x");
    token = lexer.getNextToken(false, false); // from
    token = lexer.getNextToken(false, false);
    System.out.println("token: " + token.getText());
    assertNotNull(token);
    assertEquals("\";\"", token.getText());
    assertTrue(token.isIdentifier());
  }

  @Test
  public void testIdentifiers()
  {
    PostgresLexer lexer = new PostgresLexer("select foo#>>'{one}' from table");
    SQLToken token = lexer.getNextToken(false, false);
    token = lexer.getNextToken(false, false);
    assertEquals("foo", token.getText());
    token = lexer.getNextToken(false, false);
    assertEquals("#>>", token.getText());
  }

  @Test
  public void testLiterals()
    throws IOException
  {
    PostgresLexer lexer = new PostgresLexer("select E'foobar'");
    SQLToken token = lexer.getNextToken(false, false);
    token = lexer.getNextToken(false, false);
    assertNotNull(token);
    assertEquals(SQLToken.LITERAL_STRING, token.getID());
    assertEquals("E'foobar'", token.getText());
    token = lexer.getNextToken(false, false);
    assertNull(token);

    lexer = new PostgresLexer("select E'\\\\xCAFEBABE'");
    token = lexer.getNextToken(false, false);
    token = lexer.getNextToken(false, false);
    assertNotNull(token);
    assertEquals(SQLToken.LITERAL_STRING, token.getID());
    assertEquals("E'\\\\xCAFEBABE'", token.getText());
    token = lexer.getNextToken(false, false);
    assertNull(token);

    lexer = new PostgresLexer("select E'\\foobar'");
    token = lexer.getNextToken(false, false);
    token = lexer.getNextToken(false, false);
    assertNotNull(token);
    assertEquals(SQLToken.LITERAL_STRING, token.getID());
    assertEquals("E'\\foobar'", token.getText());
    token = lexer.getNextToken(false, false);
    assertNull(token);
  }

}
