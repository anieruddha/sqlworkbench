/*
 * ConditionCheck.java
 *
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2019, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.sql-workbench.eu/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.eu
 */
package workbench.sql.wbcommands;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import workbench.resource.ResourceMgr;

import workbench.db.DBID;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.sql.StatementRunnerResult;
import workbench.sql.VariablePool;

import workbench.util.ArgumentParser;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class ConditionCheck
{
	private static final Result OK = new Result();

	public static final String PARAM_IF_FILE_EXISTS = "ifFileExists";
	public static final String PARAM_IF_TABLE_EXISTS = "ifTableExists";
	public static final String PARAM_IF_FILE_NOTEXISTS = "ifNotFileExists";
	public static final String PARAM_IF_TABLE_NOTEXISTS = "ifNotTableExists";
	public static final String PARAM_IF_DEF = "ifDefined";
	public static final String PARAM_IF_NOTDEF = "ifNotDefined";
	public static final String PARAM_IF_EQUALS = "ifEquals";
	public static final String PARAM_IF_NOTEQ = "ifNotEquals";
	public static final String PARAM_IF_EMPTY = "ifEmpty";
	public static final String PARAM_IF_NOTEMPTY = "ifNotEmpty";
	public static final String PARAM_IS_DBMS = "isDBMS";
	public static final String PARAM_ISNOT_DBMS = "isNotDBMS";

	public static void addParameters(ArgumentParser cmdLine)
	{
    List<String> knownDbIds = new ArrayList<>();
    for (DBID id : DBID.values())
    {
      knownDbIds.add(id.getId());
    }

		for (String arg : allParameters())
		{
      if (arg.equals(PARAM_IS_DBMS) || arg.equals(PARAM_ISNOT_DBMS))
      {
        cmdLine.addArgument(arg, knownDbIds);
      }
      else
      {
        cmdLine.addArgument(arg);
      }
		}
	}

	public static boolean conditionSpecified(ArgumentParser cmdLine)
	{
		for (String arg : allParameters())
		{
			if (cmdLine.isArgPresent(arg))
			{
				return true;
			}
		}
		return false;
	}

	public static boolean isCommandLineOK(StatementRunnerResult result, ArgumentParser cmdLine)
	{
		int count = 0;
		for (String arg : allParameters())
		{
			if (cmdLine.isArgPresent(arg))
			{
				count ++;
			}
		}

		if (count <= 1) return true;

		// more than one argument specified, this is not allowed
		result.addErrorMessageByKey("ErrCondTooMany", StringUtil.listToString(allParameters(), ','));
		return false;
	}

	/**
	 * Check if the condition specified on the commandline is met.
	 *
	 * @param cmdLine the parameter to check
   * @param conn    the current connection (needed for isDBMS and isNotDBMS)
   *
   * To resolve file parameters, this function simply creates a WbFile instance from the parameter name.
   * 
	 * @return {@link #OK} if the condition is met,
	 *         the parameter where the check failed otherwise
	 */
	public static Result checkConditions(ArgumentParser cmdLine, WbConnection conn)
  {
    return checkConditions(cmdLine, conn, (fname -> new WbFile(fname)));
  }

	/**
	 * Check if the condition specified on the commandline is met.
	 *
	 * @param cmdLine        the parameter to check
   * @param conn           the current connection (needed for isDBMS and isNotDBMS)
   * @param fileEvaluator  a function that creates a WbFile instance from a string parameter
   *
	 * @return {@link #OK} if the condition is met,
	 *         the parameter where the check failed otherwise
	 */
	public static Result checkConditions(ArgumentParser cmdLine, WbConnection conn, Function<String, WbFile> fileEvaluator)
	{
		if (cmdLine.isArgPresent(PARAM_IF_DEF))
		{
			String var = cmdLine.getValue(PARAM_IF_DEF);
			if (!VariablePool.getInstance().isDefined(var))
			{
				return new Result(PARAM_IF_DEF, var);
			}
		}

		if (cmdLine.isArgPresent(PARAM_IF_NOTDEF))
		{
			String var = cmdLine.getValue(PARAM_IF_NOTDEF);
			if (VariablePool.getInstance().isDefined(var))
			{
				return new Result(PARAM_IF_NOTDEF, var);
			}
		}

		if (cmdLine.isArgPresent(PARAM_IF_EMPTY))
		{
			String var = cmdLine.getValue(PARAM_IF_EMPTY);
			String value = VariablePool.getInstance().getParameterValue(var);
			if (StringUtil.isNonEmpty(value))
			{
				return new Result(PARAM_IF_EMPTY, var);
			}
		}

		if (cmdLine.isArgPresent(PARAM_IF_NOTEMPTY))
		{
			String var = cmdLine.getValue(PARAM_IF_NOTEMPTY);
			String value = VariablePool.getInstance().getParameterValue(var);
			if (StringUtil.isEmptyString(value))
			{
				return new Result(PARAM_IF_NOTEMPTY, var);
			}
		}

		if (cmdLine.isArgPresent(PARAM_IF_EQUALS))
		{
			String var = cmdLine.getValue(PARAM_IF_EQUALS);
			String[] elements = var.split("=");
			if (elements.length == 2)
			{
				String value = VariablePool.getInstance().getParameterValue(elements[0]);
				if (value != null && value.equals(elements[1]))
				{
					return OK;
				}
				return new Result(PARAM_IF_EQUALS, elements[0], elements[1]);
			}
		}

		if (cmdLine.isArgPresent(PARAM_IF_NOTEQ))
		{
			String var = cmdLine.getValue(PARAM_IF_NOTEQ);
			String[] elements = var.split("=");
			if (elements.length == 2)
			{
				String value = VariablePool.getInstance().getParameterValue(elements[0]);
				if (value == null || !value.equals(elements[1]))
				{
					return OK;
				}
				return new Result(PARAM_IF_NOTEQ, elements[0], elements[1]);
			}
		}

    if (cmdLine.isArgPresent(PARAM_IS_DBMS))
    {
      String currentDB = (conn == null ? "N/A" : conn.getDbId());
      String dbid = cmdLine.getValue(PARAM_IS_DBMS);
      if (currentDB.equalsIgnoreCase(dbid))
      {
        return OK;
      }
			return new Result(PARAM_IS_DBMS, currentDB, dbid);
    }

    if (cmdLine.isArgPresent(PARAM_ISNOT_DBMS))
    {
      if (conn == null)
      {
        return OK;
      }
      String dbid = cmdLine.getValue(PARAM_ISNOT_DBMS);
      String currentDB = conn.getDbId();
      if (!currentDB.equalsIgnoreCase(dbid))
      {
        return OK;
      }
			return new Result(PARAM_ISNOT_DBMS, currentDB, dbid);
    }

    if (cmdLine.isArgPresent(PARAM_IF_FILE_EXISTS))
    {
      String fname = cmdLine.getValue(PARAM_IF_FILE_EXISTS);
      WbFile f = fileEvaluator.apply(fname);
      if (f.exists())
      {
        return OK;
      }
			return new Result(PARAM_IF_FILE_EXISTS, fname);
    }

    if (cmdLine.isArgPresent(PARAM_IF_FILE_NOTEXISTS))
    {
      String fname = cmdLine.getValue(PARAM_IF_FILE_NOTEXISTS);
      WbFile f = fileEvaluator.apply(fname);
      if (!f.exists())
      {
        return OK;
      }
			return new Result(PARAM_IF_FILE_NOTEXISTS, fname);
    }

    if (cmdLine.isArgPresent(PARAM_IF_TABLE_EXISTS))
    {
      String tname = cmdLine.getValue(PARAM_IF_TABLE_EXISTS);
      if (tableExists(conn, tname))
      {
        return OK;
      }
			return new Result(PARAM_IF_TABLE_EXISTS, tname);
    }

    if (cmdLine.isArgPresent(PARAM_IF_TABLE_NOTEXISTS))
    {
      String tname = cmdLine.getValue(PARAM_IF_TABLE_NOTEXISTS);
      if (!tableExists(conn, tname))
      {
        return OK;
      }
			return new Result(PARAM_IF_TABLE_NOTEXISTS, tname);
    }

		return OK;
	}

  private static boolean tableExists(WbConnection conn, String tableName)
  {
    if (conn == null) return false;
    TableIdentifier tbl = conn.getMetadata().findTable(new TableIdentifier(tableName));
    return tbl != null;
  }

	public static String getMessage(String command, Result check)
	{
		String action = ResourceMgr.getFormattedString("Err_NotExecuted", command);
		return ResourceMgr.getFormattedString("Err_" + check.getFailedCondition(), action, check.getVariable(), check.getExpectedValue());
	}

	public static class Result
	{
		private boolean conditionIsOK;
		private String failedParameter;
		private String variableName;
		private String expectedValue;

		public Result()
		{
			conditionIsOK = true;
		}

		public Result(String param, String varName)
		{
			this.conditionIsOK = false;
			this.failedParameter = param;
			this.variableName = varName;
		}

		public Result(String param, String varName, String value)
		{
			this.conditionIsOK = false;
			this.failedParameter = param;
			this.variableName = varName;
			this.expectedValue = value;
		}

		public boolean isOK()
		{
			return conditionIsOK;
		}

		public String getExpectedValue()
		{
			return this.expectedValue;
		}

		public String getFailedCondition()
		{
			return failedParameter;
		}

		public String getVariable()
		{
			return variableName;
		}
	}

  private static List<String> allParameters()
  {
    List<String> args = new ArrayList<>();
    Field[] fields = ConditionCheck.class.getDeclaredFields();
    for (Field f : fields)
    {
      String arg = f.getName();
      if (arg.startsWith("PARAM_"))
      {
        try
        {
          args.add((String)f.get(null));
        }
        catch (Throwable th)
        {
          // cannot happen
        }
      }
    }
    return args;
  }

}

