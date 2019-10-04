/*
 * ResultColumnMetaDataTest.java
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
package workbench.storage;

import java.sql.ResultSet;
import java.sql.Statement;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ResultColumnMetaDataTest
  extends WbTestCase
{

  private WbConnection connection;

  public ResultColumnMetaDataTest()
  {
    super("ResultColumnMetaDataTest");
  }

  @Before
  public void setup()
    throws Exception
  {
    TestUtil util = getTestUtil();
    connection = util.getConnection();
  }

  @After
  public void tearDown()
  {
    connection.disconnect();
  }

  @Test
  public void testRetrieveComments()
    throws Exception
  {
    TestUtil.executeScript(connection,
      "create table person (id integer primary key, first_name varchar(50), last_name varchar(50));\n" +
      "comment on column person.id is 'Primary key';\n" +
      "comment on column person.first_name is 'The first name';" +
      "comment on column person.last_name is 'The last name';\n" +
      "\n" +
      "create table address (id integer primary key, person_id integer not null, address_info varchar(500));\n" +
      "comment on column address.id is 'Address PK';\n" +
      "comment on column address.person_id is 'The person ID';\n" +
      "comment on column address.address_info is 'The address';\n" +
      "commit;\n");

    validateQuery(
      "select id, first_name, last_name from person",
      "Primary key", "The first name", "The last name");

    validateQuery(
      "select p.id as person_id, p.first_name as fname, p.last_name as lname \n" +
      "from person p",
      "Primary key", "The first name", "The last name");

    validateQuery(
      "select p.id as person_id, a.person_id as address_pid, p.first_name, p.last_name, a.address_info \n" +
      "from person p \n" +
      "  join address a on p.id = a.person_id",
      "Primary key", "The person ID", "The first name", "The last name", "The address");

    validateQuery(
      "select a.address_info, p.id as person_id, a.person_id as address_pid, p.first_name, p.last_name \n" +
      "from person p \n" +
      "  join address a on p.id = a.person_id",
      "The address", "Primary key", "The person ID", "The first name", "The last name");

    validateQuery(
      "select a.id as address_id, p.id as person_id, p.first_name, p.last_name \n" +
      "from person p \n" +
      "  join address a on p.id = a.person_id",
      "Address PK", "Primary key", "The first name", "The last name");

    validateQuery(
      "select * from person",
      "Primary key", "The first name", "The last name");

    validateQuery(
      "select * from person as p",
      "Primary key", "The first name", "The last name");

    validateQuery(
      "select id, first_name from person p limit 1",
      "Primary key", "The first name");

    validateQuery(
      "select p.*, a.* \n" +
      "from person p \n" +
      "  join address a on p.id = a.person_id",
      "Primary key", "The first name", "The last name", "Address PK", "The person ID", "The address");

    validateQuery(
      "select * " +
      "  from person p join address a on p.id = a.person_id",
      "Primary key", "The first name", "The last name", "Address PK", "The person ID", "The address");
  }

  private void validateQuery(String sql, String... expectedComments)
    throws Exception
  {
    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(sql);)
    {
      ResultInfo info = new ResultInfo(rs.getMetaData(), connection);
      ResultColumnMetaData meta = new ResultColumnMetaData(sql, connection);
      meta.retrieveColumnRemarks(info);
      assertEquals(expectedComments.length, info.getColumnCount());
      for (int i=0; i < expectedComments.length; i++)
      {
        assertEquals(expectedComments[i], info.getColumn(i).getComment());
      }
    }
  }

}
