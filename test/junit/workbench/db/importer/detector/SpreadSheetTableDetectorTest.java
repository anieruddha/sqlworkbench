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
package workbench.db.importer.detector;

import java.io.File;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ColumnIdentifier;
import workbench.db.importer.ExcelReaderTest;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SpreadSheetTableDetectorTest
  extends WbTestCase
{

  public SpreadSheetTableDetectorTest()
  {
    super("SpreadSheetTableDetectorTest");
  }

  @Test
  public void testAnalyzeOneSheet()
    throws Exception
  {
    TestUtil util = getTestUtil();
		File input = util.copyResourceFile(ExcelReaderTest.class, "data.xls");

    SpreadSheetTableDetector detector = new SpreadSheetTableDetector(input, true, 0);
    detector.setSampleSize(100);
    detector.setTableName("person");
    detector.analyzeFile();
    List<ColumnIdentifier> columns = detector.getDBColumns();
    assertNotNull(columns);
    assertEquals(6,columns.size());
    String sql = detector.getCreateTable(null);
//    System.out.println(sql);
    String expected =
      "CREATE TABLE person\n" +
      "(\n" +
      "  id           DECIMAL(3),\n" +
      "  firstname    VARCHAR(32767),\n" +
      "  lastname     VARCHAR(32767),\n" +
      "  hiredate     TIMESTAMP,\n" +
      "  salary       DECIMAL(7),\n" +
      "  last_login   TIMESTAMP\n" +
      ");";
    assertEquals(expected.toLowerCase(), sql.trim().toLowerCase());
    assertTrue(input.delete());
  }

  @Test
  public void testAnalyzeAllSheets()
    throws Exception
  {
    TestUtil util = getTestUtil();
		File input = util.copyResourceFile(ExcelReaderTest.class, "data.xls");

    SpreadSheetTableDetector detector = new SpreadSheetTableDetector(input, true, -1);
    detector.setSampleSize(100);
    detector.setTableName("person");
    detector.analyzeFile();
    String sql = detector.getCreateTable(null);
    String expected =
      "CREATE TABLE person\n" +
      "(\n" +
      "  id           DECIMAL(3),\n" +
      "  firstname    VARCHAR(32767),\n" +
      "  lastname     VARCHAR(32767),\n" +
      "  hiredate     TIMESTAMP,\n" +
      "  salary       DECIMAL(7),\n" +
      "  last_login   TIMESTAMP\n" +
      ");\n" +
      "\n" +
      "CREATE TABLE orders\n" +
      "(\n" +
      "  customer_id   DECIMAL(3),\n" +
      "  order_id      DECIMAL(3),\n" +
      "  product_id    DECIMAL(3),\n" +
      "  amount        DECIMAL(5)\n" +
      ");";
    System.out.println("*****\n" + sql);
    assertEquals(expected.toLowerCase(), sql.trim().toLowerCase());
  }
}
