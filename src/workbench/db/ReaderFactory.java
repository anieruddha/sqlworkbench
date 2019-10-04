/*
 * ReaderFactory.java
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

import workbench.resource.Settings;

import workbench.db.clickhouse.ClickhouseViewReader;
import workbench.db.cubrid.CubridSequenceReader;
import workbench.db.derby.DerbyConstraintReader;
import workbench.db.derby.DerbySequenceReader;
import workbench.db.derby.DerbySynonymReader;
import workbench.db.firebird.FirebirdConstraintReader;
import workbench.db.firebird.FirebirdIndexReader;
import workbench.db.firebird.FirebirdProcedureReader;
import workbench.db.firebird.FirebirdSequenceReader;
import workbench.db.firstsql.FirstSqlConstraintReader;
import workbench.db.h2database.H2ConstraintReader;
import workbench.db.h2database.H2IndexReader;
import workbench.db.h2database.H2SequenceReader;
import workbench.db.h2database.H2UniqueConstraintReader;
import workbench.db.hana.HanaProcedureReader;
import workbench.db.hana.HanaSequenceReader;
import workbench.db.hana.HanaSynonymReader;
import workbench.db.hana.HanaViewReader;
import workbench.db.hsqldb.HsqlConstraintReader;
import workbench.db.hsqldb.HsqlIndexReader;
import workbench.db.hsqldb.HsqlSequenceReader;
import workbench.db.hsqldb.HsqlSynonymReader;
import workbench.db.hsqldb.HsqlUniqueConstraintReader;
import workbench.db.ibm.DB2UniqueConstraintReader;
import workbench.db.ibm.Db2ConstraintReader;
import workbench.db.ibm.Db2IndexReader;
import workbench.db.ibm.Db2ProcedureReader;
import workbench.db.ibm.Db2SequenceReader;
import workbench.db.ibm.Db2SynonymReader;
import workbench.db.ibm.InformixProcedureReader;
import workbench.db.ibm.InformixSequenceReader;
import workbench.db.ibm.InformixSynonymReader;
import workbench.db.ingres.IngresSequenceReader;
import workbench.db.ingres.IngresSynonymReader;
import workbench.db.mariadb.MariaDBSequenceReader;
import workbench.db.monetdb.MonetDbIndexReader;
import workbench.db.monetdb.MonetDbProcedureReader;
import workbench.db.monetdb.MonetDbSequenceReader;
import workbench.db.mssql.SqlServerConstraintReader;
import workbench.db.mssql.SqlServerIndexReader;
import workbench.db.mssql.SqlServerProcedureReader;
import workbench.db.mssql.SqlServerSequenceReader;
import workbench.db.mssql.SqlServerSynonymReader;
import workbench.db.mssql.SqlServerUniqueConstraintReader;
import workbench.db.mssql.SqlServerViewReader;
import workbench.db.mysql.MySQLConstraintReader;
import workbench.db.mysql.MySQLIndexReader;
import workbench.db.mysql.MySQLViewReader;
import workbench.db.mysql.MySqlProcedureReader;
import workbench.db.nuodb.NuoDBSequenceReader;
import workbench.db.oracle.OracleConstraintReader;
import workbench.db.oracle.OracleErrorInformationReader;
import workbench.db.oracle.OracleIndexReader;
import workbench.db.oracle.OracleProcedureReader;
import workbench.db.oracle.OracleSequenceReader;
import workbench.db.oracle.OracleSynonymReader;
import workbench.db.oracle.OracleUniqueConstraintReader;
import workbench.db.oracle.OracleViewReader;
import workbench.db.postgres.PostgresConstraintReader;
import workbench.db.postgres.PostgresIndexReader;
import workbench.db.postgres.PostgresProcedureReader;
import workbench.db.postgres.PostgresSequenceReader;
import workbench.db.postgres.PostgresUniqueConstraintReader;
import workbench.db.postgres.PostgresViewReader;
import workbench.db.progress.OpenEdgeSequenceReader;
import workbench.db.progress.OpenEdgeSynonymReader;
import workbench.db.redshift.RedshiftUDFReader;
import workbench.db.teradata.TeradataIndexReader;
import workbench.db.teradata.TeradataProcedureReader;
import workbench.db.vertica.VerticaSequenceReader;

/**
 * A factory to create instances of the various readers specific for a DBMS.
 *
 * @author Thomas Kellerer
 */
public class ReaderFactory
{
  public static ProcedureReader getProcedureReader(DbMetadata meta)
  {
    DBID dbid = DBID.fromID(meta.getDbId());
    switch (dbid)
    {
      case DB2_LUW:
      case DB2_ISERIES:
      case DB2_ZOS:
        return new Db2ProcedureReader(meta.getWbConnection(), meta.getDbId());
      case Oracle:
        return new OracleProcedureReader(meta.getWbConnection());
      case Postgres:
      case Greenplum:
        return new PostgresProcedureReader(meta.getWbConnection());
      case Redshift:
        return new RedshiftUDFReader(meta.getWbConnection());
      case Firebird:
        return new FirebirdProcedureReader(meta.getWbConnection());
      case SQL_Server:
        boolean useJdbc = Settings.getInstance().getBoolProperty("workbench.db.mssql.usejdbcprocreader", false);
        if (!useJdbc)
        {
          return new SqlServerProcedureReader(meta.getWbConnection());
        }
      case MySQL:
      case MariaDB:
        return new MySqlProcedureReader(meta.getWbConnection());
      case Teradata:
        return new TeradataProcedureReader(meta.getWbConnection());
      case MonetDB:
        if (!Settings.getInstance().getBoolProperty("workbench.db.monetdb.procedurelist.usedriver"))
        {
          return new MonetDbProcedureReader(meta.getWbConnection());
        }
      case Informix:
        if (Settings.getInstance().getBoolProperty("workbench.db.informix_dynamic_server.procedurelist.usecustom", true))
        {
          return new InformixProcedureReader(meta.getWbConnection());
        }
      case HANA:
        return new HanaProcedureReader(meta.getWbConnection());
    }
    return new JdbcProcedureReader(meta.getWbConnection());
  }

  public static SequenceReader getSequenceReader(WbConnection con)
  {
    DbMetadata meta = con.getMetadata();
    DBID dbid = DBID.fromConnection(con);
    switch (dbid)
    {
      case Postgres:
      case Greenplum:
      case Redshift:
        return new PostgresSequenceReader(con);
      case Oracle:
        return new OracleSequenceReader(con);
      case HSQLDB:
        return new HsqlSequenceReader(con);
      case Derby:
        if (JdbcUtils.hasMinimumServerVersion(con, "10.6"))
        {
          return new DerbySequenceReader(con);
        }
      case H2:
        return new H2SequenceReader(con);
      case Firebird:
        return new FirebirdSequenceReader(con);
      case DB2_ISERIES:
      case DB2_LUW:
      case DB2_ZOS:
        return new Db2SequenceReader(con, meta.getDbId());
      case Cubrid:
        return new CubridSequenceReader(con);
      case Vertica:
        return new VerticaSequenceReader(con);
      case SQL_Server:
        return new SqlServerSequenceReader(con);
      case Informix:
        return new InformixSequenceReader(con);
      case Ingres:
        return new IngresSequenceReader(con);
      case MonetDB:
        return new MonetDbSequenceReader(con);
      case HANA:
        return new HanaSequenceReader(con);
      case OPENEDGE:
        return new OpenEdgeSequenceReader(con);
      case MariaDB:
        if (JdbcUtils.hasMinimumServerVersion(con, "10.3"))
        {
          return new MariaDBSequenceReader(con);
        }
    }
    if (con.getDbId().equals("nuodb"))
    {
      return new NuoDBSequenceReader(con);
    }
    return null;
  }

  public static IndexReader getIndexReader(DbMetadata meta)
  {
    DBID dbid = DBID.fromID(meta.getDbId());
    switch (dbid)
    {
      case Oracle:
        return new OracleIndexReader(meta);
      case Postgres:
      case Greenplum:
      case Redshift:
        return new PostgresIndexReader(meta);
      case H2:
        return new H2IndexReader(meta);
      case HSQLDB:
        return new HsqlIndexReader(meta);
      case Firebird:
        if (JdbcUtils.hasMinimumServerVersion(meta.getWbConnection(), "2.5"))
        {
          return new FirebirdIndexReader(meta);
        }
      case MySQL:
      case MariaDB:
        return new MySQLIndexReader(meta);
      case SQL_Server:
        return new SqlServerIndexReader(meta);
      case DB2_LUW:
        return new Db2IndexReader(meta);
      case Teradata:
        return new TeradataIndexReader(meta);
      case MonetDB:
        return new MonetDbIndexReader(meta);
    }
    return new JdbcIndexReader(meta);
  }

  public static ConstraintReader getConstraintReader(DbMetadata meta)
  {
    DBID dbid = DBID.fromID(meta.getDbId());
    switch (dbid)
    {
      case Postgres:
      case Greenplum:
      case Redshift:
        return new PostgresConstraintReader(meta.getDbId());
      case Oracle:
        return new OracleConstraintReader(meta.getDbId());
      case HSQLDB:
        return new HsqlConstraintReader(meta.getWbConnection());
      case SQL_Server:
        return new SqlServerConstraintReader(meta.getWbConnection());
      case DB2_ISERIES:
      case DB2_LUW:
      case DB2_ZOS:
        return new Db2ConstraintReader(meta.getWbConnection());
      case Firebird:
        return new FirebirdConstraintReader();
      case H2:
        return new H2ConstraintReader();
      case Derby:
        return new DerbyConstraintReader();
      case MySQL:
        if (JdbcUtils.hasMinimumServerVersion(meta.getWbConnection(), "8.0.16"))
        {
          return new MySQLConstraintReader();
        }
      case MariaDB:
        if (JdbcUtils.hasMinimumServerVersion(meta.getWbConnection(), "10.2"))
        {
          return new MySQLConstraintReader();
        }
    }

    if (dbid.getId().startsWith("adaptive_server"))
    {
      return new SybaseConstraintReader(meta.getWbConnection());
    }
    if (dbid.getId().startsWith("firstsql"))
    {
      return new FirstSqlConstraintReader();
    }
    return ConstraintReader.NULL_READER;
  }


  public static UniqueConstraintReader getUniqueConstraintReader(WbConnection connection)
  {
    if (connection == null) return null;

    DBID dbid = DBID.fromConnection(connection);

    switch (dbid)
    {
      case Postgres:
      case Greenplum:
      case Redshift:
        return new PostgresUniqueConstraintReader();
      case Oracle:
        return new OracleUniqueConstraintReader();
      case DB2_LUW:
      case DB2_ZOS:
        return new DB2UniqueConstraintReader();
      case SQL_Server:
        return new SqlServerUniqueConstraintReader();
      case HSQLDB:
        return new HsqlUniqueConstraintReader();
      case H2:
        return new H2UniqueConstraintReader();
    }
    return null;
  }

  public static ViewReader createViewReader(WbConnection con)
  {
    DBID dbid = DBID.fromID(con.getDbId());
    switch (dbid)
    {
      case Postgres:
        return new PostgresViewReader(con);
      case MySQL:
      case MariaDB:
        return new MySQLViewReader(con);
      case Oracle:
        return new OracleViewReader(con);
      case SQL_Server:
        return new SqlServerViewReader(con);
      case HANA:
        return new HanaViewReader(con);
      case Clickhouse:
        return new ClickhouseViewReader(con);
    }
    return new DefaultViewReader(con);
  }

  public static SynonymReader getSynonymReader(WbConnection conn)
  {
    if (conn == null) return null;
    DBID dbid = DBID.fromID(conn.getDbId());
    switch (dbid)
    {
      case Oracle:
        return new OracleSynonymReader();
      case Derby:
        return new DerbySynonymReader();
      case SQL_Server:
        if (SqlServerSynonymReader.supportsSynonyms(conn))
        {
          return new SqlServerSynonymReader(conn.getMetadata());
        }
      case DB2_LUW:
      case DB2_ISERIES:
      case DB2_ZOS:
        return new Db2SynonymReader();
      case Ingres:
        return new IngresSynonymReader();
      case Informix:
        return new InformixSynonymReader();
      case HANA:
        return new HanaSynonymReader();
      case OPENEDGE:
        return new OpenEdgeSynonymReader();
      case HSQLDB:
        if (JdbcUtils.hasMinimumServerVersion(conn, "2.3.4"))
        {
          return new HsqlSynonymReader();
        }
    }
    return null;
  }

  public static ErrorInformationReader getErrorInformationReader(WbConnection conn)
  {
    if (conn == null) return null;
    if (conn.getMetadata().isOracle())
    {
      return new OracleErrorInformationReader(conn);
    }
    return null;
  }
}
