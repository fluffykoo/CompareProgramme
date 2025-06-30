package com.mmd.req;

public class ReqLogger {
    private static StringBuilder logContent = new StringBuilder();

    public static void log(String line) {
        System.out.println(line);
        logContent.append(line).append("\n");
    }

    public static String getLog() {
        return logContent.toString();
    }
}
