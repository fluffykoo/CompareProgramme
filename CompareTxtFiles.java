package com.mmd.txt;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CompareTxtFiles {
    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.out.println("Usage: java CompareTxtFiles <fichier1> <fichier2> <config.json> <dossierRapport>");
            return;
        }

        String fichier1 = args[0];
        String fichier2 = args[1];
        String configPath = args[2];
        String dossierRapport = args[3];

        TxtConfigManager config = new TxtConfigManager(configPath);
        List<Integer> keyIndexes = config.getKeyIndexes();
        List<Integer> ignoredIndexes = config.getIgnoredIndexes();

        List<String[]> refLines = readCsv(fichier1);
        List<String[]> newLines = readCsv(fichier2);

        Map<String, String> refMap = TxtComparator.listToMap(refLines, keyIndexes, ignoredIndexes);
        Map<String, String> newMap = TxtComparator.listToMap(newLines, keyIndexes, ignoredIndexes);

        String horodatage = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        TxtReportGenerator generator = new TxtReportGenerator(dossierRapport, horodatage);
        generator.genererRapportTexte(refMap, newMap);
    }

    private static List<String[]> readCsv(String filePath) throws IOException {
        List<String[]> rows = new ArrayList<>();
        List<String> lines = Files.readAllLines(Paths.get(filePath));

        String sep = detectSeparator(lines.get(0));
        for (String line : lines) {
            rows.add(line.split(Pattern.quote(sep), -1));
        }
        return rows;
    }

    private static String detectSeparator(String header) {
        if (header.contains("|")) return "|";
        if (header.contains(";")) return ";";
        if (header.contains(",")) return ",";
        return "\\s+"; // fallback
    }
}
