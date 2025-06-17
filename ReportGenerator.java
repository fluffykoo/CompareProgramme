package com.mmd;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ReportGenerator {
    private String dossierSortie;
    private String horodatage;
    
    public ReportGenerator(String dossierSortie, String horodatage) {
        this.dossierSortie = dossierSortie;
        this.horodatage = horodatage;
    }
    
    public void genererRapportTexte(List<Difference> differences) throws IOException {
        StringBuilder rapport = new StringBuilder();
        
        // Grouper par ID
        Map<String, List<Difference>> parId = grouperParId(differences);
        
        rapport.append("=== Rapport des Différences ===\n\n");
        
        for (String id : new TreeSet<>(parId.keySet())) {
            List<Difference> diffsId = parId.get(id);
            rapport.append("[Entité ").append(id).append("]\n");
            
            for (Difference diff : diffsId) {
                switch (diff.getType()) {
                    case AJOUT:
                        rapport.append("  [AJOUT] ");
                        break;
                    case SUPPRESSION:
                        rapport.append("  [SUPPRESSION] ");
                        break;
                    case MODIFICATION:
                        rapport.append("  [MODIFICATION] ");
                        rapport.append("Section: ").append(diff.getSection());
                        rapport.append(" | Clé: ").append(diff.getCle()).append("\n");
                        rapport.append("    Ancien: ").append(diff.getValeurAncienne()).append("\n");
                        rapport.append("    Nouveau: ").append(diff.getValeurNouvelle()).append("\n");
                        break;
                }
            }
            rapport.append("\n");
        }
        
        // Sauvegarder le fichier
        String nomFichier = "rapport_" + horodatage + ".txt";
        Path chemin = Paths.get(dossierSortie, nomFichier);
        Files.createDirectories(chemin.getParent());
        Files.write(chemin, rapport.toString().getBytes());
        
        System.out.println("Fichier texte généré: " + nomFichier);
    }
    
    public void genererRapportExcel(List<Difference> differences) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Différences");
        
        // Créer l'en-tête
        creerEnTete(sheet);
        
        // Créer les styles
        Map<TypeChangement, CellStyle> styles = creerStyles(workbook);
        
        // Remplir les données
        int numeroLigne = 1;
        for (Difference diff : differences) {
            Row ligne = sheet.createRow(numeroLigne++);
            remplirLigne(ligne, diff, styles.get(diff.getType()));
        }
        
        // Ajuster les colonnes
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
        
        // Sauvegarder le fichier
        String nomFichier = "rapport_" + horodatage + ".xlsx";
        Path chemin = Paths.get(dossierSortie, nomFichier);
        Files.createDirectories(chemin.getParent());
        
        try (OutputStream os = Files.newOutputStream(chemin)) {
            workbook.write(os);
        }
        
        workbook.close();
        System.out.println("Fichier Excel généré: " + nomFichier);
    }
    
    private Map<String, List<Difference>> grouperParId(List<Difference> differences) {
        Map<String, List<Difference>> groupes = new LinkedHashMap<>();
        
        for (Difference diff : differences) {
            groupes.computeIfAbsent(diff.getId(), k -> new ArrayList<>()).add(diff);
        }
        
        return groupes;
    }
    
    private void creerEnTete(Sheet sheet) {
        Row entete = sheet.createRow(0);
        String[] colonnes = {"ID", "Type", "Section", "Clé", "Ancienne Valeur", "Nouvelle Valeur"};
        
        for (int i = 0; i < colonnes.length; i++) {
            Cell cellule = entete.createCell(i);
            cellule.setCellValue(colonnes[i]);
        }
    }
    
    private Map<TypeChangement, CellStyle> creerStyles(Workbook workbook) {
        Map<TypeChangement, CellStyle> styles = new HashMap<>();
        
        CellStyle styleAjout = workbook.createCellStyle();
        styleAjout.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        styleAjout.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put(TypeChangement.AJOUT, styleAjout);
        
        CellStyle styleSuppression = workbook.createCellStyle();
        styleSuppression.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        styleSuppression.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put(TypeChangement.SUPPRESSION, styleSuppression);
        
        CellStyle styleModification = workbook.createCellStyle();
        styleModification.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        styleModification.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put(TypeChangement.MODIFICATION, styleModification);
        
        return styles;
    }
    
    private void remplirLigne(Row ligne, Difference diff, CellStyle style) {
        ligne.createCell(0).setCellValue(diff.getId());
        ligne.createCell(1).setCellValue(diff.getType().name());
        ligne.createCell(2).setCellValue(diff.getSection());
        ligne.createCell(3).setCellValue(diff.getCle());
        ligne.createCell(4).setCellValue(diff.getValeurAncienne() != null ? diff.getValeurAncienne() : "");
        ligne.createCell(5).setCellValue(diff.getValeurNouvelle() != null ? diff.getValeurNouvelle() : "");
        
        // Appliquer le style à toute la ligne
        for (int i = 0; i < 6; i++) {
            ligne.getCell(i).setCellStyle(style);
        }
    }
}
