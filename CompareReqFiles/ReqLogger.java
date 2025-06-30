package com.mmd.req;

import java.util.*;

public class ReqLogger {
    private static final StringBuilder reportText = new StringBuilder();

    public static void log(String line) {
        System.out.println(line);
        reportText.append(line).append("\n");
    }

    public static StringBuilder getReportText() {
        return reportText;
    }
}