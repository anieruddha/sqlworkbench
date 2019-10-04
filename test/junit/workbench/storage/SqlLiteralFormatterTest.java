/*
 * SqlLiteralFormatterTest.java
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
package workbench.storage;

import java.io.File;
import java.io.IOException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import workbench.WbTestCase;
import workbench.interfaces.DataFileWriter;

import workbench.db.ColumnIdentifier;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlLiteralFormatterTest
	extends WbTestCase
{

	public SqlLiteralFormatterTest()
	{
		super("SqlLiteralFormatterTest");
	}

	@Test
	public void testNVarchar()
	{
		SqlLiteralFormatter f = new SqlLiteralFormatter();
		ColumnIdentifier col = new ColumnIdentifier("SOME_STRING", Types.VARCHAR);
		ColumnData data = new ColumnData("hello", col);
		CharSequence literal = f.getDefaultLiteral(data);
		assertNotNull(literal);
		assertEquals("'hello'", literal.toString());

		col = new ColumnIdentifier("SOME_STRING", Types.NVARCHAR);
		data = new ColumnData("hello", col);
		literal = f.getDefaultLiteral(data);
		assertNotNull(literal);
		assertEquals("N'hello'", literal.toString());
	}

	@Test
	public void testGetJdbcLiteral()
	{
		SqlLiteralFormatter f = new SqlLiteralFormatter();
		f.setDateLiteralType(SqlLiteralFormatter.JDBC_DATE_LITERAL_TYPE);

    LocalDateTime ldt = LocalDateTime.of(2002, 04, 02, 14, 15, 16);

		java.sql.Time tm = java.sql.Time.valueOf(ldt.toLocalTime());
		ColumnIdentifier timecol = new ColumnIdentifier("TIME_COL", Types.TIME);
		ColumnData data = new ColumnData(tm, timecol);
		CharSequence literal = f.getDefaultLiteral(data);
		assertEquals("JDBC time incorrect", "{t '14:15:16'}", literal);

		java.sql.Date dt = java.sql.Date.valueOf(ldt.toLocalDate());
		ColumnIdentifier datecol = new ColumnIdentifier("DATE_COL", Types.DATE);
		data = new ColumnData(dt, datecol);
		literal = f.getDefaultLiteral(data);
		assertEquals("JDBC date incorrect", "{d '2002-04-02'}", literal);

		java.sql.Timestamp ts = java.sql.Timestamp.valueOf(ldt);
		ColumnIdentifier tscol = new ColumnIdentifier("TS_COL", Types.TIMESTAMP);
		data = new ColumnData(ts, tscol);
		literal = f.getDefaultLiteral(data);
		assertEquals("JDBC timestamp incorrect", "{ts '2002-04-02 14:15:16.000000'}", literal);

		ColumnIdentifier ldtcol = new ColumnIdentifier("TS_COL", Types.TIMESTAMP);
		data = new ColumnData(ldt, ldtcol);
		literal = f.getDefaultLiteral(data);
		assertEquals("JDBC timestamp incorrect", "{ts '2002-04-02 14:15:16.000000'}", literal);

    ColumnIdentifier dtcol = new ColumnIdentifier("DT_COL", Types.DATE);
		data = new ColumnData(ldt.toLocalDate(), dtcol);
		literal = f.getDefaultLiteral(data);
		assertEquals("JDBC date incorrect", "{d '2002-04-02'}", literal);
	}

	@Test
	public void testSQLServerLiteral()
	{
		SqlLiteralFormatter f = new SqlLiteralFormatter();
		f.setDateLiteralType("sqlserver");
    LocalDateTime ldt = LocalDateTime.of(2002, 04, 02, 14, 15, 16);
		ColumnIdentifier ldtcol = new ColumnIdentifier("TS_COL", Types.TIMESTAMP);
		ColumnData data = new ColumnData(ldt, ldtcol);
		String literal = f.getDefaultLiteral(data).toString();
		assertEquals("SQL Server timestamp incorrect", "convert(datetime, '2002-04-02 14:15:16.000', 120)", literal);
  }

	@Test
	public void testGetAnsiLiteral()
	{
		SqlLiteralFormatter f = new SqlLiteralFormatter();
		f.setDateLiteralType(SqlLiteralFormatter.ANSI_DATE_LITERAL_TYPE);

    LocalDateTime ldt = LocalDateTime.of(2002, 04, 02, 14, 15, 16);

		java.sql.Time tm = java.sql.Time.valueOf(ldt.toLocalTime());
		ColumnIdentifier timecol = new ColumnIdentifier("TIME_COL", Types.TIME);
		ColumnData data = new ColumnData(tm, timecol);
		CharSequence literal = f.getDefaultLiteral(data);
		assertEquals("ANSI time incorrect", "TIME '14:15:16'", literal);

		java.sql.Date dt = java.sql.Date.valueOf(ldt.toLocalDate());
		ColumnIdentifier datecol = new ColumnIdentifier("DATE_COL", Types.DATE);
		data = new ColumnData(dt, datecol);
		literal = f.getDefaultLiteral(data);
		assertEquals("ANSI date incorrect", "DATE '2002-04-02'", literal);

		java.sql.Timestamp ts = java.sql.Timestamp.valueOf(ldt);
		ColumnIdentifier tscol = new ColumnIdentifier("TS_COL", Types.TIMESTAMP);
		data = new ColumnData(ts, tscol);
		literal = f.getDefaultLiteral(data);
		assertEquals("ANSI timestamp incorrect", "TIMESTAMP '2002-04-02 14:15:16.000000'", literal);

		ColumnIdentifier ldtcol = new ColumnIdentifier("TS_COL", Types.TIMESTAMP);
		data = new ColumnData(ldt, ldtcol);
		literal = f.getDefaultLiteral(data);
		assertEquals("ANSI timestamp incorrect", "TIMESTAMP '2002-04-02 14:15:16.000000'", literal);

    ColumnIdentifier dtcol = new ColumnIdentifier("DT_COL", Types.DATE);
		data = new ColumnData(ldt.toLocalDate(), dtcol);
		literal = f.getDefaultLiteral(data);
		assertEquals("ANSI date incorrect", "DATE '2002-04-02'", literal);
	}

	@Test
	public void testGetOracleLiteral()
	{
		SqlLiteralFormatter f = new SqlLiteralFormatter();
		f.setDateLiteralType("oracle");

    LocalDateTime ldt = LocalDateTime.of(2002, 04, 02, 14, 15, 16);

		java.sql.Date dt = java.sql.Date.valueOf(ldt.toLocalDate());
		ColumnIdentifier datecol = new ColumnIdentifier("DATE_COL", Types.DATE);
		ColumnData data = new ColumnData(dt, datecol);
		CharSequence literal = f.getDefaultLiteral(data);
		assertEquals("Oracle date incorrect", "to_date('2002-04-02', 'YYYY-MM-DD')", literal);

		java.sql.Timestamp ts = java.sql.Timestamp.valueOf(ldt);
		ColumnIdentifier tscol = new ColumnIdentifier("TS_COL", Types.TIMESTAMP);
		data = new ColumnData(ts, tscol);
		literal = f.getDefaultLiteral(data);
		assertEquals("Oracle timestamp incorrect", "to_timestamp('2002-04-02 14:15:16.000000', 'YYYY-MM-DD HH24:MI:SS.FF')", literal);
	}

	@Test
	public void testClobAsFile()
	{
		SqlLiteralFormatter f = new SqlLiteralFormatter();
		ColumnIdentifier uid = new ColumnIdentifier("data", Types.CLOB);
		uid.setDbmsType("CLOB");
		ColumnData data = new ColumnData("blablabla", uid);
    DataFileWriter writer = new DataFileWriter()
    {
      @Override
      public File generateDataFileName(ColumnData column)
        throws IOException
      {
        return new File("data.txt");
      }

      @Override
      public long writeBlobFile(Object value, File outputFile)
        throws IOException
      {
        return -1;
      }

      @Override
      public void writeClobFile(String value, File outputFile, String encoding)
        throws IOException
      {
      }

      @Override
      public File getBaseDir()
      {
        return new File(getTestUtil().getBaseDir());
      }
    };

    f.setTreatClobAsFile(writer, "UTF-8", -1);
    String literal = f.getDefaultLiteral(data).toString();
    assertEquals("{$clobfile='data.txt' encoding='UTF-8'}", literal);
    f.setTreatClobAsFile(writer, "UTF-8", 4000);
    literal = f.getDefaultLiteral(data).toString();
    assertEquals("'" + data.getValue()+ "'", literal);
	}

	@Test
	public void testUUIDLiteral()
	{
		SqlLiteralFormatter f = new SqlLiteralFormatter();
		ColumnIdentifier uid = new ColumnIdentifier("uid", Types.OTHER);
		uid.setDbmsType("uuid");
		ColumnData data = new ColumnData("5b14ca52-3025-4c2e-8987-1c9f9d66acd5", uid);
		String literal = f.getDefaultLiteral(data).toString();
		assertEquals("'5b14ca52-3025-4c2e-8987-1c9f9d66acd5'", literal);
	}

	@Test
	public void testHstore()
	{
		SqlLiteralFormatter f = new SqlLiteralFormatter();
		ColumnIdentifier uid = new ColumnIdentifier("attributes", Types.OTHER);
		uid.setDbmsType("hstore");
    Map<String, String> map = new HashMap<>();
    map.put("key", "value");
		ColumnData data = new ColumnData(map, uid);
		String literal = f.getDefaultLiteral(data).toString();
		assertEquals("'\"key\"=>\"value\"'::hstore", literal);
	}
}
