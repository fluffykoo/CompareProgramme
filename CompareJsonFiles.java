package com.mmd.json;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CompareJsonFiles {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println("Usage: java CompareJsonFiles <referenceFile> <newFile> <outputFolder> <configFile>");
            return;
        }
        String referenceFile = args[0];
        String newFile = args[1];
        String outputFolder = args[2];
        String configFile = args[3];

        JsonComparator comparator = new JsonComparator(configFile);
        List<Difference> differences = comparator.compare(referenceFile, newFile);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        ReportGenerator generator = new ReportGenerator(outputFolder, timestamp);

        generator.generateTextReport(differences);
        generator.generateExcelReport(differences);

        displaySummary(differences);
    }

    private static void displaySummary(List<Difference> differences) {
        long additions = differences.stream().filter(d -> d.getType() == ChangeType.ADDITION).count();
        long deletions = differences.stream().filter(d -> d.getType() == ChangeType.DELETION).count();
        long modifications = differences.stream().filter(d -> d.getType() == ChangeType.MODIFICATION).count();

        System.out.println("\n=== Summary ===");
        System.out.println("Additions: " + additions);
        System.out.println("Deletions: " + deletions);
        System.out.println("Modifications: " + modifications);
    }
}
