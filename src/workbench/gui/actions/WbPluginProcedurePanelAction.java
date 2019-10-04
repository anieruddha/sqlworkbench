package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import workbench.db.ProcedureDefinition;
import workbench.db.WbConnection;
import workbench.interfaces.WbProcedurePanelAction;
import workbench.resource.Settings;
import workbench.util.WbThread;

/**
 * {@link WbAction} to execute {@link WbProcedurePanelAction} commands
 * @author Andreas Krist
 */
public class WbPluginProcedurePanelAction
   extends WbAction implements ListSelectionListener
{
   private final WbProcedurePanelAction ppAction;
   private final String label;
   private final Supplier<ProcedureDefinition> procedureSelectionSupplier;
   private WbConnection connection;

   /**
    * Constructor
    * @param ppAction The {@link WbProcedurePanelAction} to execute
    * @param connection The currently used workbench connection
    * @param language The configured language
    * @param procedureSelectionSupplier
    */
   public WbPluginProcedurePanelAction(final WbProcedurePanelAction ppAction, WbConnection connection, Supplier<ProcedureDefinition> procedureSelectionSupplier)
   {
      this.ppAction = ppAction;
      this.connection = connection;
      this.procedureSelectionSupplier = procedureSelectionSupplier;
      
      this.label = ppAction.getLabel(Settings.getInstance().getLanguage());
      
      this.setEnabled(false);
      this.setMenuText(label);
   }

   @Override
   public void executeAction(ActionEvent event)
   {
      new WbThread(() ->  {
         ProcedureDefinition procedureDefinition = this.procedureSelectionSupplier.get();
         procedureDefinition.getParameters(this.connection);
         
         ppAction.execute(event, this.connection, procedureDefinition);
      }, "Executing " + this.label).start();
   }
   
   @Override
   public void valueChanged(ListSelectionEvent e)
   {
      ListSelectionModel lsm = (ListSelectionModel) e.getSource();
      this.setEnabled(!lsm.isSelectionEmpty() && IntStream.range(e.getFirstIndex(), e.getLastIndex() + 1)
                                                          .filter(lsm::isSelectedIndex)
                                                          .count() == 1);
   }
}