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
package workbench.db.greenplum;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.JdbcUtils;
import workbench.db.ObjectSourceOptions;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.CharacterRange;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbStringTokenizer;


/**
 * A class to "cleanup" the reported table type for MATERIALZED VIEWS.
 *
 * The JDBC driver returns MVIEWS with the type "TABLE" which is not useful when displaying
 * objects in the DbExplorer.
 *
 * This class processes the retrieved objects and updates the object type accordingly
 *
 * @author Thomas Kellerer
 */
public class GreenplumExternalTableReader
{
  public static final String EXT_TABLE_TYPE = "EXTERNAL TABLE";

  /**
   * Returns a list with all external tables based on the filter criteria.
   */
  public List<TableIdentifier> getExternalTables(WbConnection connection, String schemaPattern, String namePattern)
  {
    if (connection == null)
    {
      return Collections.emptyList();
    }

    List<TableIdentifier> result = new ArrayList<>();

    final CallerInfo ci = new CallerInfo(){};

    StringBuilder sql = new StringBuilder(50);
    sql.append(
      "select s.nspname as schemaname, \n" +
      "       c.relname as tablename \n" +
      "from pg_exttable t\n" +
      "  join pg_class c on c.oid = t.reloid\n" +
      "  join pg_namespace s on s.oid = c.relnamespace");

    boolean whereAdded = false;
    if (StringUtil.isNonBlank(schemaPattern))
    {
      sql.append("\nWHERE ");
      SqlUtil.appendExpression(sql, "s.nspname", schemaPattern, connection);
      whereAdded = true;
    }

    if (StringUtil.isNonBlank(namePattern))
    {
      if (whereAdded) sql.append("\n  AND ");
      else sql.append("\nWHERE ");
      SqlUtil.appendExpression(sql, "c.relname", namePattern, connection);
    }

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logDebug(ci, "Retrieving external tables using:\n" + sql);
    }

    Statement stmt = null;
    ResultSet rs = null;
    Savepoint sp = null;
    try
    {
      sp = connection.setSavepoint();
      stmt = connection.getSqlConnection().createStatement();
      rs = stmt.executeQuery(sql.toString());
      while (rs.next())
      {
        String schema = rs.getString(1);
        String table = rs.getString(2);
        TableIdentifier tbl = new TableIdentifier(schema, table);
        tbl.setType(EXT_TABLE_TYPE);
        result.add(tbl);
      }
      connection.releaseSavepoint(sp);
    }
    catch (Exception e)
    {
      connection.rollback(sp);
      LogMgr.logWarning(ci, "Error retrieving external tables using:\n" + sql, e);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return result;
  }

  public void readTableOptions(WbConnection conn, TableIdentifier tbl, List<ColumnIdentifier> columns)
  {
    final CallerInfo ci = new CallerInfo(){};
    ObjectSourceOptions tblOptions = tbl.getSourceOptions();

    String sql = null;

    if (JdbcUtils.hasMinimumServerVersion(conn, "5.0"))
    {
      sql =
        "select t.urilocation, t.execlocation, t.fmttype, t.fmtopts, t.command, \n" +
        "       t.rejectlimit, t.rejectlimittype, t.encoding, t.writable, \n" +
        "       p.attrnums \n" +
        "from pg_exttable t\n" +
        "  join pg_class c on c.oid = t.reloid\n" +
        "  join pg_namespace s on s.oid = c.relnamespace\n" +
        "  left join pg_catalog.gp_distribution_policy p on p.localoid = c.oid \n" +
        " where c.relname = ? \n" +
        "   and s.nspname = ?";
    }
    else
    {
      sql =
        "select t.location as urilocation, null::text[] as execlocation, t.fmttype, t.fmtopts, t.command, \n" +
        "       t.rejectlimit, t.rejectlimittype, t.encoding, t.writable \n" +
        "       p.attrnums \n" +
        "from pg_exttable t \n" +
        "  join pg_class c on c.oid = t.reloid\n" +
        "  join pg_namespace s on s.oid = c.relnamespace" +
        "  left join pg_catalog.gp_distribution_policy p on p.localoid = c.oid \n" +
        " where c.relname = ? \n" +
        "   and s.nspname = ?";
    }


    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logDebug(ci, "Retrieving external tables definition using :\n" + SqlUtil.replaceParameters(sql, tbl.getTableName(), tbl.getSchema()));
    }

    PreparedStatement stmt = null;
    ResultSet rs = null;

    Savepoint sp = null;
    String source = "";
    try
    {
      sp = conn.setSavepoint();
      stmt = conn.getSqlConnection().prepareStatement(sql);
      stmt.setString(1, tbl.getRawTableName());
      stmt.setString(2, tbl.getRawSchema());

      rs = stmt.executeQuery();
      if (rs.next())
      {
        String[] uris = GreenplumUtil.parseStringArray(rs.getString("urilocation"));
        String[] exec = GreenplumUtil.parseStringArray(rs.getString("execlocation"));
        String format = rs.getString("fmttype");
        String fmtOptions = rs.getString("fmtopts");
        String cmd = rs.getString("command");
        int rejectLimit = rs.getInt("rejectlimit");
        if (rs.wasNull())
        {
          rejectLimit = -1;
        }
        String limitType = rs.getString("rejectlimittype");
        String encoding = rs.getString("encoding");
        boolean writeable = rs.getBoolean("writable");
        String location = arrayToString(uris, true);
        String attrNums = rs.getString("attrnums");
        int[] distrCols = GreenplumUtil.parseIntArray(attrNums);

        // check for WEB tables
        if (StringUtil.isNonBlank(cmd) || hasHTTPLocations(uris))
        {
          tbl.setType("EXTERNAL WEB TABLE");
        }

        if (!location.isEmpty())
        {
          source += "LOCATION (\n  " + location + "\n)";
        }

        if (StringUtil.isNonBlank(cmd))
        {
          source += "EXECUTE '" + cmd + "' " + decodeExeclocations(exec);
        }

        if (format != null)
        {
          source += "\nFORMAT '";
          switch (format)
          {
            case "t":
              source += "TEXT";
              break;
            case "c":
              source += "CSV";
              break;
            case "a":
              source += "AVRO";
              break;
            case "p":
              source += "PARQUET";
              break;
            case "b":
              source += "CUSTOM";
              fmtOptions = parseCustomOptions(fmtOptions);
              break;
            default:
              break;
          }
          source += "'";
          if (StringUtil.isNonBlank(fmtOptions))
          {
            source += " (\n  " + fmtOptions + "\n)";
          }
        }

        if (writeable)
        {
          tblOptions.setTypeModifier("WRITABLE");
        }

        if (StringUtil.isNonBlank(encoding))
        {
          if (StringUtil.isNumber(encoding))
          {
            source += "\nENCODING " + encoding;
          }
          else
          {
            // apparently not a number, so we need quotes
            source += "\nENCODING '" + encoding + "'";
          }
        }

        if (rejectLimit > 0)
        {
          source  += "\nSEGMENT REJECT LIMIT " + rejectLimit;
        }

        if ("r".equals(limitType))
        {
          source +=" ROWS";
        }

        if (writeable)
        {
          String distribute = GreenplumTableSourceBuilder.getDistribution(distrCols, columns);
          if (StringUtil.isNonBlank(distribute))
          {
            source += "\n" + distribute;
          }
        }
      }
      conn.releaseSavepoint(sp);
    }
    catch (Exception e)
    {
      conn.rollback(sp);
      LogMgr.logWarning(ci, "Error retrieving external table definition using:\n" +  SqlUtil.replaceParameters(sql, tbl.getTableName(), tbl.getSchema()), e);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    tblOptions.appendTableOptionSQL(source);
  }

  private boolean hasHTTPLocations(String[] uris)
  {
    if (uris == null || uris.length == 0) return false;
    int counter = 0;
    for (String uri : uris)
    {
      if (uri != null && uri.toLowerCase().startsWith("http"))
      {
        counter ++;
      }
    }
    return counter == uris.length;
  }

  private String parseCustomOptions(String options)
  {
    if (options == null) return null;

    String sqlOptions = "";
    WbStringTokenizer tok = new WbStringTokenizer(options, " ", true, "'", true);
    int count = 0;
    while (tok.hasMoreTokens())
    {
      String element = tok.nextToken();
      if (element == null) continue;

      if ((count + 1) % 2 == 0)
      {
        String escaped = StringUtil.escapeText(element, CharacterRange.RANGE_CONTROL);
        if (!element.equals(escaped))
        {
          escaped = "E" + escaped;
        }
        sqlOptions += '=';
        sqlOptions += escaped;
      }
      else
      {
        if (count > 0)
        {
          sqlOptions += ",\n  ";
        }
        sqlOptions += element;
      }
      count ++;
    }
    return sqlOptions;
  }
  private String decodeExeclocations(String[] execLocs)
  {
    if (execLocs == null || execLocs.length == 0) return "";

    String result = "";
    for (String exec : execLocs)
    {
      if (StringUtil.isBlank(exec)) continue;
      switch (exec)
      {
        case "ALL_SEGMENTS":
          result += "ON ALL ";
          break;
        case "MASTER_ONLY":
          result += "ON MASTER ";
          break;
        default:
          result += exec;
      }
    }
    return result;
  }
  private String arrayToString(String[] elements, boolean useQuotes)
  {
    String result = "";
    if (elements == null) return "";
    int count = 0;
    for (String e : elements)
    {
      if (StringUtil.isBlank(e)) continue;
      if (count > 0)
      {
        result += ",\n  ";
      }

      if (useQuotes) result += '\'';
      result += e;
      if (useQuotes) result += '\'';
      count ++;
    }
    return result;
  }
}
