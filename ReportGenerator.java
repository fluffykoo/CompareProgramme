package com.mmd.json;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ReportGenerator {
    private String outFolder, timestamp;

    public ReportGenerator(String outFolder, String timestamp) {
        this.outFolder = outFolder;
        this.timestamp = timestamp;
    }

    public void generateTextReport(List<Difference> diffs)
            throws IOException {
        StringBuilder sb = new StringBuilder("=== JSON Comparison ===\n\n");
        Map<String, List<Difference>> byId = new TreeMap<>();
        for (Difference d : diffs) {
            byId.computeIfAbsent(d.getEntityId(), k -> new ArrayList<>()).add(d);
        }
        for (Map.Entry<String, List<Difference>> e : byId.entrySet()) {
            sb.append("[Object ").append(e.getKey()).append("]\n");
            for (Difference d : e.getValue()) {
                sb.append(String.format(" %s %s: %s -> %s%n",
                    d.getType(), d.getKey(),
                    d.getOldValue(), d.getNewValue()));
            }
            sb.append("\n");
        }
        Files.write(Paths.get(outFolder,
            "report_" + timestamp + ".txt"),
            sb.toString().getBytes());
    }

    public void generateExcelReport(List<Difference> diffs)
            throws IOException {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Differences");
        String[] cols = {"ID", "Type", "Section", "Key", "Old", "New"};
        Row h = sheet.createRow(0);
        for (int i=0; i<cols.length; i++) h.createCell(i).setCellValue(cols[i]);

        Map<ChangeType, CellStyle> styles = new HashMap<>();
        CellStyle addS = wb.createCellStyle();
        addS.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        addS.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put(ChangeType.ADDITION, addS);

        CellStyle delS = wb.createCellStyle();
        delS.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        delS.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put(ChangeType.DELETION, delS);

        CellStyle modS = wb.createCellStyle();
        modS.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        modS.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put(ChangeType.MODIFICATION, modS);

        int row = 1;
        for (Difference d : diffs) {
            Row r = sheet.createRow(row++);
            r.createCell(0).setCellValue(d.getEntityId());
            r.createCell(1).setCellValue(d.getType().name());
            r.createCell(2).setCellValue(d.getSection());
            r.createCell(3).setCellValue(d.getKey());
            r.createCell(4).setCellValue(
                d.getOldValue() != null ? d.getOldValue() : "");
            r.createCell(5).setCellValue(
                d.getNewValue() != null ? d.getNewValue() : "");
            CellStyle s = styles.get(d.getType());
            for (int i=0; i<6; i++) r.getCell(i).setCellStyle(s);
        }
        try (OutputStream os = Files.newOutputStream(
                Paths.get(outFolder, "report_" + timestamp + ".xlsx"))) {
            wb.write(os);
        }
        wb.close();
    }
}
