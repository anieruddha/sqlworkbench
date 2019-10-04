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
package workbench.sql;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbAnnotationTest
{
	public WbAnnotationTest()
	{
	}

	@Test
	public void testGetAnnotationValue()
	{
		String sql = "/* test select */\nSELECT * FROM dummy;";
    WbAnnotation p = new ScrollAnnotation();
		String name = p.getAnnotationValue(sql);
		assertNull(name);

		sql = "/**@WbScrollTo end*/\nSELECT * FROM dummy;";
		name = p.getAnnotationValue(sql);
		assertEquals("end", name);

		sql = "\n\n  \n-- @WbScrollTo top \nSELECT * FROM dummy;";
		name = p.getAnnotationValue(sql);
		assertEquals("top", name);
	}

	@Test
	public void testGetAllAnnotations()
	{
		String sql = "\n  \n-- @" + MacroAnnotation.ANNOTATION + " name='someMacro'";
    List<WbAnnotation> annotations = WbAnnotation.readAllAnnotations(sql, new MacroAnnotation());
		assertNotNull(annotations);
		assertEquals(1, annotations.size());
		assertEquals("someMacro", annotations.get(0).getValue());

		sql =
      "\n\n" +
			"-- @" + MacroAnnotation.ANNOTATION + " name='someMacro'\n" +
			"-- @" + MacroAnnotation.ANNOTATION + " name=\"Another Macro\"\n";

		annotations = WbAnnotation.readAllAnnotations(sql, new MacroAnnotation());
		assertNotNull(annotations);
		assertEquals(2, annotations.size());
		assertEquals("someMacro", annotations.get(0).getValue());
		assertEquals("Another Macro", annotations.get(1).getValue());
	}

}