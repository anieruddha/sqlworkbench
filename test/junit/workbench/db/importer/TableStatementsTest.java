/*
 * TableStatementsTest.java
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
package workbench.db.importer;

import org.junit.Test;
import workbench.db.TableIdentifier;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class TableStatementsTest
{

	@Test
	public void testGetTableStatement()
	{
		TableIdentifier tbl = new TableIdentifier("tsch", "address");

		TableStatements stmt = new TableStatements("delete from ${table.name}", null);
		String sql = stmt.getPreStatement(tbl);
		assertEquals("delete from address", sql);
		assertNull(stmt.getPostStatement(tbl));

		stmt = new TableStatements("set identity insert ${table.expression} on", "set identity insert ${table.expression} off");
		assertEquals("set identity insert tsch.address on", stmt.getPreStatement(tbl));
		assertEquals("set identity insert tsch.address off", stmt.getPostStatement(tbl));

	}
}
