package com.mmd.txt;

import com.google.gson.*;
import java.io.*;
import java.util.*;

public class TxtSimpleConfigReader {
    private int indexCol;
    private Set<Integer> colonnesIgnorees;
    private String delimiter;

    public TxtSimpleConfigReader(String cheminConfig) throws IOException {
        try (FileReader reader = new FileReader(cheminConfig)) {
            Gson gson = new Gson();
            Map<String, Object> config = gson.fromJson(reader, Map.class);

            if (config.containsKey("indexCol")) {
                this.indexCol = ((Double) config.get("indexCol")).intValue() - 1;
            } else {
                throw new IllegalArgumentException("Missing 'indexCol' in config file.");
            }

            this.colonnesIgnorees = new HashSet<>();
            if (config.containsKey("ignoreColumns")) {
                List<Double> rawList = (List<Double>) config.get("ignoreColumns");
                for (Double d : rawList) {
                    colonnesIgnorees.add(d.intValue() - 1);
                }
            }

            this.delimiter = config.containsKey("delimiter") ? config.get("delimiter").toString() : "|";
        }
    }

    public int getIndexCol() {
        return indexCol;
    }

    public Set<Integer> getColonnesIgnorees() {
        return colonnesIgnorees;
    }

    public String getDelimiter() {
        return delimiter;
    }
}
