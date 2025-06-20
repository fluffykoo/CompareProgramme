package com.mmd.txt;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TxtReportGenerator {
    private final boolean afficherDansTerminal;
    private String txtFilePath;
    private String xlsxFilePath;

    public TxtReportGenerator(boolean afficherDansTerminal) {
        this.afficherDansTerminal = afficherDansTerminal;
    }

    private void afficher(String ligne) {
        if (afficherDansTerminal) System.out.println(ligne);
    }

    public void generateReports(String dossierRapport, StringBuilder rapportTexte, List<String[]> xlsxData) throws IOException {
        String horodatage = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Files.createDirectories(Paths.get(dossierRapport));

        String txtPath = Paths.get(dossierRapport, "rapportTXT_" + horodatage + ".txt").toString();
        txtFilePath = txtPath;
        Files.write(Paths.get(txtPath), rapportTexte.toString().getBytes());

        afficher("* Text (.txt) report saved to : " + txtPath);

        if (afficherDansTerminal) {
            afficher("\n=== Full Text Report ===");
            for (String ligne : rapportTexte.toString().split("\n")) {
                System.out.println(ligne);
            }
            afficher("");
            afficher("* Text (.txt) report generated : " + txtPath);
        } else {
            long totalAjout = xlsxData.stream().filter(l -> "AJOUT".equals(l[0])).count();
            long totalSupp = xlsxData.stream().filter(l -> "DELETION".equals(l[0])).count();
            long totalModif = xlsxData.stream().filter(l -> "MODIFICATION".equals(l[0])).map(l -> l[1]).distinct().count();
            long totalIdentique = rapportTexte.toString().lines().filter(l -> l.startsWith("[UNCHANGED]")).count();

            afficher("");
            afficher("=== Summary ===");
            afficher("* Identical rows     : " + totalIdentique);
            afficher("* Modified rows      : " + totalModif);
            afficher("* Added rows         : " + totalAjout);
            afficher("* Deleted rows       : " + totalSupp);
        }

        exportToXlsx(dossierRapport, horodatage, xlsxData);
    }

    private void exportToXlsx(String dossierRapport, String horodatage, List<String[]> xlsxData) throws IOException {
        String nomFichier = "rapportTXT_" + horodatage + ".xlsx";
        Path cheminFichier = Paths.get(dossierRapport, nomFichier);
        xlsxFilePath = cheminFichier.toString();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Differences");

        String[] titres = {"Type", "Key", "Changed field", "Old value", "New value"};
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row header = sheet.createRow(0);
        for (int i = 0; i < titres.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(titres[i]);
            cell.setCellStyle(headerStyle);
        }

        CellStyle ajoutStyle = workbook.createCellStyle();
        ajoutStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        ajoutStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle suppStyle = workbook.createCellStyle();
        suppStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        suppStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle modifStyle = workbook.createCellStyle();
        modifStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        modifStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        int rowNum = 1;
        for (String[] ligne : xlsxData) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < ligne.length; i++) {
                row.createCell(i).setCellValue(ligne[i] != null ? ligne[i] : "");
            }

            Cell typeCell = row.getCell(0);
            switch (typeCell.getStringCellValue().toUpperCase()) {
                case "AJOUT":
                case "ADD":
                    typeCell.setCellStyle(ajoutStyle);
                    break;
                case "SUPPRESSION":
                case "DELETION":
                    typeCell.setCellStyle(suppStyle);
                    break;
                case "MODIFICATION":
                    typeCell.setCellStyle(modifStyle);
                    break;
            }
        }

        try (FileOutputStream fileOut = new FileOutputStream(cheminFichier.toFile())) {
            workbook.write(fileOut);
        }
        workbook.close();

        afficher("* .xlsx report generated in " + dossierRapport + " : " + nomFichier);
        afficher("* Excel (.xlsx) report generated : " + cheminFichier);
    }

    public String getTxtFilePath() {
        return txtFilePath;
    }

    public String getXlsxFilePath() {
        return xlsxFilePath;
    }
}