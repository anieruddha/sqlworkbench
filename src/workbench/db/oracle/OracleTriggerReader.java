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
package workbench.db.oracle;

import java.sql.SQLException;

import workbench.db.DefaultTriggerReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleTriggerReader
  extends DefaultTriggerReader
{

  public OracleTriggerReader(WbConnection conn)
  {
    super(conn);
  }

  @Override
  public String getTriggerSource(String triggerCatalog, String triggerSchema, String triggerName, TableIdentifier triggerTable, String trgComment, boolean includeDependencies)
    throws SQLException
  {
    if (OracleUtils.getUseOracleDBMSMeta(OracleUtils.DbmsMetadataTypes.trigger))
    {
      try
      {
        return DbmsMetadata.getDDL(dbConnection, "TRIGGER", triggerName, triggerSchema);
      }
      catch (SQLException ex)
      {
        // logging was already done
        // by catching the exception we let the ancestor do the job.
      }
    }
    return super.getTriggerSource(triggerCatalog, triggerSchema, triggerName, triggerTable, trgComment, includeDependencies);
  }

}
