package com.mmd.req;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CompareReqFiles {
    public static void main(String[] args) throws IOException {
        if (args.length < 2 || args.length > 3) {
            ReqLogger.log("Usage: java CompareReqFiles <file1.req> <file2.req> [report_folder]");
            return;
        }

        String file1 = args[0];
        String file2 = args[1];
        String reportFolder = (args.length == 3) ? args[2] : ".";

        if (!new File(file1).exists() || !new File(file2).exists()) {
            ReqLogger.log("Error: One of the req files does not exist.");
            return;
        }

        List<String> lines1 = Files.readAllLines(Paths.get(file1));
        List<String> lines2 = Files.readAllLines(Paths.get(file2));

        Map<String, List<String>> sections1 = ReqSectionParser.extractSections(lines1);
        Map<String, List<String>> sections2 = ReqSectionParser.extractSections(lines2);

        int totalAdd = 0, totalDel = 0;

        ReqLogger.log("=== .req File Comparison ===");
        ReqLogger.log("Reference file : " + file1);
        ReqLogger.log("Compared file  : " + file2);
        ReqLogger.log("");

        List<String[]> csvData = new ArrayList<>();

        for (String section : sections1.keySet()) {
            List<String> part1 = ReqUtils.clean(sections1.get(section));
            List<String> part2 = ReqUtils.clean(sections2.get(section));

            Set<String> set1 = new HashSet<>(part1);
            Set<String> set2 = new HashSet<>(part2);

            ReqLogger.log("\n-- Section: " + section + " --");

            Set<String> removed = new LinkedHashSet<>(set1);
            removed.removeAll(set2);

            Set<String> added = new LinkedHashSet<>(set2);
            added.removeAll(set1);

            if (removed.isEmpty() && added.isEmpty()) {
                ReqLogger.log(" → No difference.");
            } else {
                for (String line : removed) {
                    ReqLogger.log("[Deleted]  " + line);
                    csvData.add(new String[]{"DELETION", section, line});
                    totalDel++;
                }
                for (String line : added) {
                    ReqLogger.log("[Added]    " + line);
                    csvData.add(new String[]{"ADDITION", section, line});
                    totalAdd++;
                }
            }
        }

        ReqLogger.log("\n=== Summary ===");
        ReqLogger.log("Lines added    : " + totalAdd);
        ReqLogger.log("Lines deleted  : " + totalDel);
        ReqLogger.log("Total changes  : " + (totalAdd + totalDel));

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        ReqReportGenerator generator = new ReqReportGenerator(reportFolder, timestamp);
        generator.generateTextReport(ReqLogger.getReportText());
        generator.generateCsvReport(csvData);
        generator.generateExcelReport(csvData);
    }
}