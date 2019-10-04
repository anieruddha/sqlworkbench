/*
 * StatementError.java
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
package workbench.sql;

import java.sql.SQLException;

import workbench.util.DdlObjectInfo;
import workbench.util.ExceptionUtil;
import workbench.util.WbFile;

/**
 * A class containing details about an Error based on the error message.
 *
 * Its main purpose is to hold the position of the error inside the failing SQL.
 * The position can either be defined by an absolute offset ({@link #getErrorPosition()})
 * or by line and column.
 *
 * @author Thomas Kellerer
 */
public class ErrorDescriptor
{
	private int errorPosition = -1;
	private int errorLine = -1;
	private int errorColumn = -1;
	private DdlObjectInfo object;
	private String errorMessage;
	private String errorCode;
	private boolean messageIncludesPosition;
  private int inStatementOffset = 0;
  private String originalStatement;
  private WbFile scriptFile;

	public ErrorDescriptor()
	{
	}

	public ErrorDescriptor(String msg)
	{
    this.errorMessage = msg;
	}

	public ErrorDescriptor(Throwable th)
	{
    errorMessage = ExceptionUtil.getDisplay(th);
    setErrorCode(th);
	}

  public WbFile getScriptFile()
  {
    return scriptFile;
  }

  public void setScriptFile(WbFile scriptFile)
  {
    this.scriptFile = scriptFile;
  }

  public String getOriginalStatement()
  {
    return originalStatement;
  }

  public void setOriginalStatement(String statement)
  {
    this.originalStatement = statement;
  }

  /**
	 * Indicates if the original error messages already includes the position of the error.
	 *
	 * This can be used when enhancing the error message to display it to the user
	 */
	public boolean getMessageIncludesPosition()
	{
		return messageIncludesPosition;
	}

	public void setMessageIncludesPosition(boolean flag)
	{
		this.messageIncludesPosition = flag;
	}

	/**
	 * Sets the error position as a column/line combination (relative to the start of the SQL statement).
	 *
	 * @param line     the line as reported by the DBMS
	 * @param column   the column as reported by the DBMS
	 */
	public void setErrorPosition(int line, int column)
	{
		this.errorLine = line;
		this.errorColumn = column;
	}

	public int getErrorLine()
	{
		return errorLine;
	}

	public int getErrorColumn()
	{
		return errorColumn;
	}

	public int getErrorPosition()
	{
		return errorPosition + inStatementOffset;
	}

  public void setInStatementOffset(int offset)
  {
    inStatementOffset = offset;
  }

	/**
	 * Sets the position of the error as an offset to the start of the SQL statement.
	 *
	 * @param position  the position inside the statement
	 */
	public void setErrorOffset(int position)
	{
		this.errorPosition = position;
	}

	public void setObjectInfo(DdlObjectInfo info)
	{
		if (info != null && info.isValid())
		{
			object = info;
		}
		else
		{
			object = null;
		}
	}

	public DdlObjectInfo getObjectInfo()
	{
		return object;
	}

  public String getErrorCode(boolean upper)
  {
    if (errorCode != null && upper)
    {
      return errorCode.toUpperCase();
    }

    return errorCode;
  }

  public String getErrorCode()
  {
    return errorCode;
  }

  public void setErrorCode(Throwable th)
  {
    if (th instanceof SQLException)
    {
      SQLException se = (SQLException)th;

      int errCod = se.getErrorCode();
      String errState = se.getSQLState();

      if (errCod != 0)
      {
        errorCode = String.valueOf(errCod);
      }
      else if (errState != null)
      {
        errorCode = errState;
      }
    }
  }

	public String getErrorMessage()
	{
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage)
	{
		this.errorMessage = errorMessage;
	}

	public boolean hasError()
	{
		return errorPosition > -1 || (errorLine > -1 && errorColumn > -1);
	}

	@Override
	public String toString()
	{
		String msg = errorMessage != null ? errorMessage : "";
		if (errorPosition > -1)
		{
			return msg + "\n\nat position " + errorPosition;
		}
		else if (errorLine > -1 && errorColumn > -1)
		{
			return msg + "\n\nat line: " + errorLine + ", column: " + errorColumn;
		}

		return errorMessage != null ? errorMessage : "no error";
	}

}
