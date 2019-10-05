package workbench.gui;

import javax.swing.*;
import workbench.util.Os;

public class WbUIManager extends UIManager {
    public static void setLookAndFeel(LookAndFeel newLookAndFeel) throws UnsupportedLookAndFeelException {
        if(Os.getCurrentOs() == Os.Mac){
            // show menus on mac top bar, this may override by Look-N-Feel
            System.setProperty("com.apple.macos.useScreenMenuBar", "true" );
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }
        UIManager.setLookAndFeel(newLookAndFeel);
    }
}
