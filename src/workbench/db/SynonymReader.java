/*
 * SynonymReader.java
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

import java.sql.SQLException;
import java.util.List;

import workbench.resource.Settings;


/**
 * Read the definition of synonyms from the database.
 *
 * @author Thomas Kellerer
 */
public interface SynonymReader
{
  String SYN_TYPE_NAME = "SYNONYM";

  TableIdentifier getSynonymTable(WbConnection con, String catalog, String schema, String aSynonym)
      throws SQLException;

  List<TableIdentifier> getSynonymList(WbConnection con, String catalogPattern, String schemaPattern, String namePattern)
    throws SQLException;

  default String getSynonymSource(WbConnection con, String catalog, String schema, String synonym)
      throws SQLException
  {
    TableIdentifier targetTable = getSynonymTable(con, catalog, schema, synonym);
    StringBuilder result = new StringBuilder(200);
    String nl = Settings.getInstance().getInternalEditorLineEnding();
    result.append("CREATE SYNONYM ");
    TableIdentifier syn = new TableIdentifier(catalog, schema, synonym);
    result.append(syn.getTableExpression(con));
    result.append(nl + "   FOR ");
    result.append(targetTable.getTableExpression(con));
    result.append(';');
    result.append(nl);
    return result.toString();
  }

  default String getSynonymTypeName()
  {
    return SYN_TYPE_NAME;
  }

  default boolean supportsReplace(WbConnection con)
  {
    return false;
  }
}
