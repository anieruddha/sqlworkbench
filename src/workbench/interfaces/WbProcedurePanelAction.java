package workbench.interfaces;

import java.awt.event.ActionEvent;
import java.util.Locale;

import javax.swing.SwingUtilities;

import workbench.db.ProcedureDefinition;
import workbench.db.WbConnection;

/**
 * Interface for Procedure Panel Actions
 * @author Andreas Krist
 */
public interface WbProcedurePanelAction
{
   /**
    * Implementations must return the name/label of the procedure action
    * @param language
    * @return
    */
   String getLabel(Locale language);
   
   /**
    * Executes the plug-in command. 
    * <br> <b>Note</b>: The code is executed in a thread, to access the UI thread use {@link SwingUtilities#invokeLater(Runnable)}
    * @param event The event for the action
    * @param wbConnection The workbench database connection
    * @param procedureDefinition The definition of the selected procedure
    */
   void execute(ActionEvent event, WbConnection wbConnection, ProcedureDefinition procedureDefinition);
}
