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
package workbench.interfaces;

import workbench.sql.ErrorDescriptor;
import workbench.sql.parser.ScriptParser;

/**
 *
 * @author Thomas Kellerer
 */
public interface ScriptErrorHandler
{
  /**
   * Display a prompt to the user and ask on how to proceed with a statement that caused an error.
   *
   * Return values:
   * <ul>
   * <li>WbSwingUtilities.IGNORE_ONE - ignore the current statement and continue</li>
   * <li>WbSwingUtilities.IGNORE_ALL - ignore all subsequent errors without asking any more</li>
   * <li>JOptionPane.CANCEL_OPTION - cancel script execution</li>
   * </ul>
   * @param errorStatementIndex the index of the statement in a script
   * @param errorDetails        the error description
   * @param parser              the parser used to run the script
   * @param selectionOffset     the offset of the actual statement
   * <p>
   * @return
   */
  int scriptErrorPrompt(int errorStatementIndex, ErrorDescriptor errorDetails, ScriptParser parser, int selectionOffset);

}
