/*
 * ConsoleReaderFactory.java
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
package workbench.console;

import java.io.IOException;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;
import workbench.resource.Settings;

/**
 *
 * @author Thomas Kellerer
 */
public class WbConsoleFactory
{
	private static WbConsole instance;

	public synchronized static WbConsole getConsole()
	{
		if (instance == null)
		{
			if (useJLine())
			{
				try
				{
					instance = new JLineWrapper();
          LogMgr.logDebug(new CallerInfo(){}, "Using JLine");
					return instance;
				}
				catch (IOException io)
				{
          LogMgr.logError(new CallerInfo(){}, "Could not create JLineWrapper", io);
					instance = null;
				}
			}

      if (System.console() != null)
			{
				instance = new SystemConsole();
        LogMgr.logDebug(new CallerInfo(){}, "Using System.console()");
			}

			if (instance == null)
			{
				instance = new SimpleConsole();
			}
		}
		return instance;
	}

	private static boolean useJLine()
	{
		return Settings.getInstance().getBoolProperty("workbench.console.use.jline", true);
	}

}
