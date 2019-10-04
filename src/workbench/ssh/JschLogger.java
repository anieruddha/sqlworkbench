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
package workbench.ssh;

import workbench.log.LogMgr;

import com.jcraft.jsch.Logger;

/**
 *
 * @author tkellerer
 */
public class JschLogger
  implements Logger
{

  @Override
  public boolean isEnabled(int level)
  {
    switch (level)
    {
      case Logger.DEBUG:
        return LogMgr.isDebugEnabled();
      case Logger.INFO:
        return LogMgr.isInfoEnabled();
      default:
        return true;
    }
  }

  @Override
  public void log(int level, String message)
  {
    StackTraceElement[] stack = Thread.currentThread().getStackTrace();
    StackTraceElement e = stack[2];
    String ci = "JSch" + getSimpleClassName(e) + "." + e.getMethodName() + "()";

    switch (level)
    {
      case Logger.DEBUG:
        LogMgr.logDebug(ci, message);
        break;
      case Logger.INFO:
        LogMgr.logInfo(ci, message);
        break;
      case Logger.WARN:
        LogMgr.logWarning(ci, message, null);
        break;
      default:
        // Everything else is an error
        LogMgr.logError(ci, message, null);
    }
  }

  private String getSimpleClassName(StackTraceElement e)
  {
    if (e == null) return "";
    String cls = e.getClassName();
    int pos = cls.lastIndexOf('.');
    return cls.substring(pos);
  }

}
