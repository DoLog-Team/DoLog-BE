package com.dolog.server.global.util;

public class OrderUtils {

    private static final int GAP = 100;

    public static int next(Integer max) {
        return (max != null) ? max + GAP : GAP;
    }

    public static int between(Integer prev, Integer next) {
        if (prev == null && next != null) return next - GAP;
        if (prev != null && next == null) return prev + GAP;
        if (prev != null && next != null) return (prev + next) / 2;
        throw new IllegalArgumentException("Invalid order request");
    }

    public static boolean needRebalance(Integer prev, Integer next) {
        return prev != null && next != null && Math.abs(prev - next) <= 1;
    }
}
