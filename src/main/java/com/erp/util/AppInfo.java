package com.erp.util;

import java.io.InputStream;
import java.util.Properties;

public class AppInfo {

    private static final String VERSAO;
    private static final String BUILD;

    static {
        Properties p = new Properties();
        try (InputStream in = AppInfo.class.getClassLoader().getResourceAsStream("app.properties")) {
            if (in != null) p.load(in);
        } catch (Exception ignored) {}
        VERSAO = p.getProperty("app.version", "1.0.0");
        BUILD  = p.getProperty("app.build", "");
    }

    public static String getVersao() { return VERSAO; }
    public static String getBuild()  { return BUILD; }
    public static String getVersaoCompleta() { return "v" + VERSAO + (BUILD.isEmpty() ? "" : " (" + BUILD + ")"); }
}
