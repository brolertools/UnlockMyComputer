package com.kingtous.remotefingerunlock.Common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTool {
    public static String ipRegex = "((25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))";
    public static String macRegex = "[0-9A-F]{2}:[0-9A-F]{2}:[0-9A-F]{2}:[0-9A-F]{2}:[0-9A-F]{2}:[0-9A-F]{2}";



    public static boolean isStdIp(String ip){
        if (ip==null) return false;
        Pattern pattern=Pattern.compile(ipRegex);
        Matcher matcher=pattern.matcher(ip);
        return matcher.matches();
    }

    public static boolean isStdMac(String mac){
        if (mac==null) return false;
        Pattern pattern=Pattern.compile(macRegex);
        Matcher matcher=pattern.matcher(mac);
        return matcher.matches();
    }


}
