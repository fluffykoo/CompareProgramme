package com.mmd;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ReportGenerator {
    private String outputFolder;
    private String timestamp;
    
    public ReportGenerator(String outputFolder, String timestamp) {
        this.outputFolder = outputFolder;
        this.timestamp = timestamp;
    }
    
    public void generateTextReport(List<Difference> differences) throws IOException {
        StringBuilder report = new StringBuilder();
        
        // Grouper par ID d'entité
        Map<String, List<Difference>> byEntityId = groupByEntityId(differences);
        
        report.append("=== JSON Comparison Report ===\n\n");
        
        for (String entityId : new TreeSet<>(byEntityId.keySet())) {
            List<Difference> entityDiffs = byEntityId.get(entityId);
            report.append("[Entity ").append(entityId).append("]\n");
            
            for (Difference diff : entityDiffs) {
                switch (diff.getType()) {
                    case ADDITION:
                        report.append("  [ADDED] ");
                        break;
                    case DELETION:
                        report.append("  [DELETED] ");
                        break;
                    case MODIFICATION:
                        report.append("  [MODIFIED] ");
                        report.append("Section: ").append(diff.getSection());
                        report.append(" | Key: ").append(diff.getKey()).append("\n");
                        report.append("    Old: ").append(diff.getOldValue()).append("\n");
                        report.append("    New: ").append(diff.getNewValue()).append("\n");
                        break;
                }
            }
            report.append("\n");
        }
        
        // Sauvegarder le fichier
        String fileName = "json_comparison_report_" + timestamp + ".txt";
        Path filePath = Paths.get(outputFolder, fileName);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, report.toString().getBytes());
        
        System.out.println("Text report generated: " + fileName);
    }
    
    public void generateExcelReport(List<Difference> differences) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Differences");
        
        // Créer l'en-tête
        createHeader(sheet);
        
        // Créer les styles
        Map<ChangeType, CellStyle> styles = createStyles(workbook);
        
        // Remplir les données
        int rowNumber = 1;
        for (Difference diff : differences) {
            Row row = sheet.createRow(rowNumber++);
            fillRow(row, diff, styles.get(diff.getType()));
        }
        
        // Ajuster les colonnes
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
        
        // Sauvegarder le fichier
        String fileName = "json_comparison_report_" + timestamp + ".xlsx";
        Path filePath = Paths.get(outputFolder, fileName);
        Files.createDirectories(filePath.getParent());
        
        try (OutputStream os = Files.newOutputStream(filePath)) {
            workbook.write(os);
        }
        
        workbook.close();
        System.out.println("Excel report generated: " + fileName);
    }
    
    private Map<String, List<Difference>> groupByEntityId(List<Difference> differences) {
        Map<String, List<Difference>> groups = new LinkedHashMap<>();
        
        for (Difference diff : differences) {
            groups.computeIfAbsent(diff.getEntityId(), k -> new ArrayList<>()).add(diff);
        }
        
        return groups;
    }
    
    private void createHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        String[] columns = {"Entity ID", "Change Type", "Section", "Key", "Old Value", "New Value"};
        
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
        }
    }
    
    private Map<ChangeType, CellStyle> createStyles(Workbook workbook) {
        Map<ChangeType, CellStyle> styles = new HashMap<>();
        
        CellStyle additionStyle = workbook.createCellStyle();
        additionStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        additionStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put(ChangeType.ADDITION, additionStyle);
        
        CellStyle deletionStyle = workbook.createCellStyle();
        deletionStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        deletionStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put(ChangeType.DELETION, deletionStyle);
        
        CellStyle modificationStyle = workbook.createCellStyle();
        modificationStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        modificationStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put(ChangeType.MODIFICATION, modificationStyle);
        
        return styles;
    }
    
    private void fillRow(Row row, Difference diff, CellStyle style) {
        row.createCell(0).setCellValue(diff.getEntityId());
        row.createCell(1).setCellValue(diff.getType().name());
        row.createCell(2).setCellValue(diff.getSection());
        row.createCell(3).setCellValue(diff.getKey());
        row.createCell(4).setCellValue(diff.getOldValue() != null ? diff.getOldValue() : "");
        row.createCell(5).setCellValue(diff.getNewValue() != null ? diff.getNewValue() : "");
        
        // Appliquer le style à toute la ligne
        for (int i = 0; i < 6; i++) {
            row.getCell(i).setCellStyle(style);
        }
    }
}
