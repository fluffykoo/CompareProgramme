package com.mmd.req;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ReqReportGenerator {
    private final String folder;
    private final String timestamp;

    public ReqReportGenerator(String folder, String timestamp) {
        this.folder = folder;
        this.timestamp = timestamp;
    }

    public void generateTextReport(StringBuilder content) throws IOException {
        Files.createDirectories(Paths.get(folder));
        String fileName = "req_report_" + timestamp + ".txt";
        Path path = Paths.get(folder, fileName);
        Files.write(path, content.toString().getBytes());
        System.out.println("Text (.txt) report generated: " + path);
    }

    public void generateCsvReport(List<String[]> data) throws IOException {
        String fileName = "req_report_" + timestamp + ".csv";
        Path path = Paths.get(folder, fileName);

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write("Type,Section,Content\n");
            for (String[] line : data) {
                writer.write(String.join(",", escapeCsv(line)));
                writer.newLine();
            }
        }

        System.out.println("CSV (.csv) report generated: " + path);
    }

    public void generateExcelReport(List<String[]> data) throws IOException {
        String fileName = "req_report_" + timestamp + ".xlsx";
        Path path = Paths.get(folder, fileName);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Differences");

        String[] headers = {"Type", "Section", "Content"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        CellStyle styleAdd = workbook.createCellStyle();
        styleAdd.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        styleAdd.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle styleDel = workbook.createCellStyle();
        styleDel.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        styleDel.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        for (int i = 0; i < data.size(); i++) {
            Row row = sheet.createRow(i + 1);
            String[] line = data.get(i);
            for (int j = 0; j < line.length; j++) {
                row.createCell(j).setCellValue(line[j]);
            }

            Cell typeCell = row.getCell(0);
            if ("ADDITION".equalsIgnoreCase(typeCell.getStringCellValue())) {
                typeCell.setCellStyle(styleAdd);
            } else if ("DELETION".equalsIgnoreCase(typeCell.getStringCellValue())) {
                typeCell.setCellStyle(styleDel);
            }
        }

        try (FileOutputStream out = new FileOutputStream(path.toFile())) {
            workbook.write(out);
        }
        workbook.close();
        System.out.println("Excel (.xlsx) report generated: " + path);
    }

    private String[] escapeCsv(String[] fields) {
        String[] escaped = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i] != null ? fields[i] : "";
            if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
                field = field.replace("\"", "\"\"");
                field = "\"" + field + "\"";
            }
            escaped[i] = field;
        }
        return escaped;
    }
}