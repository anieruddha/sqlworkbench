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
package workbench.sql.lexer;

import java.io.Reader;

import workbench.db.WbConnection;

import workbench.sql.parser.ParserType;

import workbench.util.CharSequenceReader;

/**
 *
 * @author Thomas Kellerer
 */
public class SQLLexerFactory
{
	public static SQLLexer createLexer()
	{
		SQLLexer lexer = new StandardLexer("");
		return lexer;
	}

	public static SQLLexer createLexer(WbConnection conn)
	{
		ParserType type = ParserType.getTypeFromConnection(conn);
		return createLexer(type, "");
	}

	public static SQLLexer createLexer(CharSequence sql)
	{
		SQLLexer lexer = new StandardLexer(sql);
		return lexer;
	}

	public static SQLLexer createLexer(WbConnection conn, String sql)
	{
		ParserType type = ParserType.getTypeFromConnection(conn);
		return createLexer(type, sql);
	}

	public static SQLLexer createLexer(WbConnection conn, CharSequence sql)
	{
		ParserType type = ParserType.getTypeFromConnection(conn);
		return createLexer(type, sql);
	}

	public static SQLLexer createLexerForDbId(String dbId, CharSequence sql)
	{
		ParserType type = ParserType.getTypeFromDBID(dbId);
		return createLexer(type, sql);
	}

	public static SQLLexer createLexer(ParserType type, CharSequence sql)
	{
		return createLexer(type, new CharSequenceReader(sql));
	}

	public static SQLLexer createLexer(ParserType type, Reader input)
	{
		SQLLexer lexer;
		switch (type)
		{
			case MySQL:
				lexer = new MySQLLexer(input);
				break;
			case SqlServer:
				lexer = new SqlServerLexer(input);
				break;
			case Postgres:
				lexer = new PostgresLexer(input);
				break;
			default:
				lexer = new StandardLexer(input);
		}
		return lexer;
	}

	public static SQLLexer createNonStandardLexer(ParserType type, Reader input)
	{
    if (type == ParserType.Postgres)
    {
      return new PgEscapingLexer(input);
    }
		return new NonStandardLexer(input);
	}

	public static SQLLexer createNonStandardLexer(ParserType type, String sql)
	{
    if (type == ParserType.Postgres)
    {
      return new PgEscapingLexer(sql);
    }
		return new NonStandardLexer(sql);
	}

}
