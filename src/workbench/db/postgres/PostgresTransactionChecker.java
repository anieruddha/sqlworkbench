/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2019 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
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
package workbench.db.postgres;

import java.lang.reflect.Method;
import java.sql.Connection;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;

import workbench.db.DefaultTransactionChecker;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresTransactionChecker
  extends DefaultTransactionChecker
{

  private boolean usePgTransactionState = true;

  public PostgresTransactionChecker(String sql)
  {
    super(sql);
  }

  @Override
  public boolean hasUncommittedChanges(WbConnection con)
  {
    if (usePgTransactionState)
    {
      Boolean isOpen = isStateOpen(con);
      if (isOpen != null)
      {
        return isOpen.booleanValue();
      }
    }
    return super.hasUncommittedChanges(con);
  }

  private Boolean isStateOpen(WbConnection con)
  {
    try
    {
      Connection pgCon = con.getSqlConnection();
      Method getState = pgCon.getClass().getMethod("getTransactionState");
      Object state = getState.invoke(pgCon);
      if (state == null) return null;
      return state.toString().equalsIgnoreCase("open");
    }
    catch (Throwable th)
    {
      LogMgr.logError(new CallerInfo(){}, "Could not read TransactionState from PgConnection", th);
      this.usePgTransactionState = false;
      return null;
    }
  }

}
