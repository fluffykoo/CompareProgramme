package com.mmd;

import java.io.IOException;

public class CompareTxtFiles {
    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.out.println("Usage : java CompareTxtFiles <fichier1> <fichier2> <indexCle> <dossierRapport> [terminal]");
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
    }
}