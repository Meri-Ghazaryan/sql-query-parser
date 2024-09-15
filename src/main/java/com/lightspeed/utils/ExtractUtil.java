package com.lightspeed.utils;

import java.util.regex.Pattern;

public final class ExtractUtil {

    public static String extractBetween(String query, String startKeyword, String endKeyword) {
        var pattern = Pattern.compile(startKeyword + "\\s+(.*?)\\s*(?=" + endKeyword + ")", Pattern.CASE_INSENSITIVE);
        var matcher = pattern.matcher(query);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static String extractAfter(String query, String keyword) {
        var pattern = Pattern.compile(keyword + "\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
        var matcher = pattern.matcher(query);
        return matcher.find() ? matcher.group(1) : null;
    }

    private ExtractUtil() {
        // empty
    }
}
