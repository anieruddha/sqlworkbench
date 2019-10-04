/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.postgres;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresUtilTest
{

  /**
   * Test of switchDatabaseURL method, of class PostgresUtil.
   */
  @Test
  public void testSwitchDatabaseURL()
  {
    assertEquals("jdbc:postgresql://localhost/postgres", PostgresUtil.switchDatabaseURL("jdbc:postgresql://localhost/test", "postgres"));

    assertEquals("jdbc:postgresql://localhost/postgres?autosave=true",
                  PostgresUtil.switchDatabaseURL("jdbc:postgresql://localhost/test?autosave=true", "postgres"));

    assertEquals("jdbc:postgresql://localhost:5544/postgres?autosave=true&prepareThreshold=42",
                  PostgresUtil.switchDatabaseURL("jdbc:postgresql://localhost:5544/test?autosave=true&prepareThreshold=42", "postgres"));
  }
}
