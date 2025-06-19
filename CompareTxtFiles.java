package com.mmd;

import java.io.IOException;
import java.util.stream.Collectors;

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

        int totalAjout = (int) comparator.getXlsxData().stream().filter(l -> "AJOUT".equals(l[0])).count();
        int totalSupp = (int) comparator.getXlsxData().stream().filter(l -> "DELETION".equals(l[0])).count();
        int totalModif = (int) comparator.getXlsxData().stream().filter(l -> "MODIFICATION".equals(l[0])).map(l -> l[1]).distinct().count();
        int totalIdentique = comparator.getTotalIdentique();

        String nomFichierTxt = "comparison_result.txt";
        String nomFichierXlsx = "comparison_result.xlsx";

        System.out.println();
        System.out.println("=== Summary ===");
        System.out.println("Reference file       : " + fichier1);
        System.out.println("New file             : " + fichier2);
        System.out.println("Text (.txt) report   : " + dossierRapport + "/" + nomFichierTxt);
        System.out.println("Excel (.xlsx) report : " + dossierRapport + "/" + nomFichierXlsx);
        System.out.println("* Identical rows     : " + totalIdentique);
        System.out.println("* Modified rows      : " + totalModif);
        System.out.println("* Added rows         : " + totalAjout);
        System.out.println("* Deleted rows       : " + totalSupp);
    }
}
