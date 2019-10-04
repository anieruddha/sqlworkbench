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
package workbench.sql.generator;

import java.util.ArrayList;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;

import workbench.storage.RowData;
import workbench.storage.SqlLiteralFormatter;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresInsertGeneratorTest
  extends WbTestCase
{

  public PostgresInsertGeneratorTest()
  {
    super("PostgresInsertGeneratorTest");
  }

  /**
   * Test of buildFinalPart method, of class PostgresInsertGenerator.
   */
  @Test
  public void testCreateUpsert()
  {
    TableDefinition table = createTable();
    PostgresInsertGenerator generator = createGenerator(table.getTable(), table.getColumns());
    generator.setInsertType(InsertType.Upsert);

    {
      String sql = generator.createPreparedSQL(1);
      String expected =
        "INSERT INTO person\n" +
      "  (id,firstname,lastname)\n" +
      "VALUES\n" +
      "  (?,?,?)\n" +
      "ON CONFLICT (id)\n" +
      "DO UPDATE \n" +
      " SET firstname = EXCLUDED.firstname,\n" +
      "      lastname = EXCLUDED.lastname";

      assertEquals(expected, sql);
    }

    {
      String sql = generator.createPreparedSQL(5);
      String expected =
        "INSERT INTO person\n" +
      "  (id,firstname,lastname)\n" +
      "VALUES\n" +
      "  (?,?,?),\n" +
      "  (?,?,?),\n" +
      "  (?,?,?),\n" +
      "  (?,?,?),\n" +
      "  (?,?,?)\n" +
      "ON CONFLICT (id)\n" +
      "DO UPDATE \n" +
      " SET firstname = EXCLUDED.firstname,\n" +
      "      lastname = EXCLUDED.lastname";
      assertEquals(expected, sql);
    }

    generator.setInsertType(InsertType.InsertIgnore);
    {
      String sql = generator.createPreparedSQL(5);
      String expected =
        "INSERT INTO person\n" +
      "  (id,firstname,lastname)\n" +
      "VALUES\n" +
      "  (?,?,?),\n" +
      "  (?,?,?),\n" +
      "  (?,?,?),\n" +
      "  (?,?,?),\n" +
      "  (?,?,?)\n" +
      "ON CONFLICT DO NOTHING";
      assertEquals(expected, sql);
    }
  }


  @Test
  public void testCreateWithValues()
  {
    TableDefinition table = createTable();
    PostgresInsertGenerator generator = createGenerator(table.getTable(), table.getColumns());
    generator.setInsertType(InsertType.Upsert);

    generator.addRow(new RowData(new Object[] {Integer.valueOf(42), "Arthur", "Dent"}));
    SqlLiteralFormatter formatter = new SqlLiteralFormatter();
    String result = TestUtil.cleanupSql(generator.createLiteralSQL(formatter));
    String expected = "INSERT INTO person (id,firstname,lastname) VALUES (42,'Arthur','Dent') ON CONFLICT (id) DO UPDATE SET firstname = EXCLUDED.firstname, lastname = EXCLUDED.lastname";
    assertEquals(expected, result);
    generator.addRow(new RowData(new Object[] {Integer.valueOf(43), "Ford", "Prefect"}));
    generator.addRow(new RowData(new Object[] {Integer.valueOf(44), "Tricia", "McMillan"}));

    result = TestUtil.cleanupSql(generator.createLiteralSQL(formatter));
    expected =
      "INSERT INTO person (id,firstname,lastname) VALUES (42,'Arthur','Dent'), (43,'Ford','Prefect'), (44,'Tricia','McMillan')" +
      " ON CONFLICT (id) DO UPDATE SET firstname = EXCLUDED.firstname, lastname = EXCLUDED.lastname";
    assertEquals(expected, result);
  }

  private TableDefinition createTable()
  {
    TableIdentifier tbl = new TableIdentifier("person");
    List<ColumnIdentifier> columns = new ArrayList<>();

    ColumnIdentifier id = new ColumnIdentifier("id");
    id.setIsPkColumn(true);
    columns.add(id);
    columns.add(new ColumnIdentifier("firstname"));
    columns.add(new ColumnIdentifier("lastname"));
    return new TableDefinition(tbl, columns);
  }

  private PostgresInsertGenerator createGenerator(TableIdentifier tbl, List<ColumnIdentifier> columns)
  {
    return new PostgresInsertGenerator(tbl, columns)
    {
      @Override
      protected boolean is95()
      {
        return true;
      }
    };
  }
}
