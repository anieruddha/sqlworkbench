/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2017 Thomas Kellerer.
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
package workbench.db;

/**
 *
 * @author Thomas Kellerer
 */
public enum DBID
{
  Oracle("oracle"),
  Postgres("postgresql"),
  SQL_Server("microsoft_sql_server"),
  Vertica("vertica_database"),
  MySQL("mysql"),
  MariaDB("mariadb"),
  Firebird("firebird"),
  DB2_LUW("db2"),  // Linux, Unix, Windows
  DB2_ISERIES("db2i"),  // AS/400 iSeries
  DB2_ZOS("db2h"),  // z/OS
  SQLite("sqlite"),
  SQL_Anywhere("sql_anywhere"),
  Teradata("teradata"),
  H2("h2"),
  HSQLDB("hsql_database_engine"),
  Derby("apache_derby"),
  OPENEDGE("openedge"),
  Greenplum("greenplum"),
  HANA("hdb"),
  Cubrid("cubrid"),
  Informix("informix_dynamic_server"),
  Exasol("exasolution"),
  SAP_DB("sap_db"),
  Clickhouse("clickhouse"),
  MonetDB("monetdb"),
  Ingres("ingres"),
  Redshift("redshift"),
  Unknown("_$unknown$_");

  private String dbid;

  private DBID(String id)
  {
    dbid = id;
  }

  public String getId()
  {
    return dbid;
  }

  public boolean isDB(String id)
  {
    return this.dbid.equalsIgnoreCase(id);
  }

  public boolean isDB(WbConnection conn)
  {
    if (conn == null) return false;
    return this.dbid.equalsIgnoreCase(conn.getDbId());
  }

  public static DBID fromConnection(WbConnection conn)
  {
    if (conn == null) return Unknown;
    return fromID(conn.getDbId());
  }

  public static DBID fromID(String dbid)
  {
    for (DBID id : values())
    {
      if (id.isDB(dbid)) return id;
    }
    return Unknown;
  }

  public static String generateId(String product)
  {
    String id = product.replaceAll("[ \\(\\)\\[\\]/$,.'=\"]", "_").toLowerCase();

    if (product.startsWith("DB2"))
    {
      // DB/2 for Host-Systems
      // apparently DB2 for z/OS identifies itself as "DB2" whereas
      // DB2 for AS/400 identifies itself as "DB2 UDB for AS/400"
      if (product.contains("AS/400") || product.contains("iSeries"))
      {
        id = DBID.DB2_ISERIES.getId();
      }
      else if(product.equals("DB2"))
      {
        id = DBID.DB2_ZOS.getId();
      }
      else
      {
        // Everything else is LUW (Linux, Unix, Windows)
        id = DBID.DB2_LUW.getId();
      }
    }
    else if (product.startsWith("HSQL"))
    {
      // As the version number is appended to the productname
      // we need to ignore that here. The properties configured
      // in workbench.settings using the DBID are (currently) identically
      // for all HSQL versions.
      id = "hsql_database_engine";
    }
    else if (product.toLowerCase().contains("ucanaccess"))
    {
      id = "ucanaccess";
    }
    return id;
  }


}
