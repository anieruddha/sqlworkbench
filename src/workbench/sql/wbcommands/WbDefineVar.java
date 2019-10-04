/*
 * WbDefineVar.java
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.WbConnection;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.VariablePool;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.CollectionUtil;
import workbench.util.EncodingUtil;
import workbench.util.ExceptionUtil;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.WbStringTokenizer;

/**
 * SQL Command to define a variable that gets stored in the system
 * wide parameter pool.
 *
 * @see workbench.sql.VariablePool
 *
 * @author  Thomas Kellerer
 */
public class WbDefineVar
	extends SqlCommand
{
	public static final String VERB = "WbVarDef";
	public static final String ALTERNATE_VERB = "WbDefineVar";
	public static final String ARG_LOOKUP_VALUES = "values";
	public static final String ARG_REMOVE_UNDEFINED = "removeUndefined";
	public static final String ARG_REPLACE_VARS = "replaceVars";
	public static final String ARG_VAR_NAME = "variable";
	public static final String ARG_VAR_VALUE = "value";
	public static final String ARG_CONTENT_FILE = "contentFile";
	public static final String ARG_CLEANUP_VALUE = "cleanupValue";
	public static final String ARG_SILENT = "silent";
	public static final String ARG_QUERY = "query";
	public static final String ARG_NULL_HANDLING = "nullHandling";

	public WbDefineVar()
	{
		super();
		this.cmdLine = new ArgumentParser();
		this.cmdLine.addArgument(CommonArgs.ARG_FILE, ArgumentType.Filename);
		this.cmdLine.addArgument(ARG_CONTENT_FILE, ArgumentType.StringArgument);
		this.cmdLine.addArgument(ARG_VAR_NAME);
		this.cmdLine.addArgument(ARG_VAR_VALUE);
		this.cmdLine.addArgument(ARG_REPLACE_VARS, ArgumentType.BoolArgument);
		this.cmdLine.addArgument(ARG_REMOVE_UNDEFINED, ArgumentType.BoolSwitch);
		this.cmdLine.addArgument(ARG_CLEANUP_VALUE, ArgumentType.BoolArgument);
		this.cmdLine.addArgument(ARG_SILENT, ArgumentType.BoolSwitch);
		this.cmdLine.addArgument(ARG_LOOKUP_VALUES);
		this.cmdLine.addArgument(ARG_QUERY);
		this.cmdLine.addArgument(ARG_NULL_HANDLING, NullVarHandling.class);

		CommonArgs.addEncodingParameter(cmdLine);
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

  @Override
  public String getAlternateVerb()
  {
    return ALTERNATE_VERB;
  }

	@Override
	protected boolean isConnectionRequired()
	{
		return false;
	}

	@Override
  @SuppressWarnings("null")
	public StatementRunnerResult execute(String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		String sql = getCommandLine(aSql);

		cmdLine.parse(sql);

    if (displayHelp(result))
    {
      return result;
    }

		WbFile file = this.evaluateFileArgument(cmdLine.getValue(CommonArgs.ARG_FILE));
		WbFile contentFile = this.evaluateFileArgument(cmdLine.getValue(ARG_CONTENT_FILE));

    boolean silent = cmdLine.getBoolean(ARG_SILENT);
		boolean removeUndefined = cmdLine.getBoolean(ARG_REMOVE_UNDEFINED);
		String varDef;
		if (cmdLine.hasArguments())
		{
			varDef = cmdLine.getNonArguments();
		}
		else
		{
			varDef = sql;
		}

		if (file != null && contentFile != null)
		{
			result.addErrorMessageByKey("ErrVarFileWrong");
			return result;
		}

		if (file != null)
		{
			initFromFile(result, file);
			return result;
		}

		if (contentFile != null)
		{
			readFileContents(result, contentFile, silent);
			return result;
		}

		String valueParameter = null;
		List<String> varNames = null;
		String varName = null;
		boolean lookupDefined = false;

		if (cmdLine.isArgPresent(ARG_VAR_NAME))
		{
			varName = cmdLine.getValue(ARG_VAR_NAME);
			varNames = CollectionUtil.arrayList(varName);
		}

		if (cmdLine.isArgPresent(ARG_VAR_NAME) && cmdLine.isArgPresent(ARG_LOOKUP_VALUES))
		{
			List<String> lookupValues = cmdLine.getListValue(ARG_LOOKUP_VALUES);
			if (CollectionUtil.isNonEmpty(lookupValues) && StringUtil.isNonEmpty(varName))
			{
				LogMgr.logDebug("WbDefineVar.execute()", "Lookup values for variable " + varName + ": " + lookupValues);
				VariablePool.getInstance().setLookupValues(varName, lookupValues);
			}
			lookupDefined = true;
		}

		if (cmdLine.isArgPresent(ARG_VAR_VALUE))
		{
			valueParameter = cmdLine.getValue(ARG_VAR_VALUE);
		}
		else if (!cmdLine.isArgPresent(ARG_VAR_NAME))
		{
			WbStringTokenizer tok = new WbStringTokenizer("=", true, "\"'", false);
			tok.setSourceString(varDef);
			tok.setKeepQuotes(true);

			if (tok.hasMoreTokens()) varName = tok.nextToken();

			varNames = StringUtil.stringToList(varName, ",", true, true);

			if (tok.hasMoreTokens())
			{
				valueParameter = tok.nextToken();
			}
		}

		if (CollectionUtil.isEmpty(varNames))
		{
			result.addErrorMessageByKey("ErrVarDefWrongParameter");
			return result;
		}

		if (lookupDefined && valueParameter == null)
		{
			valueParameter = "";
		}

    if (valueParameter != null)
    {
      // WbStringTokenizer returned any quotes that were used, so we have to remove them again
      // as they should not be part of the variable value
      valueParameter = StringUtil.trimQuotes(valueParameter.trim());
    }

		result.setSuccess();

    String query = StringUtil.trimToNull(StringUtil.trimQuotes(cmdLine.getValue(ARG_QUERY)));
    if (query == null)
    {
      query = getQueryFromValue(valueParameter);
    }

    NullVarHandling nullHandling = cmdLine.getEnumValue(ARG_NULL_HANDLING, getNullVarHandling());

		if (valueParameter == null && query == null)
		{
			for (String name : varNames)
			{
				VariablePool.getInstance().removeVariable(name);
				String removed = ResourceMgr.getFormattedString("MsgVarRemoved", name);
				result.addMessage(removed);
			}
		}
		else if (query != null)
		{
			readValuesFromDatabase(result, varNames, query, silent, nullHandling);
		}
		else
		{
			boolean cleanup = cmdLine.getBoolean(ARG_CLEANUP_VALUE, Settings.getInstance().getCleanupVariableValues());
			if (cleanup)
			{
				valueParameter = SqlUtil.makeCleanSql(valueParameter, false, false, true, currentConnection);
			}

			if (removeUndefined)
			{
				// as the SQL that was passed to this command already has all variables replaced,
				// we can simply remove anything that looks like a variable in the value.
				valueParameter = VariablePool.getInstance().removeVariables(valueParameter);
			}

			if (varNames.size() > 1)
			{
        LogMgr.logWarning(new CallerInfo(){}, "Multiple variables not supported when assigning constant values. Statement was: " + sql);
			}

			varName = varNames.get(0).trim();
			setVariable(result, varName, valueParameter, silent, nullHandling);

			if (result.isSuccess() && !silent)
			{
				String msg = ResourceMgr.getString("MsgVarDefVariableDefined");
				msg = StringUtil.replace(msg, "%var%", varName);
				msg = StringUtil.replace(msg, "%value%", valueParameter);
				msg = StringUtil.replace(msg, "%varname%", VariablePool.getInstance().buildVarName(varName, false));
				result.addMessage(msg);
			}
		}

		return result;
	}

  private String getQueryFromValue(String value)
  {
    if (value == null) return null;

    value = value.trim();

    if (value.startsWith("@"))
    {
      return StringUtil.trimQuotes(value.substring(1));
    }
    return null;
  }

	private void readValuesFromDatabase(StatementRunnerResult result, List<String> varNames, String query, boolean silent, NullVarHandling nullAction)
	{
		try
		{
			// In case the @ sign was placed inside the quotes, make sure
			// there are no quotes before removing the @ sign
			List<String> values = this.evaluateSql(currentConnection, query, result);
			int varCount = Math.min(values.size(), varNames.size());

			if (values.size() != varNames.size())
			{
        LogMgr.logWarning(new CallerInfo(){}, "The number of variables does not match the number of columns returned. Using only the first " + varCount + " variables");
			}

			for (int i=0; i < varCount; i++)
			{
				setVariable(result, varNames.get(i), values.get(i), silent, nullAction);
				if (!silent && result.isSuccess())
				{
					String msg = ResourceMgr.getString("MsgVarDefVariableDefined");
					msg = StringUtil.replace(msg, "%var%", varNames.get(i));
					msg = StringUtil.replace(msg, "%value%", values.get(i));
					msg = StringUtil.replace(msg, "%varname%", VariablePool.getInstance().buildVarName(varNames.get(i), false));
					result.addMessage(msg);
				}

			}
		}
		catch (Exception e)
		{
      LogMgr.logError(new CallerInfo(){}, "Error retrieving variable value using SQL: " + query, e);
			String err = ResourceMgr.getString("ErrReadingVarSql");
			err = StringUtil.replace(err, "%sql%", query);
			err = err + "\n\n" + ExceptionUtil.getDisplay(e);
			result.addErrorMessage(err);
		}
	}

	private void initFromFile(StatementRunnerResult result, WbFile file)
	{
		// if the file argument has been supplied, no variable definition
		// can be present, but the encoding parameter might have been passed
		String encoding = cmdLine.getValue("encoding");
		try
		{
			if (file.exists())
			{
				VariablePool.getInstance().readFromFile(file.getFullPath(), encoding, false);
				String msg = ResourceMgr.getFormattedString("MsgVarDefFileLoaded", file.getFullPath());
				result.addMessage(msg);
				result.setSuccess();
			}
			else
			{
				String msg = ResourceMgr.getFormattedString("ErrFileNotFound", file.getFullPath());
				result.addErrorMessage(msg);
			}
		}
		catch (Exception e)
		{
      LogMgr.logError(new CallerInfo(){}, "Error reading definition file: " + file.getFullPath(), e);
			String msg = ResourceMgr.getString("ErrReadingVarDefFile");
			msg = StringUtil.replace(msg, "%file%", file.getAbsolutePath());
			msg = msg + " " + ExceptionUtil.getDisplay(e);
			result.addErrorMessage(msg);
		}
	}

	private void setVariable(StatementRunnerResult result, String var, String value, boolean silent, NullVarHandling nullAction)
	{
		try
		{
      if (value == null)
      {
        switch (nullAction)
        {
          case empty:
            VariablePool.getInstance().setParameterValue(var, "");
            break;
          case remove:
            VariablePool.getInstance().removeVariable(var);
            if (!silent) result.addMessage(ResourceMgr.getFormattedString("MsgVarRemoved", var));
        }
      }
      else
      {
        VariablePool.getInstance().setParameterValue(var, value);
      }
		}
		catch (IllegalArgumentException e)
		{
			result.addErrorMessageByKey("ErrVarDefWrongName");
		}
	}

	/**
	 *	Return the result of the given SQL string and return the column values of the first row as a list of values.
	 *
	 *	If the SQL gives an error, an empty List will be returned
	 */
	private List<String> evaluateSql(WbConnection conn, String sql, StatementRunnerResult stmtResult)
		throws SQLException
	{
		ResultSet rs = null;
		List<String> result = new ArrayList<>(1);
		if (conn == null)
		{
			throw new SQLException("Cannot evaluate SQL based variable without a connection");
		}

		try
		{
			this.currentStatement = conn.createStatement();

			if (sql.endsWith(";"))
			{
				sql = sql.substring(0, sql.length() - 1);
			}
			rs = this.currentStatement.executeQuery(sql);
			ResultSetMetaData meta = rs.getMetaData();
			int colCount = meta.getColumnCount();

			if (rs.next())
			{
				for (int col=1; col <= colCount; col++)
				{
					Object value = rs.getObject(col);
					result.add(value == null ? null : value.toString());
				}
			}

			if (rs.next())
			{
				stmtResult.addWarningByKey("ErrVarDefRows");
			}

			if (stmtResult.hasWarning())
			{
				stmtResult.addMessageNewLine();
			}
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}

		return result;
	}

	private void readFileContents(StatementRunnerResult result, WbFile contentFile, boolean silent)
	{
		String varname = cmdLine.getValue(ARG_VAR_NAME);
		if (StringUtil.isBlank(varname))
		{
			result.addMessageByKey("ErrVarNoName");
			result.setFailure();
			return;
		}

		boolean replace = cmdLine.getBoolean(ARG_REPLACE_VARS, true);
		String encoding = cmdLine.getValue("encoding");
		if (encoding == null)
		{
			encoding = EncodingUtil.getDefaultEncoding();
		}

		try
		{
			String value = FileUtil.readFile(contentFile, encoding);
			if (replace)
			{
				value = VariablePool.getInstance().replaceAllParameters(value);
			}

			setVariable(result, varname, value, silent, getNullVarHandling());
			String msg = ResourceMgr.getFormattedString("MsgVarReadFile", varname, contentFile.getFullPath());
			result.addMessage(msg);
		}
		catch (FileNotFoundException fnf)
		{
      LogMgr.logError(new CallerInfo(){}, "Content file " + contentFile.getFullPath() + " not found!", fnf);
			result.addErrorMessageByKey("ErrFileNotFound", contentFile.getFullPath());
		}
		catch (IOException io)
		{
			result.addErrorMessage(ExceptionUtil.getDisplay(io));
		}
	}

  private NullVarHandling getNullVarHandling()
  {
    String val = Settings.getInstance().getProperty("workbench.sql.parameter.values.nullhandling", "empty");
    try
    {
      return NullVarHandling.valueOf(val.toLowerCase());
    }
    catch (Throwable ignore)
    {
      return NullVarHandling.empty;
    }
  }

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

  private static enum NullVarHandling
  {
    ignore,
    remove,
    empty
  }

}

