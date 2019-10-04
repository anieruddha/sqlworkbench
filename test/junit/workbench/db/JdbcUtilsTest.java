/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2016 Thomas Kellerer.
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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class JdbcUtilsTest
{

  public JdbcUtilsTest()
  {
  }

  @Test
  public void testExtractPrefix()
  {
    assertEquals("jdbc:postgresql:", JdbcUtils.extractPrefix("jdbc:postgresql://localhost/postgres"));
    assertEquals("jdbc:jtds:", JdbcUtils.extractPrefix("jdbc:jtds:sqlserver://localhost/foobar"));
    assertEquals("jdbc:oracle:", JdbcUtils.extractPrefix("jdbc:oracle:thin:@//localhost:1521/oradb"));
  }

  @Test
  public void testExtractDBMSName()
  {
    assertEquals("postgresql", JdbcUtils.getDBMSName("jdbc:postgresql://localhost/postgres"));
    assertEquals("jtds", JdbcUtils.getDBMSName("jdbc:jtds:sqlserver://localhost/foobar"));
    assertEquals("oracle", JdbcUtils.getDBMSName("jdbc:oracle:thin:@//localhost:1521/oradb"));
  }

  @Test
  public void testExtractDBID()
  {
    assertEquals(DBID.Postgres.getId(), JdbcUtils.getDbIdFromUrl("jdbc:postgresql://localhost/postgres"));
    assertEquals(DBID.HANA.getId(), JdbcUtils.getDbIdFromUrl("jdbc:sap://centos01:30015/"));
    assertEquals(DBID.SAP_DB.getId(), JdbcUtils.getDbIdFromUrl("jdbc:sapdb://127.0.0.1/dummy"));
  }
}
