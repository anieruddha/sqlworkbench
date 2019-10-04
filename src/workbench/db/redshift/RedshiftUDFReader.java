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
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import workbench.log.CallerInfo;

import workbench.db.ColumnIdentifier;
import workbench.db.JdbcProcedureReader;
import workbench.db.JdbcUtils;
import workbench.db.NoConfigException;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.WbConnection;
import workbench.db.postgres.PGProcName;
import workbench.db.postgres.PGType;
import workbench.db.postgres.PGTypeLookup;
import workbench.db.postgres.PostgresUtil;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.storage.DataStore;

import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/*
 * @author  Miguel Cornejo Silva
 */
public class RedshiftUDFReader
  extends JdbcProcedureReader
{
// Maps PG type names to Java types.
  private Map<String, Integer> pgType2Java;
  private PGTypeLookup pgTypes;
  private boolean useJDBC = false;

  public RedshiftUDFReader(WbConnection conn)
  {
    super(conn);
    try
    {
      this.useSavepoint = conn.supportsSavepoints();
    }
    catch (Throwable th)
    {
      this.useSavepoint = false;
    }
    this.useJDBC = PostgresUtil.isRedshift(conn);
  }

  @Override
  public void clearCache()
  {
    if (pgTypes != null)
    {
      pgTypes.clear();
      pgTypes = null;
    }
  }

  private Map<String, Integer> getJavaTypeMapping()
  {
    if (pgType2Java == null)
    {
      // This mapping has been copied from the JDBC driver.
      // This map is a private attribute of the class org.postgresql.jdbc2.TypeInfoCache
      // so, even if I hardcoded references to the Postgres driver I wouldn't be able
      // to use the information.
      pgType2Java = new HashMap<>();
      pgType2Java.put("int2", Integer.valueOf(Types.SMALLINT));
      pgType2Java.put("int4", Integer.valueOf(Types.INTEGER));
      pgType2Java.put("integer", Integer.valueOf(Types.INTEGER));
      pgType2Java.put("oid", Integer.valueOf(Types.BIGINT));
      pgType2Java.put("int8", Integer.valueOf(Types.BIGINT));
      pgType2Java.put("money", Integer.valueOf(Types.DOUBLE));
      pgType2Java.put("numeric", Integer.valueOf(Types.NUMERIC));
      pgType2Java.put("float4", Integer.valueOf(Types.REAL));
      pgType2Java.put("float8", Integer.valueOf(Types.DOUBLE));
      pgType2Java.put("char", Integer.valueOf(Types.CHAR));
      pgType2Java.put("bpchar", Integer.valueOf(Types.CHAR));
      pgType2Java.put("varchar", Integer.valueOf(Types.VARCHAR));
      pgType2Java.put("text", Integer.valueOf(Types.VARCHAR));
      pgType2Java.put("name", Integer.valueOf(Types.VARCHAR));
      pgType2Java.put("bytea", Integer.valueOf(Types.BINARY));
      pgType2Java.put("bool", Integer.valueOf(Types.BIT));
      pgType2Java.put("bit", Integer.valueOf(Types.BIT));
      pgType2Java.put("date", Integer.valueOf(Types.DATE));
      pgType2Java.put("time", Integer.valueOf(Types.TIME));
      pgType2Java.put("timetz", Integer.valueOf(Types.TIME));
      pgType2Java.put("timestamp", Integer.valueOf(Types.TIMESTAMP));
      pgType2Java.put("timestamptz", Integer.valueOf(Types.TIMESTAMP_WITH_TIMEZONE));
    }
    return Collections.unmodifiableMap(pgType2Java);
  }

  private Integer getJavaType(String pgType)
  {
    Integer i = getJavaTypeMapping().get(pgType);
    if (i == null) return Integer.valueOf(Types.OTHER);
    return i;
  }

  public PGTypeLookup getTypeLookup()
  {
    if (pgTypes == null)
    {
      Map<Long, PGType> typeMap = new HashMap<>(300);
      Statement stmt = null;
      ResultSet rs = null;
      Savepoint sp = null;
      String sql =
        "select t.oid, format_type(t.oid, null), t.typtype, ns.nspname as schema_name \n" +
        "from pg_type t \n" +
        "  join pg_namespace ns on ns.oid = t.typnamespace";

      LogMgr.logMetadataSql(new CallerInfo(){}, "type information", sql);

      try
      {
        if (useSavepoint)
        {
          sp = connection.setSavepoint();
        }
        stmt = connection.createStatement();
        rs = stmt.executeQuery(sql);
        while (rs.next())
        {
          long oid = rs.getLong(1);
          String typeName = rs.getString(2);
          String typType = rs.getString(3);
          String schema = rs.getString(4);

          if (typeName.equals("character varying"))
          {
            typeName = "varchar";
          }
          typeName = getFQName(typType, schema, StringUtil.trimQuotes(typeName));
          PGType typ = new PGType(typeName, oid);
          typeMap.put(Long.valueOf(oid), typ);
        }
        connection.releaseSavepoint(sp);
      }
      catch (SQLException e)
      {
        connection.rollback(sp);
        LogMgr.logMetadataError(new CallerInfo(){}, e, "type information", sql);
        typeMap = Collections.emptyMap();
      }
      finally
      {
        SqlUtil.closeAll(rs, stmt);
      }
      pgTypes = new PGTypeLookup(typeMap);
    }
    return pgTypes;
  }

  private String getFQName(String type, String schema, String typName)
  {
    if ("c".equals(type))
    {
      if (typName.indexOf('.') > -1)
      {
        // already fully qualified
        return typName;
      }
      if (!typName.startsWith(schema))
      {
        return schema + "." + typName;
      }
    }
    return typName;
  }

  private String getTypeNameFromOid(long oid)
  {
    PGType typ = getTypeLookup().getTypeFromOID(Long.valueOf(oid));
    return typ.getTypeName();
  }

  @Override
  public DataStore getProcedures(String catalog, String schemaPattern, String procName)
    throws SQLException
  {
    DataStore fullDs = super.getProcedures(catalog, procName, procName);
    DataStore ds = buildProcedureListDataStore(this.connection.getMetadata(), true);

    if ("*".equals(schemaPattern) || "%".equals(schemaPattern))
    {
      schemaPattern = null;
    }

    String namePattern = null;
    if ("*".equals(procName) || "%".equals(procName))
    {
      namePattern = null;
    }
    else if (StringUtil.isNonBlank(procName))
    {
      PGProcName pg = new PGProcName(procName, getTypeLookup());
      namePattern = pg.getName();
    }

    for (int i = 0; i < fullDs.getRowCount(); i++)
    {
      String specname = (String)fullDs.getRow(i).getValue(ProcedureReader.COLUMN_IDX_PROC_LIST_SPECIFIC_NAME);
      String cat = (String)fullDs.getRow(i).getValue(ProcedureReader.COLUMN_IDX_PROC_LIST_CATALOG);
      String schema = (String)fullDs.getRow(i).getValue(ProcedureReader.COLUMN_IDX_PROC_LIST_SCHEMA);
      String displayName = (String)fullDs.getRow(i).getValue(ProcedureReader.COLUMN_IDX_PROC_LIST_NAME);
      String bfDisplayName = i > 0 ? (String)fullDs.getRow(i - 1).getValue(ProcedureReader.COLUMN_IDX_PROC_LIST_NAME) : "";
      Integer procType = (Integer)fullDs.getRow(i).getValue(ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE);
      String remark = (String)fullDs.getRow(i).getValue(ProcedureReader.COLUMN_IDX_PROC_LIST_REMARKS);
      ProcedureDefinition def = (ProcedureDefinition)fullDs.getRow(i).getUserObject();

      if ((namePattern == null || specname.contains(namePattern)) &&
        (schemaPattern == null || schema.equalsIgnoreCase(schemaPattern)) &&
        catalog.equals(cat) && !bfDisplayName.equals(displayName))
      {
        int row = ds.addRow();

        ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_SPECIFIC_NAME, specname);
        ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_CATALOG, cat);
        ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_SCHEMA, schema);
        ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME, displayName);
        ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE, procType);
        ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_REMARKS, remark);
        ds.getRow(row).setUserObject(def);
      }
    }

    return ds;
  }

  public ProcedureDefinition createDefinition(String schema, String name, String args, String types, String modes, String procId)
  {
    if (modes == null)
    {
      // modes will be null if all arguments are IN arguments
      modes = types.replaceAll("[0-9]+", "i");
    }
    PGProcName pname = new PGProcName(name, types, modes, getTypeLookup());

    ProcedureDefinition def = new ProcedureDefinition(null, schema, name, java.sql.DatabaseMetaData.procedureReturnsResult);

    List<String> argNames = StringUtil.stringToList(args, ";", true, true);
    List<String> argTypes = StringUtil.stringToList(types, ";", true, true);
    List<String> argModes = StringUtil.stringToList(modes, ";", true, true);
    List<ColumnIdentifier> cols = convertToColumns(argNames, argTypes, argModes);
    def.setParameters(cols);
    def.setDisplayName(pname.getFormattedName());
    def.setInternalIdentifier(procId);
    return def;
  }

  @Override
  public DataStore getProcedureColumns(ProcedureDefinition def)
    throws SQLException
  {
    if (!useJDBC && Settings.getInstance().getBoolProperty("workbench.db.postgresql.fixproctypes", true) &&
       JdbcUtils.hasMinimumServerVersion(connection, "8.4"))
    {
      PGProcName pgName = new PGProcName(def, getTypeLookup());
      return getColumns(def.getCatalog(), def.getSchema(), pgName);
    }
    else
    {
      return super.getProcedureColumns(def.getCatalog(), def.getSchema(), def.getProcedureName(), null);
    }
  }

  @Override
  public void readProcedureSource(ProcedureDefinition def, String catalogForSource, String schemaForSource)
    throws NoConfigException
  {
    PGProcName name = new PGProcName(def, getTypeLookup());

    String sql = "SELECT DDL \n" +
      " FROM ( WITH arguments \n" +
      " AS \n" +
      " (SELECT oid, \n" +
      "        i, \n" +
      "        arg_name[i] AS argument_name, \n" +
      "        arg_types[i -1] argument_type \n" +
      " FROM (SELECT generate_series(1,arg_count) AS i, \n" +
      "              arg_name, \n" +
      "              arg_types, \n" +
      "              oid \n" +
      "       FROM (SELECT oid, \n" +
      "                    proargnames arg_name, \n" +
      "                    proargtypes arg_types, \n" +
      "                    pronargs arg_count \n" +
      "             FROM pg_proc \n" +
      "             WHERE proowner != 1) t) t)  \n" +
      "      SELECT schemaname,udfname,udfoid,seq,trim (ddl) ddl  \n" +
      "      FROM (SELECT n.nspname AS schemaname, \n" +
      "                   p.proname AS udfname, \n" +
      "                   p.oid AS udfoid, \n" +
      "                   1000 AS seq, \n" +
      "                   ('CREATE FUNCTION ' || QUOTE_IDENT(p.proname) || ' \\(')::VARCHAR (MAX) AS ddl  \n" +
      "      FROM pg_proc p \n" +
      "   LEFT JOIN pg_namespace n ON n.oid = p.pronamespace \n" +
      " WHERE p.proowner != 1 \n" +
      " UNION ALL \n" +
      " SELECT n.nspname AS schemaname, \n" +
      "        p.proname AS udfname, \n" +
      "        p.oid AS udfoid, \n" +
      "        2000 + nvl(i,0) AS seq, \n" +
      "        CASE \n" +
      "          WHEN i = 1 THEN NVL (argument_name,'') || ' ' || format_type (argument_type,NULL) \n" +
      "          ELSE ',' || NVL (argument_name,'') || ' ' || format_type (argument_type,NULL) \n" +
      "        END AS ddl \n" +
      " FROM pg_proc p \n" +
      "   LEFT JOIN pg_namespace n ON n.oid = p.pronamespace \n" +
      "   LEFT JOIN arguments a ON a.oid = p.oid \n" +
      " WHERE p.proowner != 1 \n" +
      " UNION ALL \n" +
      " SELECT n.nspname AS schemaname, \n" +
      "        p.proname AS udfname, \n" +
      "        p.oid AS udfoid, \n" +
      "        3000 AS seq, \n" +
      "        '\\)\\n' AS ddl \n" +
      " FROM pg_proc p \n" +
      "   LEFT JOIN pg_namespace n ON n.oid = p.pronamespace \n" +
      " WHERE p.proowner != 1 \n" +
      " UNION ALL \n" +
      " SELECT n.nspname AS schemaname, \n" +
      "        p.proname AS udfname, \n" +
      "        p.oid AS udfoid, \n" +
      "        4000 AS seq, \n" +
      "        '  RETURNS ' || pg_catalog.format_type(p.prorettype,NULL) || '\\n'AS ddl \n" +
      " FROM pg_proc p \n" +
      "   LEFT JOIN pg_namespace n ON n.oid = p.pronamespace \n" +
      " WHERE p.proowner != 1 \n" +
      " UNION ALL \n" +
      " SELECT n.nspname AS schemaname, \n" +
      "        p.proname AS udfname, \n" +
      "        p.oid AS udfoid, \n" +
      "        5000 AS seq, \n" +
      "        CASE \n" +
      "          WHEN p.provolatile = 'v' THEN 'VOLATILE\\n' \n" +
      "          WHEN p.provolatile = 's' THEN 'STABLE\\n' \n" +
      "          WHEN p.provolatile = 'i' THEN 'IMMUTABLE\\n' \n" +
      "          ELSE '' \n" +
      "        END AS ddl \n" +
      " FROM pg_proc p \n" +
      "   LEFT JOIN pg_namespace n ON n.oid = p.pronamespace \n" +
      " WHERE p.proowner != 1 \n" +
      " UNION ALL \n" +
      " SELECT n.nspname AS schemaname, \n" +
      "        p.proname AS udfname, \n" +
      "        p.oid AS udfoid, \n" +
      "        6000 AS seq, \n" +
      "        'AS $$' AS ddl \n" +
      " FROM pg_proc p \n" +
      "   LEFT JOIN pg_namespace n ON n.oid = p.pronamespace \n" +
      " WHERE p.proowner != 1 \n" +
      " UNION ALL \n" +
      " SELECT n.nspname AS schemaname, \n" +
      "        p.proname AS udfname, \n" +
      "        p.oid AS udfoid, \n" +
      "        7000 AS seq, \n" +
      "        p.prosrc AS DDL \n" +
      " FROM pg_proc p \n" +
      "   LEFT JOIN pg_namespace n ON n.oid = p.pronamespace \n" +
      " WHERE p.proowner != 1 \n" +
      " UNION ALL \n" +
      " SELECT n.nspname AS schemaname, \n" +
      "        p.proname AS udfname, \n" +
      "        p.oid AS udfoid, \n" +
      "        8000 AS seq, \n" +
      "        '$$ LANGUAGE ' + lang.lanname || '\\n;\\n\\n' AS ddl \n" +
      " FROM pg_proc p \n" +
      "   LEFT JOIN pg_namespace n ON n.oid = p.pronamespace \n" +
      "   LEFT JOIN (SELECT oid, lanname FROM pg_language) lang ON p.prolang = lang.oid \n" +
      " WHERE p.proowner != 1)) \n";

    sql += "WHERE udfname = '" + name.getName() + "' \n";
    if (StringUtil.isNonBlank(def.getSchema()))
    {
      sql += "  AND schemaname = '" + def.getSchema() + "' \n";
    }
    sql += "  ORDER BY udfoid, seq \n";

    LogMgr.logMetadataSql(new CallerInfo(){}, "procedure source", sql);
    StringBuilder source = new StringBuilder(500);

    ResultSet rs = null;
    Savepoint sp = null;
    Statement stmt = null;

    try
    {
      if (useSavepoint)
      {
        sp = this.connection.setSavepoint();
      }
      stmt = connection.createStatementForQuery();
      rs = stmt.executeQuery(sql);

      while (rs.next())
      {
        source.append(rs.getString(1));
      }

      connection.releaseSavepoint(sp);

      if (StringUtil.isNonBlank(def.getComment()))
      {
        source.append("\nCOMMENT ON FUNCTION ");
        source.append(name.getFormattedName());
        source.append(" IS '");
        source.append(SqlUtil.escapeQuotes(def.getComment()));
        source.append("'\n;\n");
      }
    }
    catch (SQLException e)
    {
      source = new StringBuilder(ExceptionUtil.getDisplay(e));
      connection.rollback(sp);
      LogMgr.logMetadataError(new CallerInfo(){}, e, "procedure source", sql);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }

    def.setSource(source);
  }

  private CharSequence buildParameterList(String names, String types, String modes)
  {
    List<String> argNames = StringUtil.stringToList(names, ";", true, true);
    List<String> argTypes = StringUtil.stringToList(types, ";", true, true);
    List<String> argModes = StringUtil.stringToList(modes, ";", true, true);

    List<ColumnIdentifier> args = convertToColumns(argNames, argTypes, argModes);
    StringBuilder result = new StringBuilder(args.size() * 10);

    result.append('(');
    int paramCount = 0;
    for (ColumnIdentifier col : args)
    {
      String mode = col.getArgumentMode();
      if ("RETURN".equals(mode)) continue;

      if (paramCount > 0) result.append(", ");

      String argName = col.getColumnName();
      String type = col.getDbmsType();
      result.append(argName);
      result.append(' ');
      result.append(type);
      paramCount++;
    }
    return result;
  }

  protected StringBuilder getAggregateSource(PGProcName name, String schema)
  {
    String baseSelect = "SELECT a.aggtransfn, a.aggfinalfn, format_type(a.aggtranstype, null) as stype, a.agginitval, op.oprname ";
    String from =
      " FROM pg_proc p \n" +
      "  JOIN pg_namespace n ON p.pronamespace = n.oid \n" +
      "  JOIN pg_aggregate a ON a.aggfnoid = p.oid \n" +
      "  LEFT JOIN pg_operator op ON op.oid = a.aggsortop ";

    boolean hasSort = JdbcUtils.hasMinimumServerVersion(connection, "8.1");
    if (hasSort)
    {
      baseSelect += ", a.aggsortop ";
    }

    boolean hasParallel = JdbcUtils.hasMinimumServerVersion(connection, "9.6");
    baseSelect += ", " + (hasParallel ? "p.proparallel" : "null as proparallel");

    String sql = baseSelect + "\n" + from;
    sql += " WHERE p.proname = '" + name.getName() + "' ";
    if (StringUtil.isNonBlank(schema))
    {
      sql += " and n.nspname = '" + schema + "' ";
    }

    LogMgr.logMetadataSql(new CallerInfo(){}, "aggregate source", sql);
    StringBuilder source = new StringBuilder();
    ResultSet rs = null;
    Statement stmt = null;
    Savepoint sp = null;

    try
    {
      if (useSavepoint)
      {
        sp = this.connection.setSavepoint();
      }
      stmt = connection.createStatementForQuery();
      rs = stmt.executeQuery(sql);
      if (rs.next())
      {

        source.append("CREATE AGGREGATE ");
        source.append(name.getFormattedName());
        source.append("\n(\n");
        String sfunc = rs.getString("aggtransfn");
        source.append("  sfunc = ");
        source.append(sfunc);

        String stype = rs.getString("stype");
        source.append(",\n  stype = ");
        source.append(stype);

        String sortop = rs.getString("oprname");
        if (StringUtil.isNonBlank(sortop))
        {
          source.append(",\n  sortop = ");
          source.append(connection.getMetadata().quoteObjectname(sortop));
        }

        String finalfunc = rs.getString("aggfinalfn");
        if (StringUtil.isNonBlank(finalfunc) && !finalfunc.equals("-"))
        {
          source.append(",\n  finalfunc = ");
          source.append(finalfunc);
        }

        String initcond = rs.getString("agginitval");
        if (StringUtil.isNonBlank(initcond))
        {
          source.append(",\n  initcond = '");
          source.append(initcond);
          source.append('\'');
        }

        String parallel = rs.getString("proparallel");
        if (nonDefaultParallel(parallel))
        {
          source.append(",\n  parallel = ");
          source.append(codeToParallelType(parallel).toLowerCase());
        }
        source.append("\n);\n");
      }
      connection.releaseSavepoint(sp);
    }
    catch (SQLException e)
    {
      source = null;
      connection.rollback(sp);
      LogMgr.logMetadataError(new CallerInfo(){}, e, "aggregate source", sql);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return source;

  }

  private boolean nonDefaultParallel(String parallel)
  {
    if (parallel == null) return false;
    return !parallel.equals("u");
  }

  private String codeToParallelType(String code)
  {
    switch (code)
    {
      case "s":
        return "SAFE";
      case "r":
        return "RESTRICTED";
      case "u":
        return "UNSAFE";
    }
    return code;
  }

  /**
   * A workaround for pre 8.3 drivers so that argument names are retrieved properly
   * from the database. This was mainly inspired by the source code of pgAdmin III
   * and the 8.3 driver sources
   *
   * @param catalog
   * @param schema
   * @param procname
   *
   * @return a DataStore with the argumens of the procedure
   *
   * @throws java.sql.SQLException
   */
  private DataStore getColumns(String catalog, String schema, PGProcName procname)
    throws SQLException
  {
    String sql =
      "SELECT format_type(p.prorettype, NULL) as formatted_type, \n" +
      "       t.typname as pg_type, \n" +
      "       coalesce(array_to_string(proallargtypes, ';'), array_to_string(proargtypes, ';')) as argtypes, \n" +
      "       array_to_string(p.proargnames, ';') as argnames, \n" +
      "       array_to_string(p.proargmodes, ';') as modes, \n" +
      "       t.typtype \n" +
      "FROM pg_catalog.pg_proc p \n" +
      "   JOIN pg_catalog.pg_namespace n ON p.pronamespace = n.oid \n" +
      "   JOIN pg_catalog.pg_type t ON p.prorettype = t.oid \n" +
      "WHERE n.nspname = ? \n" +
      "  AND p.proname = ? \n";

    DataStore result = createProcColsDataStore();

    Savepoint sp = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;

    String oids = procname.getInputOIDs();

    if (StringUtil.isNonBlank(oids))
    {
      sql += "  AND p.proargtypes = cast('" + oids + "' as oidvector)";
    }

    LogMgr.logMetadataSql(new CallerInfo(){}, "procedure columns", sql, schema, procname.getName());

    try
    {
      sp = connection.setSavepoint();

      stmt = this.connection.getSqlConnection().prepareStatement(sql);
      stmt.setString(1, schema);
      stmt.setString(2, procname.getName());

      rs = stmt.executeQuery();
      if (rs.next())
      {
        String typeName = rs.getString("formatted_type");
        String pgType = rs.getString("pg_type");
        String types = rs.getString("argtypes");
        String names = rs.getString("argnames");
        String modes = rs.getString("modes");
        String returnTypeType = rs.getString("typtype");

        // pgAdmin II distinguishes functions from procedures using only the "modes" information
        // the driver uses the returnTypeType as well
        boolean isFunction = (returnTypeType.equals("b") || returnTypeType.equals("d") || (returnTypeType.equals("p") && modes == null));

        if (isFunction)
        {
          int row = result.addRow();
          result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME, "returnValue");
          result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE, "RETURN");
          result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_JDBC_DATA_TYPE, getJavaType(pgType));
          result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE, StringUtil.trimQuotes(typeName));
        }

        List<String> argNames = StringUtil.stringToList(names, ";", true, true);
        List<String> argTypes = StringUtil.stringToList(types, ";", true, true);
        if (modes == null)
        {
          modes = types.replaceAll("[0-9]+", "i");
        }
        List<String> argModes = StringUtil.stringToList(modes, ";", true, true);

        List<ColumnIdentifier> columns = convertToColumns(argNames, argTypes, argModes);

        for (ColumnIdentifier col : columns)
        {
          int row = result.addRow();
          result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE, col.getArgumentMode());
          result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_JDBC_DATA_TYPE, col.getDataType());
          result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE, col.getDbmsType());
          result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME, col.getColumnName());
        }
      }
      else
      {
        LogMgr.logWarning(new CallerInfo(){}, "No columns returned for procedure: " + procname.getName(), null);
        return super.getProcedureColumns(catalog, schema, procname.getName(), null);
      }

      connection.releaseSavepoint(sp);
    }
    catch (Exception e)
    {
      connection.rollback(sp);
      LogMgr.logMetadataError(new CallerInfo(){}, e, "procedure columns", sql, schema, procname.getName());
      return super.getProcedureColumns(catalog, schema, procname.getName(), null);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return result;
  }

  private List<ColumnIdentifier> convertToColumns(List<String> argNames, List<String> argTypes, List<String> argModes)
  {
    List<ColumnIdentifier> result = new ArrayList<>(argTypes.size());
    for (int i = 0; i < argTypes.size(); i++)
    {
      int typeOid = StringUtil.getIntValue(argTypes.get(i), -1);
      String pgt = getTypeNameFromOid(typeOid);

      String nm = "$" + (i + 1);
      if (argNames != null && i < argNames.size())
      {
        nm = argNames.get(i);
      }

      String md = null;
      if (argModes != null && i < argModes.size())
      {
        md = pgArgModeToJdbc(argModes.get(i));
      }

      ColumnIdentifier col = new ColumnIdentifier(nm);
      col.setDataType(getJavaType(pgt));
      col.setDbmsType(getTypeNameFromOid(typeOid));
      col.setArgumentMode(md);
      result.add(col);
    }
    return result;
  }

  static String pgArgModeToJdbc(String pgMode)
  {
    if (pgMode == null) return null;

    switch (pgMode)
    {
      case "i":
        return "IN";
      case "o":
        return "OUT";
      case "b":
        return "INOUT";
      case "v":
        // treat VARIADIC as input parameter
        return "IN";
      case "t":
        return "RETURN";
      default:
        break;
    }
    return null;
  }
}
