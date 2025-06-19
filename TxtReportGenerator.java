package com.mmd.txt;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class TxtReportGenerator {
    private String dossierSortie;
    private String horodatage;

    public TxtReportGenerator(String dossierSortie, String horodatage) {
        this.dossierSortie = dossierSortie;
        this.horodatage = horodatage;
    }

    public String genererRapportTexte(Map<String, String> refMap, Map<String, String> newMap) throws IOException {
        String chemin = dossierSortie + "/comparison_result_" + horodatage + ".txt";
        BufferedWriter writer = Files.newBufferedWriter(Paths.get(chemin));

        int identical = 0, modified = 0, added = 0, deleted = 0;

        for (String key : refMap.keySet()) {
            if (newMap.containsKey(key)) {
                if (refMap.get(key).equals(newMap.get(key))) {
                    identical++;
                } else {
                    modified++;
                    writer.write("[MODIFIED] " + key + "\n");
                }
            } else {
                deleted++;
                writer.write("[DELETED] " + key + "\n");
            }
        }

        for (String key : newMap.keySet()) {
            if (!refMap.containsKey(key)) {
                added++;
                writer.write("[ADDED] " + key + "\n");
            }
        }

        writer.write("\n=== Summary ===\n");
        writer.write("Identical : " + identical + "\n");
        writer.write("Modified  : " + modified + "\n");
        writer.write("Added     : " + added + "\n");
        writer.write("Deleted   : " + deleted + "\n");
        writer.close();

        System.out.println("* Text (.txt) report generated: " + chemin);
        return chemin;
    }
}
