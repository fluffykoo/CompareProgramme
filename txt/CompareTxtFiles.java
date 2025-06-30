package com.mmd.txt;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class CompareTxtFiles {
    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.out.println("Usage : java CompareTxtFiles <fichier1> <fichier2> <configPath> <dossierRapport> [terminal]");
            return;
        }

        String fichier1 = args[0];
        String fichier2 = args[1];
        String configPath = args[2];
        String dossierRapport = args[3];
        boolean afficherTerminal = args.length >= 5 && args[4].equalsIgnoreCase("terminal");

        // Lire la config JSON
        TxtSimpleConfigReader config = new TxtSimpleConfigReader(configPath);
        List<Integer> indexCols = config.getIndexCols(); //composite keys
        Set<Integer> colonnesIgnorees = config.getColonnesIgnorees();
        String separator = config.getSeparator();

        // Comparaison
        TxtComparator comparator = new TxtComparator(afficherTerminal, separator);
        comparator.runComparison(fichier1, fichier2, indexCols, colonnesIgnorees); 

        // Génération des rapports
        TxtReportGenerator generator = new TxtReportGenerator(afficherTerminal);
        generator.generateReports(dossierRapport, comparator.getRapportTexte(), comparator.getXlsxData());

        int ajout = (int) comparator.getXlsxData().stream().filter(l -> "AJOUT".equals(l[0])).count();
        int suppression = (int) comparator.getXlsxData().stream().filter(l -> "DELETION".equals(l[0])).count();
        int modification = (int) comparator.getXlsxData().stream().filter(l -> "MODIFICATION".equals(l[0])).map(l -> l[1]).distinct().count();
        int identique = comparator.getTotalIdentique();

        if (!afficherTerminal) {
            System.out.println();
            System.out.println("=== Summary ===");
            System.out.println("Reference file       : " + fichier1);
            System.out.println("New file             : " + fichier2);
            System.out.println("* Text (.txt) report generated   : " + generator.getTxtFilePath());
            System.out.println("* Excel (.xlsx) report generated : " + generator.getXlsxFilePath());
            System.out.println("* CSV (.csv) report generated   : " + generator.getCsvFilePath());
            System.out.println("* Identical rows     : " + identique);
            System.out.println("* Modified rows      : " + modification);
            System.out.println("* Added rows         : " + ajout);
            System.out.println("* Deleted rows       : " + suppression);
        }
    }
}
