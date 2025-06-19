package com.mmd.txt;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class TxtComparator {

    public static List<String> readFile(String path, String separator) throws IOException {
        return Files.readAllLines(Paths.get(path));
    }

    public static String detectSeparator(String path) throws IOException {
        String line = Files.readAllLines(Paths.get(path)).get(0);
        if (line.contains("|")) return "\\|";
        if (line.contains(";")) return ";";
        if (line.contains(",")) return ",";
        return "\\s+"; // fallback
    }

    public TxtComparator() {}

    public void comparer(List<String> lignes1, List<String> lignes2,
                         List<Integer> keyIndexes, Set<Integer> ignoredIndexes,
                         List<String[]> ajout, List<String[]> suppression,
                         List<String[]> modification, List<String[]> identique) {

        Map<String, String[]> map1 = toMap(lignes1, keyIndexes, ignoredIndexes);
        Map<String, String[]> map2 = toMap(lignes2, keyIndexes, ignoredIndexes);

        Set<String> allKeys = new HashSet<>(map1.keySet());
        allKeys.addAll(map2.keySet());

        for (String key : allKeys) {
            String[] l1 = map1.get(key);
            String[] l2 = map2.get(key);

            if (l1 == null) {
                ajout.add(l2);
            } else if (l2 == null) {
                suppression.add(l1);
            } else if (!Arrays.equals(l1, l2)) {
                modification.add(l2);
            } else {
                identique.add(l1);
            }
        }
    }

    private Map<String, String[]> toMap(List<String> lignes, List<Integer> keyIndexes, Set<Integer> ignoredIndexes) {
        Map<String, String[]> map = new HashMap<>();
        for (String ligne : lignes) {
            String[] parts = ligne.split("\\|", -1);
            StringBuilder key = new StringBuilder();
            for (int index : keyIndexes) {
                if (index < parts.length) key.append(parts[index]).append("|");
            }

            List<String> filtered = new ArrayList<>();
            for (int i = 0; i < parts.length; i++) {
                if (!ignoredIndexes.contains(i)) {
                    filtered.add(parts[i]);
                }
            }

            map.put(key.toString(), filtered.toArray(new String[0]));
        }
        return map;
    }
}
