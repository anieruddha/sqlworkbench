package workbench.interfaces;

import java.util.List;

import workbench.db.DBID;

/**
 * @author Andreas Krist
 */
public interface WbPluginProcedureActionProvider
{
  List<WbProcedurePanelAction> getProcedurePanelActions(DBID dbid);
}
