package workbench.gui;


import javax.swing.*;
import java.util.Locale;

public class WbUIManager extends UIManager {
    public static void setLookAndFeel(LookAndFeel newLookAndFeel) throws UnsupportedLookAndFeelException {
        if(getOS() == OS.Mac){
            try {
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                WbUIManager.setLookAndFeel(WbUIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException
                             | InstantiationException
                             | IllegalAccessException
                             | UnsupportedLookAndFeelException e) {
                System.err.println("not able to set menu for mac " + e.getMessage());
            }
        }
        UIManager.setLookAndFeel(newLookAndFeel);
    }

    private enum OS {
        Windows, Mac, Linux, NotSupported
    }

    private static OS getOS() {
        String os = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if ((os.indexOf("mac") >= 0) || (os.indexOf("darwin") >= 0)) {
            return OS.Mac;
        } else if (os.indexOf("win") >= 0) {
            return OS.Windows;
        } else if (os.indexOf("nux") >= 0) {
            return OS.Linux;
        } else {
            return OS.NotSupported;
        }
    }
}
