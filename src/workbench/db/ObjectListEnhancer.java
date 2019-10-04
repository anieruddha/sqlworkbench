/*
 * ObjectListEnhancer.java
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

import workbench.storage.DataStore;

/**
 *
 * @author Thomas Kellerer
 */
public interface ObjectListEnhancer
{
  /**
   * Updates the objects in the passed result DataStore with additional information (e.g. table remarks)
   *
   * @param con the database connection to be used
   * @param result the DataStore containing the objects already returned by the driver
   * @param catalogPattern the catalog pattern
   * @param schemaPattern  the schema pattern
   * @param objectNamePattern the object name pattern
   * @param requestedTypes the object types as passed to DbMetadata.getObjects()
   *
   */
  void updateObjectList(WbConnection con, DataStore result, String catalogPattern, String schemaPattern, String objectNamePattern, String[] requestedTypes);

}
