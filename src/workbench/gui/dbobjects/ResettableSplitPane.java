/*
 * ResettableSplitPane.java
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
package workbench.gui.dbobjects;

import java.util.HashSet;
import java.util.Set;

import workbench.interfaces.Resettable;

import workbench.gui.components.WbSplitPane;

/**
 * @author Thomas Kellerer
 */
public class ResettableSplitPane
	extends WbSplitPane
	implements Resettable
{
	private Set<Resettable> clients = new HashSet<>();

	public ResettableSplitPane(int type)
	{
		super(type);
	}

	public void addClient(Resettable r)
	{
		clients.add(r);
	}

  @Override
	public void reset()
	{
		for (Resettable r : clients)
		{
			r.reset();
		}
	}

}
