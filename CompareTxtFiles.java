package com.mmd.txt;

import java.io.IOException;
import java.util.stream.Collectors;

public class CompareTxtFiles {
    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.out.println("Usage: java CompareTxtFiles <referenceFile> <newFile> <keyColumnIndex> <reportFolder> [terminal]");
            return;
        }

        boolean afficherDansTerminal = args.length >= 5 && args[4].trim().replaceAll("\"", "").equalsIgnoreCase("terminal");

        String fichier1 = args[0];
        String fichier2 = args[1];
        int indexCle = Integer.parseInt(args[2]) - 1;
        String dossierRapport = args[3];

        TxtComparator comparator = new TxtComparator(afficherDansTerminal);

        comparator.runComparison(fichier1, fichier2, indexCle);

        TxtReportGenerator generator = new TxtReportGenerator(afficherDansTerminal);
        generator.generateReports(dossierRapport, comparator.getRapportTexte(), comparator.getXlsxData());

        int totalAjout = comparator.getXlsxData().stream().filter(l -> "AJOUT".equals(l[0])).toArray().length;
        int totalSupp = comparator.getXlsxData().stream().filter(l -> "DELETION".equals(l[0])).toArray().length;
        int totalModif = comparator.getXlsxData().stream().filter(l -> "MODIFICATION".equals(l[0])).map(l -> l[1]).collect(Collectors.toSet()).size();
        int totalIdentique = comparator.getTotalIdentique(); // méthode à ajouter

        if (!afficherDansTerminal) {
            System.out.println();
            System.out.println("=== Summary ===");
            System.out.println("Reference file : " + fichier1);
            System.out.println("New file       : " + fichier2);
            System.out.println("* Text (.txt) report generated   : " + generator.getTxtFilePath());
            System.out.println("* Excel (.xlsx) report generated : " + generator.getXlsxFilePath());
            System.out.println("* Identical rows : " + totalIdentique);
            System.out.println("* Modified rows  : " + totalModif);
            System.out.println("* Added rows     : " + totalAjout);
            System.out.println("* Deleted rows   : " + totalSupp);
        }
    }
}
