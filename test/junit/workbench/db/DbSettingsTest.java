/*
 * DbSettingsTest.java
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
package workbench.db;

import java.util.List;

import workbench.WbTestCase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Kellerer
 */
public class DbSettingsTest
	extends WbTestCase
{

	public DbSettingsTest()
	{
		super("DbSettingsTest");
	}

  @Test
  public void testAlias()
  {
    DbSettings maria = new DbSettings("mariadb");
    DbSettings mysql = new DbSettings("mysql");

    List<String> types = maria.getListProperty("additional.tabletypes");
    assertNotNull(types);
    assertFalse(types.isEmpty());

    List<String> types2 = mysql.getListProperty("additional.tabletypes");
    assertTrue(types2.isEmpty());

    boolean wildcards = maria.supportsMetaDataWildcards();
    assertFalse(wildcards);
  }

	@Test
	public void testGetIdentifierCase()
	{
		DbSettings test = new DbSettings("dummy");

		IdentifierCase idCase = test.getObjectNameCase();
		assertEquals(IdentifierCase.unknown, idCase);

		test.setObjectNameCase("mixed");
		idCase = test.getObjectNameCase();
		assertEquals(IdentifierCase.mixed, idCase);

		test.setObjectNameCase("gaga");
		idCase = test.getObjectNameCase();
		assertEquals(IdentifierCase.unknown, idCase);

		test.setObjectNameCase("lower");
		idCase = test.getObjectNameCase();
		assertEquals(IdentifierCase.lower, idCase);
	}

	@Test
	public void testTruncate()
	{
		DbSettings db = new DbSettings("oracle");
		assertTrue(db.supportsTruncate());
		assertFalse(db.supportsCascadedTruncate());
		assertFalse(db.truncateNeedsCommit());

		db = new DbSettings("postgresql");
		assertTrue(db.supportsTruncate());
		assertTrue(db.supportsCascadedTruncate());
		assertTrue(db.truncateNeedsCommit());

		db = new DbSettings("microsoft_sql_server");
		assertTrue(db.supportsTruncate());
		assertFalse(db.supportsCascadedTruncate());
		assertTrue(db.truncateNeedsCommit());

		db = new DbSettings("mysql");
		assertTrue(db.supportsTruncate());

		db = new DbSettings("h2");
		assertTrue(db.supportsTruncate());

		db = new DbSettings("hsql_database_engine");
		assertTrue(db.supportsTruncate());

		db = new DbSettings("apache_derby");
		assertTrue(db.supportsTruncate());

	}

}
