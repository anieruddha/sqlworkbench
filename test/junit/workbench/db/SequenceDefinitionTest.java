/*
 * SequenceDefinitionTest.java
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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Kellerer
 */
public class SequenceDefinitionTest
{

	@Test
	public void testEquals()
	{
		SequenceDefinition def1 = new SequenceDefinition("public", "seq_one");
		def1.setSequenceProperty("INCREMENT", new Integer(1));
		SequenceDefinition def2 = new SequenceDefinition("public", "seq_two");
		def2.setSequenceProperty("INCREMENT", new Integer(1));
		assertEquals(def1.propertiesAreEqual(def2), true);

		def2.setSequenceProperty("CACHE", new Integer(50));
		assertEquals(def1.propertiesAreEqual(def2), false);
	}
}
