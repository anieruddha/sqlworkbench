/*
 * TableSourceBuilderFactory.java
 *
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
package workbench.db;

import workbench.db.derby.DerbyTableSourceBuilder;
import workbench.db.exasol.ExasolTableSourceBuilder;
import workbench.db.firebird.FirebirdTableSourceBuilder;
import workbench.db.greenplum.GreenplumTableSourceBuilder;
import workbench.db.h2database.H2TableSourceBuilder;
import workbench.db.hana.HanaTableSourceBuilder;
import workbench.db.hsqldb.HsqlTableSourceBuilder;
import workbench.db.ibm.Db2TableSourceBuilder;
import workbench.db.ibm.InformixTableSourceBuilder;
import workbench.db.mssql.SqlServerTableSourceBuilder;
import workbench.db.mysql.MySQLTableSourceBuilder;
import workbench.db.oracle.OracleTableSourceBuilder;
import workbench.db.postgres.PostgresTableSourceBuilder;
import workbench.db.redshift.RedshiftTableSourceBuilder;

/**
 * A factory to create a TableSourceBuilder.
 *
 * @author Thomas Kellerer
 */
public class TableSourceBuilderFactory
{

  public static TableSourceBuilder getBuilder(WbConnection con)
  {
    switch (DBID.fromConnection(con))
    {
      case Postgres:
        return new PostgresTableSourceBuilder(con);
      case Greenplum:
        return new GreenplumTableSourceBuilder(con);
      case Redshift:
        return new RedshiftTableSourceBuilder(con);
      case Derby:
        return new DerbyTableSourceBuilder(con);
      case Oracle:
        return new OracleTableSourceBuilder(con);
      case H2:
        return new H2TableSourceBuilder(con);
      case MySQL:
      case MariaDB:
        return new MySQLTableSourceBuilder(con);
      case SQL_Server:
        return new SqlServerTableSourceBuilder(con);
      case HSQLDB:
        return new HsqlTableSourceBuilder(con);
      case DB2_LUW:
        return new Db2TableSourceBuilder(con);
      case Informix:
        return new InformixTableSourceBuilder(con);
      case Firebird:
        return new FirebirdTableSourceBuilder(con);
      case HANA:
        return new HanaTableSourceBuilder(con);
      case Exasol:
        return new ExasolTableSourceBuilder(con);
      default:
        return new TableSourceBuilder(con);
    }
  }

}
