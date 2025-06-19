package com.mmd.txt;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CompareTxtFiles {
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Usage : java CompareTxtFiles <fichier1> <fichier2> <dossierRapport> [terminal]");
            return;
        }

        String fichier1 = args[0];
        String fichier2 = args[1];
        String dossierRapport = args[2];
        boolean afficherTerminal = args.length > 3 && args[3].equalsIgnoreCase("terminal");

        // Charger configuration si elle existe
        TxtConfigManager config = new TxtConfigManager("txt_config.json");
        List<Integer> keyIndexes = config.getKeyIndexes();
        Set<Integer> ignoredIndexes = config.getIgnoredIndexes();

        List<String> lignesFichier1 = Files.readAllLines(Paths.get(fichier1));
        List<String> lignesFichier2 = Files.readAllLines(Paths.get(fichier2));

        String separator = detectSeparator(lignesFichier1);

        TxtComparator comparator = new TxtComparator(keyIndexes, ignoredIndexes);
        List<String[]> ajout = new ArrayList<>();
        List<String[]> suppression = new ArrayList<>();
        List<String[]> modification = new ArrayList<>();
        List<String[]> identique = new ArrayList<>();

        comparator.comparer(lignesFichier1, lignesFichier2, separator, ajout, suppression, modification, identique);

        LocalDateTime now = LocalDateTime.now();
        String horodatage = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        TxtReportGenerator generator = new TxtReportGenerator(dossierRapport, horodatage);
        generator.genererRapportTexte(ajout, suppression, modification, identique);

        if (afficherTerminal) {
            generator.afficherDansTerminal(ajout, suppression, modification, identique);
        }

        System.out.println("\n=== Résumé ===");
        System.out.println("Clés utilisées : " + keyIndexes);
        System.out.println("Colonnes ignorées : " + ignoredIndexes);
        System.out.println("Fichier de référence : " + fichier1);
        System.out.println("Nouveau fichier      : " + fichier2);
        System.out.println("Rapport généré (.txt): " + generator.getTxtFilePath());
        System.out.println("* Lignes identiques : " + identique.size());
        System.out.println("* Lignes modifiées  : " + modification.size());
        System.out.println("* Lignes ajoutées   : " + ajout.size());
        System.out.println("* Lignes supprimées : " + suppression.size());
    }

    private static String detectSeparator(List<String> lignes) {
        String[] candidats = {"|", ",", ";", "\t"};
        for (String sep : candidats) {
            if (lignes.get(0).contains(sep)) {
                return sep;
            }
        }
        return "|";
    }
}
