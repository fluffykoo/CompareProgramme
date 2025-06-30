package com.mmd.txt;

import com.google.gson.*;
import java.io.*;
import java.util.*;

public class TxtSimpleConfigReader {
    private int indexCol;
    private Set<Integer> colonnesIgnorees;
    private String separator;

    public TxtSimpleConfigReader(String cheminConfig) throws IOException {
        try (FileReader reader = new FileReader(cheminConfig)) {
            Gson gson = new Gson();
            Map<String, Object> config = gson.fromJson(reader, Map.class);

            // Lecture de l'index de clé
            if (config.containsKey("indexCol")) {
                this.indexCol = ((Double) config.get("indexCol")).intValue() - 1;
            } else {
                throw new IllegalArgumentException("Le champ 'indexCol' est manquant dans le fichier config.");
            }

            // Lecture des colonnes à ignorer
            this.colonnesIgnorees = new HashSet<>();
            if (config.containsKey("ignoreColumns")) {
                List<Double> rawList = (List<Double>) config.get("ignoreColumns");
                for (Double d : rawList) {
                    colonnesIgnorees.add(d.intValue() - 1);
                }
            }

            // Lecture du séparateur (par défaut "|")
            if (config.containsKey("separator")) {
                this.separator = config.get("separator").toString();
            } else {
                this.separator = "|"; // valeur par défaut
            }
        }
    }

    public int getIndexCol() {
        return indexCol;
    }

    public Set<Integer> getColonnesIgnorees() {
        return colonnesIgnorees;
    }

    public String getSeparator() {
        return separator;
    }
}
