package workbench.util;

import java.util.Locale;

public enum Os {
    Windows, Mac, Linux, NotSupported;

    public static Os getCurrentOs() {
        final String os = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if ((os.indexOf("mac") >= 0) || (os.indexOf("darwin") >= 0)) {
            return Os.Mac;
        } else if (os.indexOf("win") >= 0) {
            return Os.Windows;
        } else if (os.indexOf("nux") >= 0) {
            return Os.Linux;
        } else {
            return Os.NotSupported;
        }
    }
}
