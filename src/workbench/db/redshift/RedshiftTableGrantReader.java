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
package workbench.db.redshift;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import workbench.log.CallerInfo;

import workbench.db.TableGrantReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.log.LogMgr;

import workbench.util.SqlUtil;


/*
 * @author  Miguel Cornejo Silva
 */
public class RedshiftTableGrantReader
  extends TableGrantReader
{

  public RedshiftTableGrantReader()
  {
  }

  @Override
  public StringBuilder getTableGrantSource(WbConnection dbConnection, TableIdentifier table)
  {
    String sql = "SELECT * FROM (\n" +
      "WITH objprivs AS ( \n" +
      "	SELECT objowner, \n" +
      "	schemaname, \n" +
      "	objname, \n" +
      "	objtype,\n" +
      "	CASE WHEN split_part(aclstring,'=',1)='' THEN 'PUBLIC' ELSE translate(trim(split_part(aclstring,'=',1)),'\"','') END::text AS grantee,\n" +
      "	translate(trim(split_part(aclstring,'/',2)),'\"','')::text AS grantor, \n" +
      "	trim(split_part(split_part(aclstring,'=',2),'/',1))::text AS privilege, \n" +
      "	CASE WHEN objtype = 'default acl' THEN objname \n" +
      "	WHEN objtype = 'function' AND regexp_instr(schemaname,'[^a-z]') > 0 THEN objname\n" +
      "	WHEN objtype = 'function' THEN QUOTE_IDENT(schemaname)||'.'||objname \n" +
      "	ELSE nvl(QUOTE_IDENT(schemaname)||'.'||QUOTE_IDENT(objname),QUOTE_IDENT(objname)) END::text as fullobjname,\n" +
      "	CASE WHEN split_part(aclstring,'=',1)='' THEN 'PUBLIC' \n" +
      "	ELSE trim(split_part(aclstring,'=',1)) \n" +
      "	END::text as splitgrantee,\n" +
      "	grantseq \n" +
      "	FROM (\n" +
      "		-- TABLE AND VIEW privileges\n" +
      "		SELECT pg_get_userbyid(b.relowner)::text AS objowner, \n" +
      "		trim(c.nspname)::text AS schemaname,  \n" +
      "		b.relname::text AS objname,\n" +
      "		CASE WHEN relkind='r' THEN 'table' ELSE 'view' END::text AS objtype, \n" +
      "		TRIM(SPLIT_PART(array_to_string(b.relacl,','), ',', NS.n))::text AS aclstring, \n" +
      "		NS.n as grantseq\n" +
      "		FROM \n" +
      "		(SELECT oid,generate_series(1,array_upper(relacl,1))  AS n FROM pg_class) NS\n" +
      "		inner join pg_class B ON b.oid = ns.oid AND  NS.n <= array_upper(b.relacl,1)\n" +
      "		join pg_namespace c on b.relnamespace = c.oid\n" +
      "		where relkind in ('r','v')\n" +
      "		UNION ALL\n" +
      "		-- SCHEMA privileges\n" +
      "		SELECT pg_get_userbyid(b.nspowner)::text AS objowner,\n" +
      "		null::text AS schemaname,\n" +
      "		b.nspname::text AS objname,\n" +
      "		'schema'::text AS objtype,\n" +
      "		TRIM(SPLIT_PART(array_to_string(b.nspacl,','), ',', NS.n))::text AS aclstring,\n" +
      "		NS.n as grantseq\n" +
      "		FROM \n" +
      "		(SELECT oid,generate_series(1,array_upper(nspacl,1)) AS n FROM pg_namespace) NS\n" +
      "		inner join pg_namespace B ON b.oid = ns.oid AND NS.n <= array_upper(b.nspacl,1)\n" +
      "		UNION ALL\n" +
      "		-- DATABASE privileges\n" +
      "		SELECT pg_get_userbyid(b.datdba)::text AS objowner,\n" +
      "		null::text AS schemaname,\n" +
      "		b.datname::text AS objname,\n" +
      "		'database'::text AS objtype,\n" +
      "		TRIM(SPLIT_PART(array_to_string(b.datacl,','), ',', NS.n))::text AS aclstring,\n" +
      "		NS.n as grantseq\n" +
      "		FROM \n" +
      "		(SELECT oid,generate_series(1,array_upper(datacl,1)) AS n FROM pg_database) NS\n" +
      "		inner join pg_database B ON b.oid = ns.oid AND NS.n <= array_upper(b.datacl,1) \n" +
      "		UNION ALL\n" +
      "		-- FUNCTION privileges \n" +
      "		SELECT pg_get_userbyid(b.proowner)::text AS objowner,\n" +
      "		trim(c.nspname)::text AS schemaname, \n" +
      "		textin(regprocedureout(b.oid::regprocedure))::text AS objname,\n" +
      "		'function'::text AS objtype,\n" +
      "		TRIM(SPLIT_PART(array_to_string(b.proacl,','), ',', NS.n))::text AS aclstring,\n" +
      "		NS.n as grantseq  \n" +
      "		FROM \n" +
      "		(SELECT oid,generate_series(1,array_upper(proacl,1)) AS n FROM pg_proc) NS\n" +
      "		inner join pg_proc B ON b.oid = ns.oid and NS.n <= array_upper(b.proacl,1)\n" +
      "		join pg_namespace c on b.pronamespace=c.oid \n" +
      "		UNION ALL\n" +
      "		-- LANGUAGE privileges\n" +
      "		SELECT null::text AS objowner,\n" +
      "		null::text AS schemaname,\n" +
      "		lanname::text AS objname,\n" +
      "		'language'::text AS objtype,\n" +
      "		TRIM(SPLIT_PART(array_to_string(b.lanacl,','), ',', NS.n))::text AS aclstring,\n" +
      "		NS.n as grantseq \n" +
      "		FROM \n" +
      "		(SELECT oid,generate_series(1,array_upper(lanacl,1)) AS n FROM pg_language) NS\n" +
      "		inner join pg_language B ON b.oid = ns.oid and NS.n <= array_upper(b.lanacl,1)\n" +
      "		UNION ALL\n" +
      "		-- DEFAULT ACL privileges\n" +
      "		SELECT pg_get_userbyid(b.defacluser)::text AS objowner,\n" +
      "		trim(c.nspname)::text AS schemaname,\n" +
      "		decode(b.defaclobjtype,'r','tables','f','functions')::text AS objname,\n" +
      "		'default acl'::text AS objtype,\n" +
      "		TRIM(SPLIT_PART(array_to_string(b.defaclacl,','), ',', NS.n))::text AS aclstring,\n" +
      "		NS.n as grantseq \n" +
      "		FROM \n" +
      "		(SELECT oid,generate_series(1,array_upper(defaclacl,1)) AS n FROM pg_default_acl) NS\n" +
      "		join pg_default_acl b ON b.oid = ns.oid and NS.n <= array_upper(b.defaclacl,1) \n" +
      "		left join  pg_namespace c on b.defaclnamespace=c.oid\n" +
      "	) \n" +
      "	where  (split_part(aclstring,'=',1) <> split_part(aclstring,'/',2) \n" +
      "	and split_part(aclstring,'=',1) <> 'rdsdb'\n" +
      "	and NOT (split_part(aclstring,'=',1)='' AND split_part(aclstring,'/',2) = 'rdsdb'))\n" +
      "	OR (split_part(aclstring,'=',1) = split_part(aclstring,'/',2) AND objtype='default acl')\n" +
      ")\n" +
      "-- Extract object GRANTS\n" +
      "SELECT objowner, schemaname, objname, objtype, grantor, grantee, 'grant' AS ddltype, grantseq,\n" +
      "decode(objtype,'database',0,'schema',1,'language',1,'table',2,'view',2,'function',2,'default acl',3) AS objseq,\n" +
      "CASE WHEN (grantor <> current_user AND grantor <> 'rdsdb' AND objtype <> 'default acl') THEN 'SET SESSION AUTHORIZATION '||QUOTE_IDENT(grantor)||';' ELSE '' END::text||\n" +
      "CASE WHEN objtype = 'default acl' THEN 'ALTER DEFAULT PRIVILEGES for user '||QUOTE_IDENT(grantor)||nvl(' in schema '||QUOTE_IDENT(schemaname)||' ',' ')\n" +
      "ELSE '' END::text||(CASE WHEN privilege = 'arwdRxt' OR privilege = 'a*r*w*d*R*x*t*' THEN 'GRANT ALL on '||fullobjname||' to '||splitgrantee||\n" +
      "(CASE WHEN privilege = 'a*r*w*d*R*x*t*' THEN ' with grant option;' ELSE ';' END::text) \n" +
      "when privilege = 'UC' OR privilege = 'U*C*' THEN 'GRANT ALL on '||objtype||' '||fullobjname||' to '||splitgrantee||\n" +
      "(CASE WHEN privilege = 'U*C*' THEN ' with grant option;' ELSE ';' END::text) \n" +
      "when privilege = 'CT' OR privilege = 'U*C*' THEN 'GRANT ALL on '||objtype||' '||fullobjname||' to '||splitgrantee||\n" +
      "(CASE WHEN privilege = 'C*T*' THEN ' with grant option;' ELSE ';' END::text)\n" +
      "ELSE  \n" +
      "(\n" +
      "CASE WHEN charindex('a',privilege) > 0 THEN 'GRANT INSERT on '||fullobjname||' to '||splitgrantee|| \n" +
      "(CASE WHEN charindex('a*',privilege) > 0 THEN ' with grant option;' ELSE ';' END::text) ELSE '' END::text||\n" +
      "CASE WHEN charindex('r',privilege) > 0 THEN 'GRANT SELECT on '||fullobjname||' to '||splitgrantee||\n" +
      "(CASE WHEN charindex('r*',privilege) > 0 THEN ' with grant option;' ELSE ';' END::text) ELSE '' END::text||\n" +
      "CASE WHEN charindex('w',privilege) > 0 THEN 'GRANT UPDATE on '||fullobjname||' to '||splitgrantee||\n" +
      "(CASE WHEN charindex('w*',privilege) > 0 THEN ' with grant option;' ELSE ';' END::text) ELSE '' END::text||\n" +
      "CASE WHEN charindex('d',privilege) > 0 THEN 'GRANT DELETE on '||fullobjname||' to '||splitgrantee||\n" +
      "(CASE WHEN charindex('d*',privilege) > 0 THEN ' with grant option;' ELSE ';' END::text) ELSE '' END::text||\n" +
      "CASE WHEN charindex('R',privilege) > 0 THEN 'GRANT RULE on '||fullobjname||' to '||splitgrantee||\n" +
      "(CASE WHEN charindex('R*',privilege) > 0 THEN ' with grant option;' ELSE ';' END::text) ELSE '' END::text||\n" +
      "CASE WHEN charindex('x',privilege) > 0 THEN 'GRANT REFERENCES on '||fullobjname||' to '||splitgrantee||\n" +
      "(CASE WHEN charindex('x*',privilege) > 0 THEN ' with grant option;' ELSE ';' END::text) ELSE '' END::text||\n" +
      "CASE WHEN charindex('t',privilege) > 0 THEN 'GRANT TRIGGER on '||fullobjname||' to '||splitgrantee||\n" +
      "(CASE WHEN charindex('t*',privilege) > 0 THEN ' with grant option;' ELSE ';' END::text) ELSE '' END::text||\n" +
      "CASE WHEN charindex('U',privilege) > 0 THEN 'GRANT USAGE on '||objtype||' '||fullobjname||' to '||splitgrantee||\n" +
      "(CASE WHEN charindex('U*',privilege) > 0 THEN ' with grant option;' ELSE ';' END::text) ELSE '' END::text||\n" +
      "CASE WHEN charindex('C',privilege) > 0 THEN 'GRANT CREATE on '||objtype||' '||fullobjname||' to '||splitgrantee||\n" +
      "(CASE WHEN charindex('C*',privilege) > 0 THEN ' with grant option;' ELSE ';' END::text) ELSE '' END::text||\n" +
      "CASE WHEN charindex('T',privilege) > 0 THEN 'GRANT TEMP on '||objtype||' '||fullobjname||' to '||splitgrantee||\n" +
      "(CASE WHEN charindex('T*',privilege) > 0 THEN ' with grant option;' ELSE ';' END::text) ELSE '' END::text||\n" +
      "CASE WHEN charindex('X',privilege) > 0 THEN 'GRANT EXECUTE on '||\n" +
      "(CASE WHEN objtype = 'default acl' THEN '' ELSE objtype||' ' END::text)||fullobjname||' to '||splitgrantee||\n" +
      "(CASE WHEN charindex('X*',privilege) > 0 THEN ' with grant option;' ELSE ';' END::text) ELSE '' END::text\n" +
      ") END::text)|| \n" +
      "CASE WHEN (grantor <> current_user AND grantor <> 'rdsdb' AND objtype <> 'default acl') THEN 'RESET SESSION AUTHORIZATION;' ELSE '' END::text AS ddl\n" +
      "FROM objprivs\n" +
      "UNION ALL\n" +
      "-- Extract object REVOKES\n" +
      "SELECT objowner, schemaname, objname, objtype, grantor, grantee, 'revoke'::text AS ddltype, grantseq,\n" +
      "decode(objtype,'default acl',0,'function',1,'table',1,'view',1,'schema',2,'language',2,'database',3) AS objseq,\n" +
      "CASE WHEN (grantor <> current_user AND grantor <> 'rdsdb' AND objtype <> 'default acl' AND grantor <> objowner) THEN 'SET SESSION AUTHORIZATION '||QUOTE_IDENT(grantor)||';' ELSE '' END::text||\n" +
      "(CASE WHEN objtype = 'default acl' THEN 'ALTER DEFAULT PRIVILEGES for user '||QUOTE_IDENT(grantor)||nvl(' in schema '||QUOTE_IDENT(schemaname)||' ',' ')\n" +
      "||'REVOKE ALL on '||fullobjname||' FROM '||splitgrantee||';'\n" +
      "ELSE 'REVOKE ALL on '||(CASE WHEN objtype = 'table' OR objtype = 'view' THEN '' ELSE objtype||' ' END::text)||fullobjname||' FROM '||splitgrantee||';' END::text)||\n" +
      "CASE WHEN (grantor <> current_user AND grantor <> 'rdsdb' AND objtype <> 'default acl' AND grantor <> objowner) THEN 'RESET SESSION AUTHORIZATION;' ELSE '' END::text AS ddl\n" +
      "FROM objprivs\n" +
      "WHERE NOT (objtype = 'default acl' AND grantee = 'PUBLIC' and objname='functions')\n" +
      "UNION ALL\n" +
      "-- Eliminate empty default ACLs\n" +
      "SELECT null::text AS objowner, trim(c.nspname)::text AS schemaname, decode(b.defaclobjtype,'r','tables','f','functions')::text AS objname,\n" +
      "		'default acl'::text AS objtype,  pg_get_userbyid(b.defacluser)::text AS grantor, null::text AS grantee, 'revoke'::text AS ddltype, 5 as grantseq, 5 AS objseq,\n" +
      "  'ALTER DEFAULT PRIVILEGES for user '||QUOTE_IDENT(pg_get_userbyid(b.defacluser))||nvl(' in schema '||QUOTE_IDENT(trim(c.nspname))||' ',' ')\n" +
      "||'GRANT ALL on '||decode(b.defaclobjtype,'r','tables','f','functions')||' TO '||QUOTE_IDENT(pg_get_userbyid(b.defacluser))||\n" +
      "CASE WHEN b.defaclobjtype = 'f' then ', PUBLIC;' ELSE ';' END::text AS ddl \n" +
      "		FROM pg_default_acl b \n" +
      "		LEFT JOIN  pg_namespace c on b.defaclnamespace = c.oid\n" +
      "		where EXISTS (select 1 where b.defaclacl='{}'::aclitem[]\n" +
      "		UNION ALL\n" +
      "		select 1 WHERE array_to_string(b.defaclacl,'')=('=X/'||QUOTE_IDENT(pg_get_userbyid(b.defacluser))))\n" +
      ") X\n" +
      "WHERE ddltype = 'grant' AND grantee <> 'PUBLIC' AND schemaname = ? AND objname = ? \n" +
      "ORDER BY schemaname, objname, grantseq";

    StringBuilder result = new StringBuilder(100);

    LogMgr.logMetadataSql(new CallerInfo(){}, "table grants", sql, table.getSchema(), table.getTableName());

    PreparedStatement stmt = null;
    ResultSet rs = null;

    try
    {
      stmt = dbConnection.getSqlConnection().prepareStatement(sql);
      stmt.setString(1, table.getSchema());
      stmt.setString(2, table.getTableName());
      rs = stmt.executeQuery();
      while (rs.next())
      {
        String grant = rs.getString("ddl");
        result.append(grant);
        result.append("\n");
      }
    }
    catch (SQLException ex)
    {
      LogMgr.logError(new CallerInfo(){}, "Error retrieving table source", ex);
      return null;
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return result;
  }
}
