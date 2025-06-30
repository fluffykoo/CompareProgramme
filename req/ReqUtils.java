package com.mmd.req;

import java.util.*;

public class ReqUtils {
    public static List<String> clean(List<String> lines) {
        List<String> cleaned = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                cleaned.add(trimmed);
            }
        }
        return cleaned;
    }
}
