/*
 * WbSavePkMapping.java
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

import java.io.File;
import java.sql.SQLException;

import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.storage.PkMapping;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.FileDialogUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class WbSavePkMapping
	extends SqlCommand
{
	public static final String VERB = "WbSavePKMap";

	public WbSavePkMapping()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(CommonArgs.ARG_FILE, ArgumentType.Filename);
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
	public StatementRunnerResult execute(final String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		cmdLine.parse(getCommandLine(sql));
		String file = cmdLine.getValue(CommonArgs.ARG_FILE);
		if (file == null)
		{
			file = Settings.getInstance().getPKMappingFilename();
		}
		else
		{
			WbFile cd = new WbFile(Settings.getInstance().getConfigDir());
			file = StringUtil.replace(file, FileDialogUtil.CONFIG_DIR_KEY, cd.getFullPath());
		}

		if (file == null)
		{
			result.setFailure();
			result.addMessage(ResourceMgr.getString("ErrPkDefNoFile"));
			return result;
		}

		PkMapping.getInstance().saveMapping(file);
		String msg = ResourceMgr.getString("MsgPkMappingSaved");
		File f = new File(file);
		msg = StringUtil.replace(msg, "%filename%", f.getAbsolutePath());
		result.addMessage(msg);
		result.setSuccess();
		return result;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}
}
