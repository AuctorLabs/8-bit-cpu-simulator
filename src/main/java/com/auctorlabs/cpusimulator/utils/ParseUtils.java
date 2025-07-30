package com.auctorlabs.cpusimulator.utils;

public class ParseUtils {
    public static int parseSafeInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
