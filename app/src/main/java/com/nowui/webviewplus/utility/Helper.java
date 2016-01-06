package com.nowui.webviewplus.utility;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;


public class Helper {

    public static final String WebUrl = "http://61.191.16.150:8081/System-HFCDC-Web/";
    //public static final String WebUrl = "http://192.168.200.5:8080/";

    //Key
    public static final String KeyUrl = "url";
    public static final String KeyText = "text";
    public static final String KeyParameter = "parameter";
    public static final String KeyIsOpen = "isOpen";
    public static final String KeyMimeType = "mimeType";
    public static final String KeyName = "name";
    public static final String KeyIsLocal = "isLocal";
    public static final String KeyPath = "path";

    //Event
    public static final String EventPull = "Pull";
    public static final String EventTitle = "Title";
    public static final String EventBack = "Back";
    public static final String EventBackCallback = "BackCallback";
    public static final String EventNormal = "Normal";
    public static final String EventNormal2 = "Normal2";
    public static final String EventNormalCallback = "NormalCallback";
    public static final String EventNormalCallback2 = "NormalCallback2";
    public static final String EventDownload = "Download";

    //Code
    public static final int CodeRequest = 0;
    public static final int CodeResult = 10;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    public static boolean isNullOrEmpty(Object obj) {
        if (obj == null)
            return true;

        if (obj instanceof CharSequence)
            return ((CharSequence) obj).length() == 0;

        if (obj instanceof Collection)
            return ((Collection<?>) obj).isEmpty();

        if (obj instanceof Map)
            return ((Map<?, ?>) obj).isEmpty();

        if (obj instanceof Object[]) {
            Object[] object = (Object[]) obj;
            if (object.length == 0) {
                return true;
            }
            boolean empty = true;
            for (int i = 0; i < object.length; i++) {
                if (!isNullOrEmpty(object[i])) {
                    empty = false;
                    break;
                }
            }
            return empty;
        }
        return false;
    }

    public static String formatDateTime() {
        return dateFormat.format(new Date());
    }

}
