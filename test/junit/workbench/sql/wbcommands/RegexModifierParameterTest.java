/*
 * RegexModifierParameterTest.java
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
package workbench.sql.wbcommands;

import workbench.WbTestCase;
import workbench.db.exporter.RegexReplacingModifier;
import workbench.util.ArgumentParser;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class RegexModifierParameterTest
  extends WbTestCase
{

  public RegexModifierParameterTest()
  {
    super("RegexModifierParameterTest");
  }

  @Test
  public void testParseParameterValue()
  {
    ArgumentParser cmdLine = new ArgumentParser();
    RegexModifierParameter.addArguments(cmdLine);
    cmdLine.parse("-"+ RegexModifierParameter.ARG_REPLACE_REGEX + "=[\\r\\n]+ -" + RegexModifierParameter.ARG_REPLACE_WITH + "=' ' ");

    RegexReplacingModifier modifier = RegexModifierParameter.buildFromCommandline(cmdLine);
    assertNotNull(modifier);
    String result = modifier.replacePattern("this\r\nis\ra\ntest");
    assertEquals("this is a test", result);

    cmdLine.parse("-"+ RegexModifierParameter.ARG_REPLACE_REGEX + "=[\\r\\n]+ " +
      "-" + RegexModifierParameter.ARG_REPLACE_WITH + "='*'");

    modifier = RegexModifierParameter.buildFromCommandline(cmdLine);
    assertNotNull(modifier);
    result = modifier.replacePattern("this\r\nis\ra\ntest");
    assertEquals("this*is*a*test", result);
  }
}
