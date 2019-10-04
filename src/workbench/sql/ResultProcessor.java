/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2016 Thomas Kellerer.
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
package workbench.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;

import workbench.db.WbConnection;


/**
 * A wrapper to process the results produced by a Statement.
 *
 * This properly deals with ResultSets that contain ResultSets.
 *
 * @author Thomas Kellerer
 */
public class ResultProcessor
{
  private final Statement currentStatement;
  private ResultSet currentResult;
  private final WbConnection originalConnection;
  private boolean supportsGetMoreResults = true;

  public ResultProcessor(Statement statement, ResultSet firstResult, WbConnection conn)
  {
    originalConnection = conn;
    currentStatement = statement;
    currentResult = firstResult;
  }

  public ResultSet getResult()
  {
    if (currentResult != null)
    {
      ResultSet rs = currentResult;
      currentResult = null;
      return rs;
    }
    
    try
    {
      return currentStatement.getResultSet();
    }
    catch (Exception ex)
    {
      return null;
    }
  }

  public boolean hasMoreResults()
    throws SQLException
  {
    try
    {
      return supportsGetMoreResults && checkForMoreResults();
    }
    catch (SQLFeatureNotSupportedException | AbstractMethodError ex)
    {
      LogMgr.logWarning(new CallerInfo(){}, "Error when calling getMoreResults()", ex);
      supportsGetMoreResults = false;
    }
    catch (SQLException sql)
    {
      // assume that SQLException indicates a real error
      LogMgr.logError(new CallerInfo(){}, "Error when calling getMoreResults()", sql);
      if (!originalConnection.getDbSettings().ignoreSQLErrorsForGetMoreResults())
      {
        throw sql;
      }
    }
    catch (Throwable ex)
    {
      // Some drivers throw errors if no result is available.
      // In this case simply assume there are no more results.
      LogMgr.logWarning(new CallerInfo(){}, "Error when calling getMoreResults()", ex);
    }
    return false;
  }

  private boolean checkForMoreResults()
    throws SQLException
  {
    return currentStatement.getMoreResults();
  }

}
