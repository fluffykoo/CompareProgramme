package com.mmd.txt;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class TxtComparator {

    public static List<Difference> compareByKeyColumns(
            List<String[]> file1,
            List<String[]> file2,
            List<Integer> keyColumns,
            Set<Integer> ignoredColumns
    ) {
        List<Difference> differences = new ArrayList<>();

        Map<String, String[]> mapFile1 = indexByKey(file1, keyColumns);
        Map<String, String[]> mapFile2 = indexByKey(file2, keyColumns);

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(mapFile1.keySet());
        allKeys.addAll(mapFile2.keySet());

        for (String key : allKeys) {
            String[] row1 = mapFile1.get(key);
            String[] row2 = mapFile2.get(key);

            if (row1 != null && row2 != null) {
                if (!Arrays.equals(filterIgnored(row1, ignoredColumns), filterIgnored(row2, ignoredColumns))) {
                    differences.add(new Difference("MODIFIED", key, row1, row2));
                } else {
                    differences.add(new Difference("IDENTICAL", key, row1, row2));
                }
            } else if (row1 == null) {
                differences.add(new Difference("ADDED", key, null, row2));
            } else {
                differences.add(new Difference("DELETED", key, row1, null));
            }
        }
        return differences;
    }

    private static Map<String, String[]> indexByKey(List<String[]> rows, List<Integer> keyColumns) {
        Map<String, String[]> map = new HashMap<>();
        for (String[] row : rows) {
            if (row.length <= Collections.max(keyColumns)) continue;
            String key = keyColumns.stream().map(i -> row[i]).collect(Collectors.joining("|"));
            map.put(key, row);
        }
        return map;
    }

    private static String[] filterIgnored(String[] row, Set<Integer> ignoredColumns) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < row.length; i++) {
            if (!ignoredColumns.contains(i)) {
                result.add(row[i]);
            }
        }
        return result.toArray(new String[0]);
    }

    public static List<String[]> readFile(String path, String separator) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(path));
        List<String[]> data = new ArrayList<>();
        for (String line : lines) {
            data.add(line.split(Pattern.quote(separator)));
        }
        return data;
    }

    public static String detectSeparator(String line) {
        String[] possibleSeparators = {"|", ",", ";", "\t"};
        int maxParts = 0;
        String bestSeparator = "|";
        for (String sep : possibleSeparators) {
            int parts = line.split(Pattern.quote(sep)).length;
            if (parts > maxParts) {
                maxParts = parts;
                bestSeparator = sep;
            }
        }
        return bestSeparator;
    }

    public static class Difference {
        public String type;
        public String key;
        public String[] row1;
        public String[] row2;

        public Difference(String type, String key, String[] row1, String[] row2) {
            this.type = type;
            this.key = key;
            this.row1 = row1;
            this.row2 = row2;
        }
    }
}
