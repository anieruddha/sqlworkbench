/*
 * DbShutdownFactory.java
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
package workbench.db.shutdown;

import workbench.db.WbConnection;

/**
 * A factory to create instances of the DbShutdownHook interface.
 *
 * @author Thomas Kellerer
 */
public class DbShutdownFactory
{
  /**
   * Create a DbShutdownHook for the given connection.
   * @param con the connection for which to create the shutdown hook
   * @return null if not shutdown processing is necessary, an approriate instance otherwise
   */
  public static DbShutdownHook getShutdownHook(WbConnection con)
  {
    if (con == null) return null;
    if (con.getMetadata() == null) return null;

    if (con.getMetadata().isHsql())
    {
      return new HsqlShutdownHook();
    }
    else if (con.getMetadata().isApacheDerby())
    {
      return new DerbyShutdownHook();
    }
    return null;
  }
}
