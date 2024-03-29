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
 *     https://sql-workbench.eu/manual/license.html
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
package workbench.db.postgres;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.JdbcUtils;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.VersionNumber;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresUtil
{

  /**
   * The property that can be passed during connecting to identify the application.
   *
   * @see #supportsAppInfoProperty(java.lang.Class)
   */
  public static final String APP_NAME_PROPERTY = "ApplicationName";

  /**
   * Sets the application name for pg_stat_activity.
   * To set the name, the autocommit will be turned off, and the transaction will be committed afterwards.
   * The name will only be set if the PostgreSQL version is >= 9.0
   *
   * @param con the connection
   * @param appName the name to set
   */
  public static void setApplicationName(Connection con, String appName)
  {
    if (JdbcUtils.hasMinimumServerVersion(con, "9.0") && Settings.getInstance().getBoolProperty("workbench.db.postgresql.set.appname", true))
    {
      Statement stmt = null;
      try
      {
        // SET application_name seems to require autocommit to be turned off
        // as the autocommit setting that the user specified in the connection profile
        // will be set after this call, setting it to false should not do any harm
        con.setAutoCommit(false);
        stmt = con.createStatement();
        stmt.execute("SET application_name = '" + appName + "'");
        // make sure the transaction is ended
        // as this is absolutely the first thing we did, commit() should be safe
        con.commit();
      }
      catch (Exception e)
      {
        // Make sure the transaction is ended properly
        try { con.rollback(); } catch (Exception ex) {}
        LogMgr.logWarning(new CallerInfo(){}, "Could not set client info", e);
      }
      finally
      {
        SqlUtil.closeStatement(stmt);
      }
    }
  }

  /**
   * Checks if the passed driver supports the ApplicationName property.
   *
   * Setting the application name for pg_stat_activity is only supported by drivers >= 9.1
   *
   * @param pgDriver the Postgres JDBC driver class
   * @return true if the driver supports the ApplicationName property
   * @see #APP_NAME_PROPERTY
   */
  public static boolean supportsAppInfoProperty(Class pgDriver)
  {
    try
    {
      Driver drv = (Driver)pgDriver.newInstance();
      int majorVersion = drv.getMajorVersion();
      int minorVersion = drv.getMinorVersion();

      VersionNumber version = new VersionNumber(majorVersion, minorVersion);
      VersionNumber min = new VersionNumber(9,1);
      return version.isNewerOrEqual(min);
    }
    catch (Throwable th)
    {
      return false;
    }
  }

  /**
   * Returns the current search path defined in the session (or the user).
   * <br/>
   * This uses the Postgres function <tt>current_schemas(boolean)</tt>
   * <br/>
   * @param con the connection for which the search path should be retrieved
   * @return the list of schemas in the search path.
   */
  public static List<String> getSearchPath(WbConnection con)
  {
    if (con == null) return Collections.emptyList();
    List<String> result = new ArrayList<>();

    ResultSet rs = null;
    Statement stmt = null;
    Savepoint sp = null;

    String query = Settings.getInstance().getProperty("workbench.db.postgresql.retrieve.search_path", "select array_to_string(current_schemas(true), ',')");

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logInfo(new CallerInfo(){}, "Query used to retrieve search path:\n" + query);
    }

    try
    {
      sp = con.setSavepoint();
      stmt = con.createStatementForQuery();
      rs = stmt.executeQuery(query);
      if (rs.next())
      {
        String path = rs.getString(1);
        result.addAll(StringUtil.stringToList(path, ",", true, true, false, false));
      }
      con.releaseSavepoint(sp);
    }
    catch (SQLException sql)
    {
      con.rollback(sp);
      LogMgr.logError(new CallerInfo(){}, "Could not read search path", sql);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }

    if (result.isEmpty())
    {
      LogMgr.logWarning(new CallerInfo(){}, "Using public as the default search path");
      // Fallback. At least look in the public schema
      result.add("public");
    }
    return result;
  }

  /**
   * Change the passed JDBC URL to point to the new database.
   *
   * @param url           the Postgres JDBC URL
   * @param newDatabase   the new database
   *
   * @return the new JDBC URL suitable to connect to that database
   */
  public static String switchDatabaseURL(String url, String newDatabase)
  {
    if (StringUtil.isBlank(url)) return url;
    if (!url.startsWith("jdbc:postgresql:")) throw new IllegalArgumentException("Not a Postgres JDBC URL");

    int pos = url.indexOf(("//"));
    if (pos < 0) return url;
    pos = url.indexOf('/', pos + 2);
    if (pos < 0) return url;
    String base = url.substring(0, pos + 1);
    int qPos = url.indexOf('?', pos + 1);
    String newUrl = base + newDatabase;
    if (qPos > 0)
    {
      newUrl += url.substring(qPos);
    }
    return newUrl;
  }

  public static String getCurrentDatabase(WbConnection conn)
  {
    try
    {
      // The Postgres JDBC driver uses an internally cached value
      // for the current database, so there is no need to check
      // if the connection is busy.
      return conn.getSqlConnection().getCatalog();
    }
    catch (SQLException sql)
    {
      return null;
    }
  }

  public static List<String> getAccessibleDatabases(WbConnection conn)
  {
    if (conn == null) return Collections.emptyList();

    List<String> result = new ArrayList();

    DataStore names = SqlUtil.getResult(conn,
      "select datname " +
      "from pg_database " +
      "where has_database_privilege(datname, 'connect') \n" +
      "  and datallowconn \n" +
      "order by datname", true);

    if (names != null)
    {
      for (int row = 0; row < names.getRowCount(); row++)
      {
        result.add(names.getValueAsString(row, 0));
      }
    }
    return result;
  }

  public static List<String> getAllDatabases(WbConnection currentConnection)
  {
    List<String> result = new ArrayList<>();
    try
    {
      DataStore ds = listPgDatabases(currentConnection, false);
      for (int i=0; i < ds.getRowCount(); i ++)
      {
        result.add(ds.getValueAsString(i, 0));
      }
    }
    catch (SQLException sql)
    {
      LogMgr.logError(new CallerInfo(){}, "Could not retrieve databases", sql);
    }
    return result;
  }

  public static DataStore listPgDatabases(WbConnection currentConnection, boolean verbose)
    throws SQLException
  {
    String name = StringUtil.capitalize(currentConnection.getMetadata().getCatalogTerm());
    String size = verbose ?
      "       CASE WHEN pg_catalog.has_database_privilege(d.datname, 'CONNECT')\n" +
      "            THEN pg_catalog.pg_size_pretty(pg_catalog.pg_database_size(d.datname))\n" +
      "            ELSE 'No Access'\n" +
      "       END as \"Size\", \n" : "";
    String sql =
      "SELECT d.datname as \"" + name + "\",\n" +
      "       pg_catalog.pg_get_userbyid(d.datdba) as \"Owner\",\n" +
      "       pg_catalog.pg_encoding_to_char(d.encoding) as \"Encoding\",\n" +
      "       d.datcollate as \"Collate\",\n" +
      "       d.datctype as \"Ctype\",\n" +
      "       pg_catalog.array_to_string(d.datacl, E'\\n') AS \"Access privileges\", \n" +
      size +
      "       pg_catalog.shobj_description(d.oid, 'pg_database') as \"Description\" \n" +
      "FROM pg_catalog.pg_database d\n" +
      "ORDER BY 1";
    DataStore ds = SqlUtil.getResult(currentConnection, sql, true);
    ds.setGeneratingSql(sql);
    ds.setResultName(ResourceMgr.getString("TxtDbList"));
    return ds;
  }

  public static boolean isGreenplum(Connection conn)
  {
    Statement stmt = null;
    ResultSet rs = null;
    try
    {
      DatabaseMetaData metaData = conn.getMetaData();
      int major = metaData.getDatabaseMajorVersion();
      if (major > 8)
      {
        return false;
      }
      stmt = conn.createStatement();
      rs = stmt.executeQuery("select version()");
      String version = "";
      if (rs.next())
      {
        version = rs.getString(1);
      }

      if (!conn.getAutoCommit())
      {
        conn.commit();
      }
      return version.toLowerCase().contains("greenplum");
    }
    catch (Throwable th)
    {
      LogMgr.logWarning(new CallerInfo(){}, "Could not check real database version", th);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return false;
  }

  public static boolean isRedshift(WbConnection conn)
  {
    if (conn == null) return false;
    return conn.getUrl().startsWith("jdbc:redshift:");
  }
}
