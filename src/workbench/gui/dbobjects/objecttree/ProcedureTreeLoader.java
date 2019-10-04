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
package workbench.gui.dbobjects.objecttree;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import workbench.resource.ResourceMgr;

import workbench.db.BaseObjectType;
import workbench.db.PackageDefinition;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.ReaderFactory;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class ProcedureTreeLoader
{

  public void loadProcedures(ObjectTreeNode procListNode, DbObjectTreeModel model, WbConnection connection, TreeLoader loader)
    throws SQLException
  {
    if (procListNode == null) return;
    ProcedureReader procReader = ReaderFactory.getProcedureReader(connection.getMetadata());
    ObjectTreeNode schemaNode = procListNode.getParent();
    String schemaName = schemaNode.getName();
    String catalog = null;
    ObjectTreeNode parent = schemaNode.getParent();
    if (parent != null && parent.isCatalogNode())
    {
      catalog = parent.getName();
    }
    List<ProcedureDefinition> procedures = procReader.getProcedureList(catalog, schemaName, null);

    Map<String, List<ProcedureDefinition>> procs = getPackageProcedures(procedures);
    for (Map.Entry<String, List<ProcedureDefinition>> entry : procs.entrySet())
    {
      PackageDefinition pkg = new PackageDefinition(schemaName, entry.getKey());
      ObjectTreeNode pkgNode = new ObjectTreeNode(pkg);
      pkgNode.setNodeType(TreeLoader.TYPE_PACKAGE_NODE);
      pkgNode.setAllowsChildren(true);
      for (ProcedureDefinition proc : entry.getValue())
      {
        ObjectTreeNode node = new ObjectTreeNode(proc);
        addParameterNode(node);
        node.setChildrenLoaded(false);
        pkgNode.add(node);
      }
      pkgNode.setChildrenLoaded(true);
      loader.addDependencyNodes(pkgNode);
      procListNode.add(pkgNode);
    }

    Map<String, List<ProcedureDefinition>> types = getTypeMethods(procedures);
    for (Map.Entry<String, List<ProcedureDefinition>> entry : types.entrySet())
    {
      BaseObjectType typeDbo = new BaseObjectType(schemaName, entry.getKey());
      ObjectTreeNode pkgNode = new ObjectTreeNode(typeDbo);
      pkgNode.setAllowsChildren(true);
      for (ProcedureDefinition proc : entry.getValue())
      {
        ObjectTreeNode procNode = new ObjectTreeNode(proc);
        addParameterNode(procNode);
        procNode.setChildrenLoaded(true);
        pkgNode.add(procNode);
      }
      pkgNode.setChildrenLoaded(true);
      loader.addDependencyNodes(pkgNode);
      procListNode.add(pkgNode);
    }

    for (ProcedureDefinition proc : procedures)
    {
      if (!proc.isPackageProcedure() && !proc.isOracleObjectType())
      {
        ObjectTreeNode node = new ObjectTreeNode(proc);
        addParameterNode(node);
        loader.addDependencyNodes(node);
        node.setChildrenLoaded(true);
        procListNode.add(node);
      }
    }
    procListNode.setChildrenLoaded(true);
    model.nodeStructureChanged(procListNode);
  }

  private void addParameterNode(ObjectTreeNode procNode)
  {
    ObjectTreeNode node = new ObjectTreeNode(ResourceMgr.getString("TxtDbExpProcParams"), TreeLoader.TYPE_PARAMETER_LIST);
    procNode.setAllowsChildren(true);
    procNode.add(node);
  }

  private Map<String, List<ProcedureDefinition>> getTypeMethods(List<ProcedureDefinition> allProcs)
  {
     Map<String, List<ProcedureDefinition>> packages = new TreeMap<>();
     for (ProcedureDefinition def : allProcs)
     {
       if (def.isOracleObjectType())
       {
         List<ProcedureDefinition> procList = packages.get(def.getPackageName());
         if (procList == null)
         {
           procList = new ArrayList<>();
           packages.put(def.getPackageName(), procList);
         }
         procList.add(def);
       }
     }
     return packages;
  }

  private Map<String, List<ProcedureDefinition>> getPackageProcedures(List<ProcedureDefinition> allProcs)
  {
     Map<String, List<ProcedureDefinition>> packages = new TreeMap<>();
     for (ProcedureDefinition def : allProcs)
     {
       if (def.isPackageProcedure())
       {
         List<ProcedureDefinition> procList = packages.get(def.getPackageName());
         if (procList == null)
         {
           procList = new ArrayList<>();
           packages.put(def.getPackageName(), procList);
         }
         procList.add(def);
       }
     }
     return packages;
  }
}
