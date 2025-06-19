package com.mmd.txt;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class TxtComparator {
    private final boolean afficherDansTerminal;
    private final StringBuilder rapportTexte = new StringBuilder();
    private final List<String[]> xlsxData = new ArrayList<>();

    public TxtComparator(boolean afficherDansTerminal) {
        this.afficherDansTerminal = afficherDansTerminal;
    }

    public StringBuilder getRapportTexte() {
        return rapportTexte;
    }

    public List<String[]> getXlsxData() {
        return xlsxData;
    }

    private void afficher(String ligne) {
        if (afficherDansTerminal) System.out.println(ligne);
        rapportTexte.append(ligne).append("\n");
    }

    public void runComparison(String fichier1, String fichier2, int indexCle) throws IOException {
        List<String> lignesFichier1 = Files.readAllLines(Paths.get(fichier1));
        List<String> lignesFichier2 = Files.readAllLines(Paths.get(fichier2));

        String ligneEntete = lignesFichier1.get(1);
        String[] nomsColonnes = ligneEntete.split("\\|");

        List<String> donneesRef = lignesFichier1.stream().skip(2).filter(l -> !l.trim().isEmpty()).collect(Collectors.toList());
        List<String> donneesNouv = lignesFichier2.stream().skip(2).filter(l -> !l.trim().isEmpty()).collect(Collectors.toList());

        Map<String, String> mapRef = toMap(donneesRef, indexCle);
        Map<String, String> mapNouv = toMap(donneesNouv, indexCle);

        Set<String> toutesLesCles = new HashSet<>();
        toutesLesCles.addAll(mapRef.keySet());
        toutesLesCles.addAll(mapNouv.keySet());

        int totalIdentique = 0, totalModif = 0, totalAjout = 0, totalSupp = 0;

        for (String cle : toutesLesCles) {
            String ancienne = mapRef.get(cle);
            String nouvelle = mapNouv.get(cle);

            if (ancienne == null) {
                xlsxData.add(new String[]{"AJOUT", cle, "", "", nouvelle});
                afficher("[ADD] Key = " + cle);
                afficher("  New : " + nouvelle);
                totalAjout++;
            } else if (nouvelle == null) {
                xlsxData.add(new String[]{"DELETION", cle, "", ancienne, ""});
                afficher("[DELETE] Key = " + cle);
                afficher("  Old : " + ancienne);
                totalSupp++;
            } else if (!ancienne.equals(nouvelle)) {
                afficher("[MODIFIED] Key = " + cle);
                List<String[]> differences = comparerColonnes(ancienne, nouvelle, "\\|", nomsColonnes, cle);
                for (String[] ligne : differences) {
                    afficher("  * " + ligne[2] + " :");
                    afficher("    Old  : " + ligne[3]);
                    afficher("    New  : " + ligne[4]);
                }
                xlsxData.addAll(differences);
                totalModif++;
            } else {
                totalIdentique++;
            }
        }

        if (!afficherDansTerminal) {
            System.out.println();
            System.out.println("=== Summary ===");
            System.out.println("* Identical rows  : " + totalIdentique);
            System.out.println("* Modified rows   : " + totalModif);
            System.out.println("* Added rows      : " + totalAjout);
            System.out.println("* Deleted rows    : " + totalSupp);
        }
    }

    private Map<String, String> toMap(List<String> lignes, int indexCle) {
        return lignes.stream()
                .map(l -> l.split("\\|", -1))
                .filter(cols -> cols.length > indexCle)
                .collect(Collectors.toMap(cols -> cols[indexCle].trim(), l -> String.join("|", l), (a, b) -> a, LinkedHashMap::new));
    }

    private List<String[]> comparerColonnes(String ancienne, String nouvelle, String delim, String[] noms, String cle) {
        String[] oldCols = ancienne.split(delim, -1);
        String[] newCols = nouvelle.split(delim, -1);
        List<String[]> lignes = new ArrayList<>();

        for (int i = 0; i < Math.max(oldCols.length, newCols.length); i++) {
            String v1 = i < oldCols.length ? oldCols[i].trim() : "";
            String v2 = i < newCols.length ? newCols[i].trim() : "";

            if (!v1.equals(v2)) {
                String nom = i < noms.length ? noms[i].trim() : "Column " + (i + 1);
                lignes.add(new String[]{"MODIFICATION", cle, nom, v1, v2});
            }
        }
        return lignes;
    }
}
    public int getTotalIdentique() {
        return totalIdentique;
    }

    public int getTotalAjout() {
        return totalAjout;
    }

    public int getTotalSupp() {
        return totalSupp;
    }

    public int getTotalModif() {
        return totalModif;
    }
