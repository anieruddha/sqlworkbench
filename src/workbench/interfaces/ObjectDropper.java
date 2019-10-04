/*
 * ObjectDropper.java
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
package workbench.interfaces;

import java.sql.SQLException;
import java.util.List;

import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.RowActionMonitor;

/**
 *
 * @author Thomas Kellerer
 */
public interface ObjectDropper
{
	boolean supportsCascade();
	boolean supportsFKSorting();
	void setCascade(boolean flag);
	void setConnection(WbConnection con);
	WbConnection getConnection();
	void setObjectTable(TableIdentifier tbl);

	void setObjects(List<? extends DbObject> objects);
	List<? extends DbObject> getObjects();

	void dropObjects()
		throws SQLException;

	void cancel()
		throws SQLException;

	void setRowActionMonitor(RowActionMonitor monitor);

	CharSequence getScript();
	CharSequence getDropForObject(DbObject toDrop, boolean cascade);
	CharSequence getDropForObject(DbObject toDrop);
  boolean supportsObject(DbObject object);
}
