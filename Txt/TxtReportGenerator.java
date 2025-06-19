package com.mmd.txt;

import java.io.*;
import java.util.*;

public class TxtReportGenerator {
    private final String dossier;
    private final String horodatage;
    private String txtFilePath;

    public TxtReportGenerator(String dossier, String horodatage) {
        this.dossier = dossier;
        this.horodatage = horodatage;
    }

    public void generateReports(List<String[]> ajout, List<String[]> suppression,
                                List<String[]> modification, List<String[]> identique) throws IOException {
        txtFilePath = dossier + File.separator + "rapport_" + horodatage + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(txtFilePath))) {
            writer.write("=== Résumé ===" + System.lineSeparator());
            writer.write("Ajouts : " + ajout.size() + System.lineSeparator());
            writer.write("Suppressions : " + suppression.size() + System.lineSeparator());
            writer.write("Modifications : " + modification.size() + System.lineSeparator());
            writer.write("Identiques : " + identique.size() + System.lineSeparator());
        }
    }

    public void afficherDansTerminal(List<String[]> ajout, List<String[]> suppression,
                                     List<String[]> modification, List<String[]> identique) {
        System.out.println("=== Résumé (terminal) ===");
        System.out.println("Ajouts : " + ajout.size());
        System.out.println("Suppressions : " + suppression.size());
        System.out.println("Modifications : " + modification.size());
        System.out.println("Identiques : " + identique.size());
    }

    public String getTxtFilePath() {
        return txtFilePath;
    }
}
