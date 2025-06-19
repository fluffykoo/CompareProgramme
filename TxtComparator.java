package com.mmd.txt;

import java.io.*;
import java.util.*;

public class TxtComparator {

    public List<Difference> compareFiles(String file1, String file2, TxtConfigManager config) throws IOException {
        List<String[]> lignes1 = lireFichier(file1);
        List<String[]> lignes2 = lireFichier(file2);

        Map<String, String[]> map1 = new HashMap<>();
        Map<String, String[]> map2 = new HashMap<>();

        for (String[] ligne : lignes1) {
            String cle = construireCle(ligne, config.getKeyColumns());
            map1.put(cle, ligne);
        }

        for (String[] ligne : lignes2) {
            String cle = construireCle(ligne, config.getKeyColumns());
            map2.put(cle, ligne);
        }

        List<Difference> differences = new ArrayList<>();

        for (String cle : map1.keySet()) {
            if (!map2.containsKey(cle)) {
                differences.add(new Difference("SUPPRIME", cle, map1.get(cle), null));
            } else {
                String[] ligne1 = map1.get(cle);
                String[] ligne2 = map2.get(cle);
                if (!sontEgales(ligne1, ligne2, config.getIgnoredColumns())) {
                    differences.add(new Difference("MODIFIE", cle, ligne1, ligne2));
                }
            }
        }

        for (String cle : map2.keySet()) {
            if (!map1.containsKey(cle)) {
                differences.add(new Difference("AJOUTE", cle, null, map2.get(cle)));
            }
        }

        return differences;
    }

    private List<String[]> lireFichier(String chemin) throws IOException {
        List<String[]> lignes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(chemin))) {
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                String separateur = detecterSeparateur(ligne);
                lignes.add(ligne.split(separateur));
            }
        }
        return lignes;
    }

    private String construireCle(String[] ligne, List<Integer> colonnes) {
        StringBuilder cle = new StringBuilder();
        for (int index : colonnes) {
            if (index >= 0 && index < ligne.length) {
                cle.append(ligne[index]).append("|");
            }
        }
        return cle.toString();
    }

    private boolean sontEgales(String[] ligne1, String[] ligne2, List<Integer> colonnesIgnorees) {
        int max = Math.max(ligne1.length, ligne2.length);
        for (int i = 0; i < max; i++) {
            if (colonnesIgnorees.contains(i)) continue;

            String val1 = i < ligne1.length ? ligne1[i] : "";
            String val2 = i < ligne2.length ? ligne2[i] : "";

            if (!val1.equals(val2)) return false;
        }
        return true;
    }

    private String detecterSeparateur(String ligne) {
        if (ligne.contains("|")) return "\\|";
        if (ligne.contains(";")) return ";";
        if (ligne.contains(",")) return ",";
        return "\\s+";
    }
}
