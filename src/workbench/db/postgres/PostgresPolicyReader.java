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
package workbench.db.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Savepoint;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;

import workbench.db.JdbcUtils;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresPolicyReader
{

  public String getTablePolicies(WbConnection conn, TableIdentifier table)
  {

    boolean isPg10 = JdbcUtils.hasMinimumServerVersion(conn, "10.0");

    String permissiveCol;
    if (isPg10)
    {
      permissiveCol = "p.polpermissive";
    }
    else
    {
      permissiveCol = "null as polpermissive";
    }

    String query =
      "select polname, \n" +
      "       pg_get_expr(p.polqual, p.polrelid, true) as expression, \n" +
      "       case p.polcmd \n" +
      "         when 'r' then 'SELECT' \n" +
      "         when 'a' then 'INSERT' \n" +
      "         when 'w' then 'UPDATE' \n" +
      "         when 'd' then 'DELETE' \n" +
      "         else 'ALL' \n" +
      "       end as command, \n" +
      "       " + permissiveCol + ", \n" +
      "       (select string_agg(quote_ident(rolname), ',') from pg_roles r where r.oid = any(p.polroles)) as roles, \n" +
      "       pg_get_expr(p.polwithcheck, p.polrelid, true) as with_check, \n" +
      "       t.relrowsecurity, \n" +
      "       t.relforcerowsecurity \n" +
      "from pg_policy p \n" +
      "  join pg_class t on t.oid = p.polrelid \n" +
      "where p.polrelid = cast(? as regclass)\n " +
      "order by p.polname";

    String tname = table.getFullyQualifiedName(conn);

    PreparedStatement pstmt = null;
    ResultSet rs = null;
    Savepoint sp = null;

    final CallerInfo ci = new CallerInfo(){};

    LogMgr.logMetadataSql(ci, "table policies", query, tname);

    StringBuilder policies = new StringBuilder(100);
    boolean rlsEnabled = false;
    boolean forceRls = false;

    try
    {
      sp = conn.setSavepoint();
      pstmt = conn.getSqlConnection().prepareStatement(query);
      pstmt.setString(1, tname);
      rs = pstmt.executeQuery();

      while (rs.next())
      {
        String name = rs.getString("polname");
        String expr = rs.getString("expression");
        String command = rs.getString("command");
        boolean permissive = rs.getBoolean("polpermissive");
        rlsEnabled = rs.getBoolean("relrowsecurity");
        forceRls = rs.getBoolean("relforcerowsecurity");

        String withCheck = rs.getString("with_check");
        String roles = rs.getString("roles");

        String policy = "CREATE POLICY " + SqlUtil.quoteObjectname(name) + " ON " + tname + "\n";
        if (isPg10)
        {
          policy += "  AS " + (permissive ? "PERMISSIVE" : "RESTRICTIVE") + "\n";
        }
        policy += "  FOR " + command;

        if (StringUtil.isNonBlank(roles))
        {
          policy += "\n  TO " + roles;
        }

        if (StringUtil.isNonBlank(expr))
        {
          policy += "\n  USING (" + expr + ")";
        }

        if (StringUtil.isNonBlank(withCheck))
        {
          policy += "\n  WITH CHECK (" + withCheck + ")";
        }
        policy += ";\n";

        if (policies.length() >  0)
        {
          policies.append('\n');
        }
        policies.append(policy);
      }
      conn.releaseSavepoint(sp);
    }
    catch (Exception ex)
    {
      conn.rollback(sp);
      LogMgr.logMetadataError(ci, ex, "table policies", query, tname);
    }
    finally
    {
      SqlUtil.close(rs, pstmt);
    }

    if (policies.length() > 0 && !rlsEnabled)
    {
      policies.append("\nALTER TABLE " + tname + " DISABLE ROW LEVEL SECURITY;");
    }

    if (rlsEnabled)
    {
      policies.append("\nALTER TABLE " + tname + " ENABLE ROW LEVEL SECURITY;");
    }

    if (forceRls)
    {
      policies.append("\nALTER TABLE " + tname + " FORCE ROW LEVEL SECURITY;\n");
    }

    return policies.toString();
  }
}
