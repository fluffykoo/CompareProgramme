package com.mmd.txt;

import java.util.*;

public class TxtComparator {
    public static Map<String, String> listToMap(List<String[]> rows, List<Integer> keyIndexes, List<Integer> ignoredIndexes) {
        Map<String, String> map = new HashMap<>();

        for (String[] row : rows) {
            StringBuilder keyBuilder = new StringBuilder();
            StringBuilder valueBuilder = new StringBuilder();

            for (int i = 0; i < row.length; i++) {
                if (keyIndexes.contains(i)) {
                    keyBuilder.append(row[i]).append("|");
                } else if (!ignoredIndexes.contains(i)) {
                    valueBuilder.append(row[i]).append("|");
                }
            }

            String key = keyBuilder.toString();
            String value = valueBuilder.toString();
            map.put(key, value);
        }

        return map;
    }
}
