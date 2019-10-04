/*
 * RegexModifierTest.java
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
package workbench.db.importer.modifier;

import workbench.db.ColumnIdentifier;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class RegexModifierTest
{
	public RegexModifierTest()
	{
	}

	@Test
	public void testModifyValue()
	{
		RegexModifier modifier = new RegexModifier();

		ColumnIdentifier fname = new ColumnIdentifier("fname");
		ColumnIdentifier lname = new ColumnIdentifier("lname");
		modifier.addDefinition(fname, "bronx", "brox");
		modifier.addDefinition(lname, "\\\"", "\\'");

		String modified = modifier.modifyValue(fname, "Zaphod Beeblebronx");
		assertEquals("Zaphod Beeblebrox", modified);

		modified = modifier.modifyValue(lname, "Zaphod Beeblebronx");
		assertEquals("Zaphod Beeblebronx", modified);

		modified = modifier.modifyValue(lname, "Test\" value");
	}

	@Test
	public void testMakeArray()
	{
		RegexModifier modifier = new RegexModifier();

		ColumnIdentifier list = new ColumnIdentifier("list_column");
		modifier.addDefinition(list, "(.+)", "\\{$1\\}");

		String modified = modifier.modifyValue(list, "a,b,c");
		assertEquals("{a,b,c}", modified);
	}
}
