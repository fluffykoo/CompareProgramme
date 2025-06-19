package com.mmd.txt;

import java.io.*;
import java.util.*;

public class CompareTxtFiles {
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Usage : java CompareTxtFiles <fichier1> <fichier2> <fichier_config> [terminal]");
            return;
        }

        String fichier1 = args[0];
        String fichier2 = args[1];
        String configPath = args[2];
        boolean afficherTerminal = args.length >= 4 && args[3].equalsIgnoreCase("terminal");

        TxtConfigManager config = new TxtConfigManager(configPath);
        List<Integer> keyIndexes = config.getKeyIndexes();
        Set<Integer> ignoredIndexes = config.getIgnoredIndexes();

        List<String> lignesFichier1 = TxtComparator.readFile(fichier1, TxtComparator.detectSeparator(fichier1));
        List<String> lignesFichier2 = TxtComparator.readFile(fichier2, TxtComparator.detectSeparator(fichier2));

        TxtComparator comparator = new TxtComparator();
        List<String[]> ajout = new ArrayList<>();
        List<String[]> suppression = new ArrayList<>();
        List<String[]> modification = new ArrayList<>();
        List<String[]> identique = new ArrayList<>();

        comparator.comparer(lignesFichier1, lignesFichier2, keyIndexes, ignoredIndexes, ajout, suppression, modification, identique);

        String horodatage = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        TxtReportGenerator generator = new TxtReportGenerator(".", horodatage);
        generator.generateReports(ajout, suppression, modification, identique);

        if (afficherTerminal) {
            generator.afficherDansTerminal(ajout, suppression, modification, identique);
        }
    }
}
