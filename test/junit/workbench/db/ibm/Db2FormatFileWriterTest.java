/*
 * Db2FormatFileWriterTest.java
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
package workbench.db.ibm;

import java.io.IOException;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.exporter.DataExporter;
import workbench.db.exporter.RowDataConverter;

import workbench.storage.ResultInfo;
import workbench.storage.RowData;

import workbench.util.FileUtil;
import workbench.util.WbFile;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2FormatFileWriterTest
	extends WbTestCase
{

	public Db2FormatFileWriterTest()
	{
		super("Db2FormatFileWriterTest");
	}

	@Test
	public void testWriteFormatFile()
		throws IOException
	{
		ColumnIdentifier id = new ColumnIdentifier("ID", java.sql.Types.INTEGER);
		id.setDbmsType("INTEGER");

		ColumnIdentifier name = new ColumnIdentifier("NAME", java.sql.Types.VARCHAR);
		id.setDbmsType("VARCHAR(50)");

		ColumnIdentifier blob = new ColumnIdentifier("BINARY_DATA", java.sql.Types.BLOB);
		id.setDbmsType("BLOB");

		ResultInfo info = new ResultInfo(new ColumnIdentifier[] { id, name, blob} );
		info.setUpdateTable(new TableIdentifier("SOME_TABLE"));

		TestUtil util = new TestUtil("TestDb2Format");
		String dir = util.getBaseDir();
		final WbFile exportFile = new WbFile(dir, "test_export.txt");

		final RowDataConverter converter = new RowDataConverter()
		{
			@Override
			public StringBuilder convertRowData(RowData row, long rowIndex)
			{
				return null;
			}

			@Override
			public StringBuilder getStart()
			{
				return null;
			}

			@Override
			public StringBuilder getEnd(long totalRows)
			{
				return null;
			}

		};

		final DataExporter exporter = new DataExporter(null)
		{
			@Override
			public String getFullOutputFilename()
			{
				return exportFile.getFullPath();
			}

			@Override
			public String getTableNameToUse()
			{
				if (getTableName() != null)
				{
					return getTableName();
				}
				return converter.getResultInfo().getUpdateTable().getTableName();
			}
		};

		exporter.setTextDelimiter("\t");
		exporter.setDecimalSymbol(",");
		exporter.setEncoding("ISO-8859-1");
		exporter.setDateFormat("dd.mm.yyyy");
		exporter.setTableName("UNIT.TEST_TABLE");
		exporter.setWriteClobAsFile(false, -1);

		converter.setResultInfo(info);
		Db2FormatFileWriter instance = new Db2FormatFileWriter();

		instance.writeFormatFile(exporter, converter);
		WbFile controlFile = new WbFile(dir, "test_export.clp");
		assertTrue(controlFile.exists());
		String content = FileUtil.readFile(controlFile, "ISO-8859-1");
		assertTrue(content.contains("LOBS FROM ."));
		assertTrue(content.contains("coldelX09"));
		assertTrue(content.contains("IMPORT FROM test_export.txt OF DEL"));
		assertTrue(content.contains("INTO UNIT.TEST_TABLE"));
		assertTrue(content.contains("decpt=,"));
		assertTrue(content.contains("codepage=819"));

		exporter.setTableName(null);
		exporter.setEncoding("UTF-8");
		info = new ResultInfo(new ColumnIdentifier[] { id, name, } );
		info.setUpdateTable(new TableIdentifier("SOME_TABLE"));
		converter.setResultInfo(info);
		instance.writeFormatFile(exporter, converter);
		assertTrue(controlFile.exists());
		content = FileUtil.readFile(controlFile, "ISO-8859-1");
		assertFalse(content.contains("LOBS FROM"));
		assertTrue(content.contains("INTO SOME_TABLE"));
		assertTrue(content.contains("codepage=1208"));
	}
}
