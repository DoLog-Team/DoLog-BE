package com.dolog.server.global.util;

public class TextUtils {

    private TextUtils() {}

    public static String normalizeNewlines(String text) {
        if (text == null) return null;
        return text.replace("\\n", "\n");
    }
}
