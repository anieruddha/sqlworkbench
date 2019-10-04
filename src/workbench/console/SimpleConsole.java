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
package workbench.console;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * A simple WbConsoleReader using a Scanner
 *
 * This is a fallback if neither JLine nor the new Java 6 Console is available
 *
 * @author Thomas Kellerer
 */
public class SimpleConsole
	implements WbConsole
{
	private static Scanner inputScanner;

	public SimpleConsole()
	{
		inputScanner = new Scanner(System.in);
	}


  @Override
  public void clearScreen()
  {
  }

  @Override
  public char readCharacter()
  {
    try
    {
      int value = System.in.read();
      return (char)value;
    }
    catch (IOException ex)
    {
    }
    return (char)0;
  }

  @Override
  public void reset()
  {
    try
    {
      System.in.reset();
    }
    catch (IOException ex)
    {
    }
  }

	@Override
	public String readPassword(String prompt)
	{
		return readLine(prompt);
	}

	@Override
	public String readLine(String prompt)
	{
		System.out.print(prompt);
		return inputScanner.nextLine();
	}

	@Override
	public void shutdown()
	{
	}

	@Override
	public int getColumns()
	{
		return -1;
	}

	@Override
	public String readLineWithoutHistory(String prompt)
	{
		return readLine(prompt);
	}

	@Override
	public void clearHistory()
	{
	}

	@Override
	public void addToHistory(List<String> lines)
	{
	}

}
