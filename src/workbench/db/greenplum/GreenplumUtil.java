/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.greenplum;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;

import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.VersionNumber;

/**
 *
 * @author Thomas Kellerer
 */
public class GreenplumUtil
{
  /**
   * Parses the string representation of a Postgres/Greenplum array.
   *
   * The Greenplum driver can't return arrays natively, so we have to parse it manually.
   */
  public static int[] parseIntArray(String array)
  {
    if (array == null || array.length() < 3) return new int[0];
    String clean  = array.substring(1, array.length() - 1);
    String[] ids = clean.split(",");
    int[] result = new int[ids.length];
    for (int i=0; i < ids.length; i++)
    {
      result[i] = StringUtil.getIntValue(ids[i], 0);
    }
    return result;

  }
  /**
   * Parses the string representation of a Postgres/Greenplum array.
   *
   * The Greenplum driver can't return arrays natively, so we have to parse it manually.
   */
  public static String[] parseStringArray(String array)
  {
    if (array == null || array.length() < 3) return new String[0];
    String clean  = array.substring(1, array.length() - 1);
    return clean.split(",");
  }

  public static String getDatabaseVersionString(WbConnection conn)
  {
    if (conn.isBusy()) return null;

    Statement stmt = null;
    ResultSet rs = null;
    String version = null;

    try
    {
      if (conn.getUrl().startsWith("jdbc:postgresql"))
      {
        stmt = conn.createStatement();
        rs = stmt.executeQuery("select version()");
        if (rs.next())
        {
          String dbVersion = rs.getString(1);
          Pattern gpVersion = Pattern.compile("Greenplum Database ([0-9.]+)", Pattern.CASE_INSENSITIVE);
          Matcher m = gpVersion.matcher(dbVersion);
          if (m.find())
          {
            version = m.group(1);
          }
        }
      }
      else
      {
        version = conn.getSqlConnection().getMetaData().getDatabaseProductVersion();
      }
    }
    catch (Throwable ex)
    {
      LogMgr.logWarning(new CallerInfo(){}, "Could not retrieve database version using version()" , ex);
      try
      {
        version = conn.getSqlConnection().getMetaData().getDatabaseProductVersion();
      }
      catch (Throwable th)
      {
        // ignore
      }
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return version;
  }

  public static VersionNumber getDatabaseVersion(WbConnection conn)
  {
    String versionString = getDatabaseVersionString(conn);
    return new VersionNumber(versionString);
  }

}
