package com.patetlex.jcefnative;

import com.patetlex.jcefnative.util.CefHelper;
import org.cef.CefApp;
import org.cef.SystemBootstrap;

public class JCefNative {
    public static void setup() {
        CefHelper.extractNativeAndResource();
        SystemBootstrap.setLoader(new SystemBootstrap.Loader() {
            @Override
            public void loadLibrary(String s) {
                try {
                    System.load(System.getProperty("java.io.tmpdir") + "jcefwin64\\" + (s.contains(".") ? s : s + ".dll"));
                } catch (UnsatisfiedLinkError e) {
                    System.loadLibrary(s);
                }
            }
        });
    }

    public static void dispose() {
        CefApp.getInstance().dispose();
    }
}
