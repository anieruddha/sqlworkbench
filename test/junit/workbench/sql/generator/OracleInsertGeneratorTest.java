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

import workbench.WbTestCase;

import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleInsertGeneratorTest
  extends WbTestCase
{

  public OracleInsertGeneratorTest()
  {
    super("OracleInsertGeneratorTest");
  }

  @Test
  public void testInsertIgnore()
  {
    TableIdentifier tbl = new TableIdentifier("person");
    List<ColumnIdentifier> columns = new ArrayList<>();
    ColumnIdentifier id = new ColumnIdentifier("id");
    id.setIsPkColumn(true);
    columns.add(id);
    columns.add(new ColumnIdentifier("firstname"));
    columns.add(new ColumnIdentifier("lastname"));

    OracleInsertGenerator generator = new OracleInsertGenerator(tbl, columns);
    generator.setInsertType(InsertType.InsertIgnore);
    String sql = generator.createPreparedSQL(1);
    String expected =
      "INSERT /*+ IGNORE_ROW_ON_DUPKEY_INDEX (person(id)) */ INTO  person\n" +
      "  (id,firstname,lastname)\n" +
      "VALUES\n" +
      "  (?,?,?)";
    assertEquals(expected, sql);
  }

}
