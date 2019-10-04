/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.greenplum;

import java.util.List;

import workbench.db.DbMetadata;
import workbench.db.ObjectListEnhancer;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.postgres.PostgresTypeReader;

import workbench.storage.DataStore;

import static workbench.db.greenplum.GreenplumExternalTableReader.*;

/**
 *
 * @author Thomas Kellerer
 */
public class GreenplumObjectListEnhancer
  implements ObjectListEnhancer
{
  private final GreenplumExternalTableReader tableReader = new GreenplumExternalTableReader();

  @Override
  public void updateObjectList(WbConnection con, DataStore result, String catalogPattern, String schemaPattern, String objectNamePattern, String[] types)
  {
    if (con == null) return;
    if (!DbMetadata.typeIncluded("TABLE", types)) return;

    List<TableIdentifier> externalTables = tableReader.getExternalTables(con, schemaPattern, objectNamePattern);
    for (int row = 0; row < result.getRowCount(); row++)
    {
      String tName = result.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
      String tSchema = result.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
      TableIdentifier extTable = TableIdentifier.findTableByNameAndSchema(externalTables, new TableIdentifier(tSchema, tName));
      if (extTable != null)
      {
        result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, EXT_TABLE_TYPE);
      }
    }
  }

}
