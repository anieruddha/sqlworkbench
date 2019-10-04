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
public class DefaultInsertGeneratorTest
  extends WbTestCase
{

  public DefaultInsertGeneratorTest()
  {
    super("InsertGeneratorTest");
  }

  @Test
  public void testCreatePreparedSQL()
  {
    TableDefinition def = createTable();
    DefaultInsertGenerator gen = new DefaultInsertGenerator(def.getTable(), def.getColumns());

    String result = TestUtil.cleanupSql(gen.createPreparedSQL(1));
    assertEquals("INSERT INTO person (id,firstname,lastname) VALUES (?,?,?)", result);
    result = TestUtil.cleanupSql(gen.createPreparedSQL(4));
    assertEquals("INSERT INTO person (id,firstname,lastname) VALUES (?,?,?), (?,?,?), (?,?,?), (?,?,?)", result);
  }

  @Test
  public void testCreateWithValues()
  {
    TableDefinition def = createTable();
    DefaultInsertGenerator gen = new DefaultInsertGenerator(def.getTable(), def.getColumns());

    gen.addRow(new RowData(new Object[] {Integer.valueOf(42), "Arthur", "Dent"}));
    SqlLiteralFormatter formatter = new SqlLiteralFormatter();
    String result = TestUtil.cleanupSql(gen.createLiteralSQL(formatter));
    assertEquals("INSERT INTO person (id,firstname,lastname) VALUES (42,'Arthur','Dent')", result);
    gen.addRow(new RowData(new Object[] {Integer.valueOf(43), "Ford", "Prefect"}));
    gen.addRow(new RowData(new Object[] {Integer.valueOf(44), "Tricia", "McMillan"}));
    result = TestUtil.cleanupSql(gen.createLiteralSQL(formatter));
    assertEquals("INSERT INTO person (id,firstname,lastname) VALUES (42,'Arthur','Dent'), (43,'Ford','Prefect'), (44,'Tricia','McMillan')", result);
  }

  @Test
  public void testConstants()
  {
    TableDefinition def = createTable();
    DefaultInsertGenerator gen = new DefaultInsertGenerator(def.getTable(), def.getColumns());
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

}
