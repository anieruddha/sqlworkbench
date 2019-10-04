package workbench.util;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import workbench.db.DBID;

import workbench.interfaces.WbPluginProcedureActionProvider;
import workbench.interfaces.WbPluginProvider;
import workbench.interfaces.WbProcedurePanelAction;
import workbench.log.CallerInfo;
import workbench.log.LogMgr;
import workbench.resource.Settings;

/**
 * Helper class for Wb Plugins
 * @author Andreas Krist
 */
public class WbPluginUtil
{
   private static List<WbPluginProvider> PLUGIN_PROVIDERS;

   static
   {
      ServiceLoader<WbPluginProvider> plugins = ServiceLoader.load(WbPluginProvider.class);
      WbPluginUtil.PLUGIN_PROVIDERS = StreamSupport.stream(plugins.spliterator(), false).collect(toList());
   }

   private WbPluginUtil()
   {
      // nothing to do
   }
   private static <T> Stream<T> filterByProviderInterface(Class<T> clazz)
   {
      return WbPluginUtil.PLUGIN_PROVIDERS.stream()
                             .filter(clazz::isInstance)
                             .map(clazz::cast);
   }
   /**
    * Retrieves the list of plugin provided {@link WbProcedurePanelAction} items.
    * @return {@link List} of {@link WbProcedurePanelAction} objects.
    */
   public static List<WbProcedurePanelAction> getPluginProcedurePanelActions(DBID dbId)
   {
      final Locale language = Settings.getInstance().getLanguage();

      return WbPluginUtil.filterByProviderInterface(WbPluginProcedureActionProvider.class)
         .map(p -> p.getProcedurePanelActions(dbId))
         .filter(Objects::nonNull)
         .flatMap(List::stream)
         .filter(proc -> {
            boolean keep = true;

            String label = null;
            if (proc == null)
            {
              LogMgr.logWarning(new CallerInfo(){}, "Null is not a valid action");
              keep = false;
            }
            else
            {
              label = proc.getLabel(language);
            }

            if (StringUtil.isBlank(label))
            {
              LogMgr.logWarning(new CallerInfo(){}, "No Label for action " + proc + " defined");
              keep = false;
            }

            return keep;
         })
         .collect(toList());
   }



}
