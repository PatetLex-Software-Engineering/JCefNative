package com.patetlex.jcefnative.util;

public class DataHelper {
    public static String createHtmlURL(String contents) {
        return "data:text/html;base64," + java.util.Base64.getEncoder().encodeToString(contents.getBytes());
    }
}
