/*
 * ResultInfoTest.java
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

import java.sql.Types;

import workbench.db.ColumnIdentifier;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ResultInfoTest
{

  @Test
  public void testAddColum()
  {
		ColumnIdentifier col1 = new ColumnIdentifier("id", java.sql.Types.INTEGER, true);
		ColumnIdentifier col2 = new ColumnIdentifier("lastname", java.sql.Types.VARCHAR, false);
		ResultInfo info = new ResultInfo(new ColumnIdentifier[] { col1, col2} );
    assertEquals(2, info.getColumnCount());
    info.addColumn(new ColumnIdentifier("firstname", Types.VARCHAR, 30));
    assertEquals(3, info.getColumnCount());
    assertEquals("id", info.getColumnName(0));
    assertEquals("lastname", info.getColumnName(1));
    assertEquals("firstname", info.getColumnName(2));
  }

	@Test
	public void testFindColumn()
		throws Exception
	{
		ColumnIdentifier col1 = new ColumnIdentifier("\"KEY\"", java.sql.Types.VARCHAR, true);
		ColumnIdentifier col2 = new ColumnIdentifier("\"Main Cat\"", java.sql.Types.VARCHAR, false);
		ColumnIdentifier col3 = new ColumnIdentifier("firstname", java.sql.Types.VARCHAR, false);
		ResultInfo info = new ResultInfo(new ColumnIdentifier[] { col1, col2, col3 } );
		assertEquals(3, info.getColumnCount());
		assertEquals(true, info.hasPkColumns());

		int index = info.findColumn("key");
		assertEquals(0, index);

		index = info.findColumn("\"KEY\"");
		assertEquals(0, index);

		index = info.findColumn("\"key\"");
		assertEquals(0, index);

		index = info.findColumn("\"Main Cat\"");
		assertEquals(1, index);

		index = info.findColumn("firstname");
		assertEquals(2, index);
	}


}
