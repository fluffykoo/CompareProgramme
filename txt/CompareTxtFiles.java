package com.mmd.txt;

import java.io.IOException;

public class CompareTxtFiles {
    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.out.println("Usage : java CompareTxtFiles <fichier1> <fichier2> <indexCol> <dossierRapport> [terminal]");
            return;
        }

        String fichier1 = args[0];
        String fichier2 = args[1];
        int indexCol = Integer.parseInt(args[2]) - 1;
        String dossierRapport = args[3];
        boolean afficherTerminal = args.length >= 5 && args[4].equalsIgnoreCase("terminal");

        TxtComparator comparator = new TxtComparator(afficherTerminal);
        comparator.runComparison(fichier1, fichier2, indexCol);

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
            System.out.println("* Identical rows     : " + identique);
            System.out.println("* Modified rows      : " + modification);
            System.out.println("* Added rows         : " + ajout);
            System.out.println("* Deleted rows       : " + suppression);
        }
    }
}