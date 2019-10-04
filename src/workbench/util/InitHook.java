package workbench.util;

/**
 * Callback for plugins to do work during initalization of SQL Workbench.
 *
 * This class can be overriden from extensions.
 *
 * @author Franz.Mayer
 */
public interface InitHook {


	/**
	 * Called when SQL Workbench scans for extensions during startup.
	 */
	void init();
}
