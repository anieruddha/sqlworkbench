/*
 * DdlCommandTest.java
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
package workbench.sql.commands;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class DdlCommandTest
	extends WbTestCase
{

	public DdlCommandTest()
	{
		super("DdlCommandTest");
	}

	@Test
	public void testIgnoreDropErrors()
		throws Exception
	{
		TestUtil util = getTestUtil();
		StatementRunner runner = util.createConnectedStatementRunner();
		String sql = "drop table does_not_exist";
		runner.setIgnoreDropErrors(true);
		StatementRunnerResult result = runner.runStatement(sql);
		assertTrue(result.isSuccess());

		runner.setIgnoreDropErrors(false);
		runner.setUseSavepoint(true);
		result = runner.runStatement(sql);
		assertFalse(result.isSuccess());
	}


}
