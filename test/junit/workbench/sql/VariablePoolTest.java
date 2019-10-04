/*
 * VariablePoolTest.java
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
package workbench.sql;

import java.io.File;
import java.util.Set;

import workbench.AppArguments;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.resource.Settings;

import workbench.storage.DataStore;

import workbench.util.ArgumentParser;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class VariablePoolTest
	extends WbTestCase
{

	private final String prefix;
	private final String suffix;

	public VariablePoolTest()
	{
		super("VariablePoolTest");
    prefix = Settings.getInstance().getSqlParameterPrefix();
    suffix = Settings.getInstance().getSqlParameterSuffix();
	}

	@AfterClass
	public static void tearDown()
	{
		VariablePool.getInstance().reset();
	}

	@Before
	public void beforeTest()
	{
		VariablePool.getInstance().reset();
	}

	@Test
	public void testNoSuffix()
	{
		VariablePool pool = VariablePool.getInstance();
		pool.setPrefixSuffix(":", "");
		pool.setParameterValue("some_id", "1");
		String replaced = pool.replaceAllParameters("select * from foo where id = :some_id");
		assertEquals("select * from foo where id = 1", replaced);

		replaced = pool.replaceAllParameters("select * from foo where id =:some_id and col = 42");
		assertEquals("select * from foo where id =1 and col = 42", replaced);

		replaced = pool.replaceAllParameters("select * from foo where id >:some_id");
		assertEquals("select * from foo where id >1", replaced);

		pool.setParameterValue("other_id", "2");
		replaced = pool.replaceAllParameters("select * from foo where id in (:some_id, :other_id)");
		assertEquals("select * from foo where id in (1, 2)", replaced);

		pool.setParameterValue("some_name", "Dent");
		replaced = pool.replaceAllParameters("select * from person where lastname = ':some_name';");
		assertEquals("select * from person where lastname = 'Dent';", replaced);
	}

  @Test
  public void deleteVars()
  {
		VariablePool pool = VariablePool.getInstance();
    pool.setParameterValue("base_id", "42");
    pool.setParameterValue("base_name", "Arthur");
    pool.setParameterValue("some_var", "Tricia");
    pool.setParameterValue("some_varx", "Tricia");
    int deleted = pool.removeVariable("base*");
    assertEquals(2, deleted);
    assertFalse(pool.isDefined("base_id"));
    assertFalse(pool.isDefined("base_name"));
    assertTrue(pool.isDefined("some_var"));
    pool.setParameterValue("name_var", "Zaphod");
    deleted = pool.removeVariable("some_var");
    assertEquals(1, deleted);

    pool.setParameterValue("answer", "42");
    deleted = pool.removeVariable("*");
    assertEquals(3, deleted);
    assertEquals(0, pool.getParameterCount());

    pool.setParameterValue("cust_id", "42");
    pool.setParameterValue("person_id", "42");
    pool.setParameterValue("id_value", "42");
    deleted = pool.removeVariable("*_id");
    assertEquals(2, deleted);
    assertTrue(pool.isDefined("id_value"));
    assertFalse(pool.isDefined("cust_id"));
    assertFalse(pool.isDefined("person_id"));
  }

	@Test
	public void testRemoveVars()
	{
		VariablePool pool = VariablePool.getInstance();
		String result = pool.removeVariables("42" + prefix + "foo" + suffix);
		assertEquals("42", result);

		result = pool.removeVariables("" + prefix + "foo" + suffix + "42");
		assertEquals("42", result);

		result = pool.removeVariables("" + prefix + "?foo" + suffix + "42");
		assertEquals("42", result);

		result = pool.removeVariables("4" + prefix + "&foo" + suffix + "2");
		assertEquals("42", result);
	}

	@Test
	public void testInitFromCommandLine()
		throws Exception
	{
		TestUtil util = getTestUtil();
		VariablePool pool = VariablePool.getInstance();

		ArgumentParser p = new ArgumentParser();
		p.addArgument(AppArguments.ARG_VARDEF);
		p.parse("-" + AppArguments.ARG_VARDEF + "='#exportfile=/user/home/test.txt'");
		pool.readDefinition(p.getValue(AppArguments.ARG_VARDEF), false);
		assertEquals("Wrong parameter retrieved from commandline", "/user/home/test.txt", pool.getParameterValue("exportfile"));

		File f = new File(util.getBaseDir(), "vars.properties");

    TestUtil.writeFile(f,
      "exportfile=/user/home/export.txt\n" +
      "exporttable=person\n");
		pool.clear();
		p.parse("-" + AppArguments.ARG_VARDEF + "='" + f.getAbsolutePath() + "'");
		pool.readDefinition(p.getValue(AppArguments.ARG_VARDEF), false);
		assertEquals("Wrong parameter retrieved from file", "/user/home/export.txt", pool.getParameterValue("exportfile"));
		assertEquals("Wrong parameter retrieved from file", "person", pool.getParameterValue("exporttable"));
	}

	@Test
	public void testInitFromProperties()
		throws Exception
	{
		VariablePool pool = VariablePool.getInstance();
		System.setProperty(VariablePool.PROP_PREFIX + "testvalue", "value1");
		System.setProperty(VariablePool.PROP_PREFIX + "myprop", "value2");
		System.setProperty("someprop.testvalue", "value2");

		pool.initFromProperties(System.getProperties());
		assertEquals("Wrong firstvalue", "value1", pool.getParameterValue("testvalue"));
		assertEquals("Wrong firstvalue", "value2", pool.getParameterValue("myprop"));
	}

	@Test
	public void testPool()
		throws Exception
	{
		TestUtil util = getTestUtil();
		util.prepareEnvironment();

		VariablePool pool = VariablePool.getInstance();

		pool.setParameterValue("id", "42");

		String value = pool.getParameterValue("id");
		assertEquals("Wrong value stored", "42", value);

		String sql = "select * from test where id=" + prefix + "id" + suffix;
		String realSql = pool.replaceAllParameters(sql);
		assertEquals("Parameter not replaced", "select * from test where id=42", realSql);

		sql = "select * from test where id=" + prefix + "?id" + suffix;
		boolean hasPrompt = pool.hasPrompt(sql);
		assertEquals("Prompt not detected", true, hasPrompt);

		sql = "select * from test where id=" + prefix + "&id" + suffix;
		Set<String> vars = pool.getVariablesNeedingPrompt(sql);
		assertEquals("Prompt not detected", 0, vars.size());

		pool.removeVariable("id");
		vars = pool.getVariablesNeedingPrompt(sql);
		assertEquals("Prompt not detected", 1, vars.size());
		assertEquals("Variable not in prompt pool", true, vars.contains("id"));

		File f = new File(util.getBaseDir(), "vardef.props");

    TestUtil.writeFile(f,
      "lastname=Dent\n" +
       "firstname=Arthur");
		pool.readFromFile(f.getAbsolutePath(), null, false);

		value = pool.getParameterValue("lastname");
		assertEquals("Lastname not defined", "Dent", value);
		value = pool.getParameterValue("firstname");
		assertEquals("Firstname not defined", "Arthur", value);
		assertTrue(f.delete());
	}

	@Test
	public void testDataStore()
		throws Exception
	{
		VariablePool pool = VariablePool.getInstance();
		pool.clear();
		DataStore ds = pool.getVariablesDataStore();
		assertEquals(0, ds.getRowCount());
		int row = ds.addRow();
		ds.setValue(row, 0, "varname");
		ds.setValue(row, 1, "value");
		ds.updateDb(null, null);
		assertEquals(1, pool.getParameterCount());
		assertEquals("value", pool.getParameterValue("varname"));
	}

	@Test
	public void testAlternatePrefix()
	{
		VariablePool pool = VariablePool.getInstance();
		pool.setPrefixSuffix("${", "}");
		pool.setParameterValue("foo.bar.value", "1");
		String sql = "select * from foo where bar = ${foo.bar.value}";
		String replaced = pool.replaceAllParameters(sql);
		assertEquals("select * from foo where bar = 1", replaced);
	}

  @Test
  public void testNestedReplacements()
  {
		VariablePool pool = VariablePool.getInstance();
    pool.setParameterValue("template", "Variable one=" + prefix + "parameter_one" + suffix + " and two=" + prefix + "parameter_two" + suffix + " defined");
    pool.setParameterValue("parameter_one", "the first value");
    pool.setParameterValue("parameter_two", "this is the second parameter");
    String result = pool.replaceAllParameters("" + prefix + "template" + suffix);
    assertEquals("Variable one=the first value and two=this is the second parameter defined", result);
  }

  /**
   * Test case: value equals to parameter, including pre- and suffix
   */
  @Test
  public void testParamEqualsValue()
  {
    VariablePool pool = VariablePool.getInstance();
    pool.setParameterValue("FOO_PARAM", prefix + "FOO_PARAM" + suffix);
    String sql = "select '" + prefix + "FOO_PARAM" + suffix + "' as TEST;";
    String replaced = pool.replaceAllParameters(sql);
    assertEquals("select '" + prefix + "FOO_PARAM" + suffix + "' as TEST;", replaced);

    // mixed with "normal" parameter
    pool.setParameterValue("ZOO_PARAM", "tiger");
    sql = "select '" + prefix + "FOO_PARAM" + suffix + "' as TEST, '" + prefix + "ZOO_PARAM" + suffix + "' as TEST2;";
    replaced = pool.replaceAllParameters(sql);
    assertEquals("select '" + prefix + "FOO_PARAM" + suffix + "' as TEST, 'tiger' as TEST2;", replaced);
  }
}
