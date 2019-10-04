/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2019 Thomas Kellerer.
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
package workbench.db.mariadb;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.QuoteHandler;
import workbench.db.SequenceDefinition;

import workbench.db.SequenceReader;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

import static workbench.db.SequenceReader.*;

/**
 *
 * @author Thomas Kellerer
 */
public class MariaDBSequenceReader
  implements SequenceReader
{
  private WbConnection dbConnection;

  public MariaDBSequenceReader(WbConnection conn)
  {
    this.dbConnection = conn;
  }

  @Override
  public List<SequenceDefinition> getSequences(String catalog, String owner, String namePattern)
  {
    // nothing to do as the driver already returns sequences with getTables()
    return new ArrayList<>();
  }

  @Override
  public CharSequence getSequenceSource(String catalog, String schema, String sequence)
  {
    SequenceDefinition seq = getSequenceDefinition(catalog, schema, sequence);
    return seq.getSource();
  }

  @Override
  public void readSequenceSource(SequenceDefinition def)
  {
    def.setSource(buildSource(def));
  }

  @Override
  public SequenceDefinition getSequenceDefinition(String catalog, String schema, String sequence)
  {
    DataStore ds = getRawSequenceDefinition(catalog, schema, sequence);
    if (ds == null || ds.getRowCount() == 0) return null;

    SequenceDefinition seq = createSequenceDefinition(catalog, schema, sequence, ds, 0);
    seq.setSource(buildSource(seq));
    return seq;
  }

  @Override
  public DataStore getRawSequenceDefinition(String catalog, String schema, String sequence)
  {
    String prefix = StringUtil.coalesce(StringUtil.trimToNull(catalog), StringUtil.trimToNull(schema));
    QuoteHandler quoter = dbConnection.getMetadata();
    String sql =
      "select * \n" +
      "from " + quoter.quoteObjectname(prefix)+ "." + quoter.quoteObjectname(sequence);

    LogMgr.logMetadataSql(new CallerInfo(){}, "SEQUENCE DEFINITION", sql);
    ResultSet rs = null;
    Statement stmt = null;
    DataStore result = null;
    try
    {
      stmt = dbConnection.createStatementForQuery();
      rs = stmt.executeQuery(sql);
      result = new DataStore(rs, dbConnection, true);
    }
    catch (SQLException ex)
    {
      LogMgr.logMetadataError(new CallerInfo(){}, ex, "SEQUENCE DEFINITION", sql);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return result;
  }

  @Override
  public String getSequenceTypeName()
  {
    return DEFAULT_TYPE_NAME;
  }

  private SequenceDefinition createSequenceDefinition(String catalog, String schema, String sequence, DataStore ds, int row)
  {
    SequenceDefinition result = null;

    if (ds == null || ds.getRowCount() == 0) return null;

    result = new SequenceDefinition(catalog, schema, sequence);

    result.setSequenceProperty(PROP_START_VALUE, ds.getValue(row, "start_value"));
    result.setSequenceProperty(PROP_MAX_VALUE, ds.getValue(row, "maximum_value"));
    result.setSequenceProperty(PROP_MIN_VALUE, ds.getValue(row, "minimum_value"));
    result.setSequenceProperty(PROP_INCREMENT, ds.getValue(row, "increment"));
    result.setSequenceProperty(PROP_CYCLE, ds.getValue(row, "cycle_option"));
    result.setSequenceProperty(PROP_CACHE_SIZE, ds.getValue(row, "cache_size"));
    result.setSource(buildSource(result));
    return result;
  }

  protected CharSequence buildSource(SequenceDefinition def)
  {
    if (def == null) return StringUtil.EMPTY_STRING;

    StringBuilder result = new StringBuilder(100);
    result.append("CREATE SEQUENCE ");
    String nl = Settings.getInstance().getInternalEditorLineEnding();
    result.append(def.getSequenceName());

    Number start = (Number)def.getSequenceProperty(PROP_START_VALUE);
    if (start != null && start.longValue() > 1)
    {
      result.append(nl);
      result.append("       START ");
      result.append(start);
    }

    Number inc = (Number)def.getSequenceProperty(PROP_INCREMENT);
    if (inc != null && inc.longValue() > 1)
    {
      result.append(nl);
      result.append("       INCREMENT BY ");
      result.append(inc);
    }

    Number min = (Number)def.getSequenceProperty(PROP_MIN_VALUE);
    if (min != null && min.longValue() != 1 && min.longValue() != -9223372036854775807l)
    {
      result.append(nl);
      result.append("       MINVALUE ");
      result.append(min);
    }
    Number max = (Number)def.getSequenceProperty(PROP_MAX_VALUE);
    if (max != null && max.longValue() != -1 && max.longValue() != 9223372036854775806l)
    {
      result.append(nl);
      result.append("       MAXVALUE ");
      result.append(max);
    }

    Number cache = (Number)def.getSequenceProperty(PROP_CACHE_SIZE);
    if (cache != null && cache.longValue() != 1000)
    {
      result.append(nl);
      result.append("       CACHE ");
      result.append(cache);
    }

    Boolean cycle = (Boolean)def.getSequenceProperty(PROP_CYCLE);
    if (cycle != null && cycle.booleanValue())
    {
      result.append(nl);
      result.append("       CYCLE");
    }

    result.append(';');
    result.append(nl);


    return result;
  }

}
