package com.mmd.json;

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
        
        // Regroupement par ID d'entité - comme dans le code original
        Map<String, List<Difference>> byEntityId = groupByEntityId(differences);
        
        report.append("=== Rapport des differences ===\n\n");
        
        for (String entityId : new TreeSet<>(byEntityId.keySet())) {
            List<Difference> entityDiffs = byEntityId.get(entityId);
            report.append("[Entite (titre, instrument, etc) ").append(entityId).append("]\n");
            
            boolean headerShown = false;
            for (Difference diff : entityDiffs) {
                if (!headerShown) {
                    if (diff.getType() == ChangeType.ADDITION) report.append(" [Ajout]\n");
                    else if (diff.getType() == ChangeType.DELETION) report.append(" [Suppression]\n");
                    else report.append(" [Modifications]\n");
                    headerShown = true;
                }
                
                if (diff.getType() == ChangeType.MODIFICATION) {
                    report.append(" * Section : ").append(diff.getSection());
                    report.append(" | ").append(diff.getKey()).append("\n");
                    report.append(" * Valeur fichier de référence : ").append(diff.getOldValue()).append("\n");
                    report.append(" * Valeur nouveau fichier : ").append(diff.getNewValue()).append("\n");
                }
            }
            report.append("\n");
        }
        
        // Sauvegarde du fichier
        String fileName = "rapportJSON_" + timestamp + ".txt";
        Path filePath = Paths.get(outputFolder, fileName);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, report.toString().getBytes());
        
        System.out.println("Fichier .txt genere : " + fileName);
    }
    
    public void generateExcelReport(List<Difference> differences) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Differences");
        
        // Création de l'en-tête
        Row header = sheet.createRow(0);
        String[] columns = {"ID", "Type", "Section", "KEY", "OLD VALUE", "NEW VALUE"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
        }
        
        // Création des styles
        CellStyle ajoutStyle = workbook.createCellStyle();
        ajoutStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        ajoutStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        CellStyle suppressionStyle = workbook.createCellStyle();
        suppressionStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        suppressionStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        CellStyle modificationStyle = workbook.createCellStyle();
        modificationStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        modificationStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        // Remplissage des données
        int rowNum = 1;
        for (Difference diff : differences) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(diff.getEntityId());
            row.createCell(1).setCellValue(diff.getType().name());
            row.createCell(2).setCellValue(diff.getSection());
            row.createCell(3).setCellValue(diff.getKey());
            row.createCell(4).setCellValue(diff.getOldValue() != null ? diff.getOldValue() : "");
            row.createCell(5).setCellValue(diff.getNewValue() != null ? diff.getNewValue() : "");
            
            // Application du style selon le type de différence
            CellStyle style;
            switch (diff.getType()) {
                case ADDITION:
                    style = ajoutStyle;
                    break;
                case DELETION:
                    style = suppressionStyle;
                    break;
                case MODIFICATION:
                    style = modificationStyle;
                    break;
                default:
                    style = null;
            }
            
            // Application du style à toute la ligne
            for (int i = 0; i < 6; i++) {
                row.getCell(i).setCellStyle(style);
            }
        }
        
        // Sauvegarde du fichier
        String fileName = "rapportJSON_" + timestamp + ".xlsx";
        Path filePath = Paths.get(outputFolder, fileName);
        Files.createDirectories(filePath.getParent());
        
        try (OutputStream os = Files.newOutputStream(filePath)) {
            workbook.write(os);
        }
        
        workbook.close();
        System.out.println("Fichier .xlsx genere : " + fileName);
    }
    
    private Map<String, List<Difference>> groupByEntityId(List<Difference> differences) {
        Map<String, List<Difference>> groups = new LinkedHashMap<>();
        
        for (Difference diff : differences) {
            if (!groups.containsKey(diff.getEntityId())) {
                groups.put(diff.getEntityId(), new ArrayList<>());
            }
            groups.get(diff.getEntityId()).add(diff);
        }
        
        return groups;
    }
}
