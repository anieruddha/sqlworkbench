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
package workbench.log;

import org.apache.log4j.spi.LoggerFactory;

/**
 * This is a specialized LoggerFactory that creates the Workbench specific
 * Log4Jlogger.
 *
 * @author Peter Franken
 */
public class Log4JLoggerFactory
	implements LoggerFactory
{
	private static Class loggerFqcn = Log4JLogger.class;

	public Log4JLoggerFactory()
	{
	}

	@Override
	public Log4JLogger makeNewLoggerInstance(String name)
	{
		return new Log4JLogger(name);
	}

	public static void setLoggerFqcn(Class loggerFqcn)
	{
		Log4JLoggerFactory.loggerFqcn = loggerFqcn;
	}

	public static Class getLoggerFqcn()
	{
		return loggerFqcn;
	}
}
