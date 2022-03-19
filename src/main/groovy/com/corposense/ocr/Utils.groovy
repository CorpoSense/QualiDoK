package com.corposense.ocr;

class Utils {
    static String getOsName(){
        //by using ROOT we don't need to specify the language/country.
        return System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);
    }
    static boolean isLinux(){
        return Utils.getOsName().contains("nux");
    }
    static boolean isWindows(){
        return Utils.getOsName().contains("win");
    }
}
