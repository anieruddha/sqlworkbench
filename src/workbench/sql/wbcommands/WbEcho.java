/*
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

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.sql.MessagePriority;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class WbEcho
	extends SqlCommand
{
	public static final String VERB = "WbEcho";

	private static final String ARG_MESSAGE = "message";
	private static final String ARG_MODE = "mode";
	private static final String ARG_IMPORTANT = "important";

  public WbEcho()
  {
    cmdLine = new ArgumentParser();
    CommonArgs.addEncodingParameter(cmdLine);
    cmdLine.addArgument(CommonArgs.ARG_FILE, ArgumentType.Filename);
    cmdLine.addArgument(ARG_MESSAGE, ArgumentType.RepeatableValue);
    cmdLine.addArgument(ARG_IMPORTANT, ArgumentType.BoolSwitch);

    List<String> values = new ArrayList<>();
    for (Mode m : Mode.values())
    {
      values.add(m.toString());
    }
    cmdLine.addArgument(ARG_MODE, values);
  }

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	protected boolean isConnectionRequired()
	{
		return false;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
    StatementRunnerResult result = new StatementRunnerResult(sql);
    String line = getCommandLine(sql);
    cmdLine.parse(line);

    if (cmdLine.hasArguments())
    {
      List<String> messageList = cmdLine.getList(ARG_MESSAGE);
      String cmdMessage = null;

      if (messageList != null && messageList.size() > 0)
      {
        cmdMessage = StringUtil.listToString(messageList, StringUtil.LINE_TERMINATOR, false);
      }

      if (cmdMessage != null)
      {
        WbFile file = evaluateFileArgument(cmdLine.getValue(CommonArgs.ARG_FILE));
        Mode mode = Mode.byString(cmdLine.getValue(ARG_MODE), Mode.normal);

        if (mode == Mode.log)
        {
          LogMgr.logInfo(new CallerInfo(){}, cmdMessage);
        }

        if (file != null)
        {
          String encoding = cmdLine.getValue(CommonArgs.ARG_ENCODING, Settings.getInstance().getDefaultEncoding());
          encoding = EncodingUtil.cleanupEncoding(encoding);

          try
          {
            printToFile(file, cmdMessage, encoding, mode);
          }
          catch (IOException e)
          {
            LogMgr.logError(new CallerInfo(){}, "Could not write message to file " + file, e);
          }
        }

        if (cmdLine.getBoolean(ARG_IMPORTANT, false))
        {
          result.setMessagePriority(MessagePriority.high);
        }
        result.addMessage(cmdMessage);
        result.setSuccess();
        return result;
      }
    }

    if (line != null && line.startsWith("!!"))
    {
      result.setMessagePriority(MessagePriority.high);
      line = line.replaceFirst("!!\\s*", "");
    }

		result.addMessage(line);
		result.setSuccess();
		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

  private void printToFile(WbFile file, String message, String encoding, Mode mode)
    throws IOException
  {
    if (mode == Mode.prepend && !file.exists())
    {
      LogMgr.logWarning(new CallerInfo(){}, "Cannot prepend to file "+ file + " since it does not exist!");
    }

    if (mode == Mode.prepend)
    {
      FileUtil.writeAtStart(file, message + StringUtil.LINE_TERMINATOR, encoding);
    }
    else if (mode == Mode.append)
    {
      FileUtil.writeString(file, StringUtil.LINE_TERMINATOR + message, mode == Mode.append);
    }
    else
    {
      FileUtil.writeString(file, message, false);
    }

    LogMgr.logInfo(new CallerInfo(){}, "Successfully wrote message to file " + file);
  }

  private enum Mode
  {
    normal, append, prepend, log;

    static Mode byString(String modeStr, Mode defaultMode)
    {
      if (modeStr != null && modeStr.trim().length() > 0)
      {
        for (Mode m : Mode.values())
        {
          if (m.toString().equalsIgnoreCase(modeStr))
          {
            return m;
          }
        }
      }

      return defaultMode;
    }
  };


}
