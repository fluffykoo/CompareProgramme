// TxtSimpleConfigReader.java
package com.mmd.txt;

import com.google.gson.*;
import java.io.*;
import java.util.*;

public class TxtSimpleConfigReader {
    private List<Integer> indexCols;
    private Set<Integer> colonnesIgnorees;
    private String separator;

    public TxtSimpleConfigReader(String cheminConfig) throws IOException {
        try (FileReader reader = new FileReader(cheminConfig)) {
            Gson gson = new Gson();
            Map<String, Object> config = gson.fromJson(reader, Map.class);

            Object indexColObj = config.get("indexCol");
            this.indexCols = new ArrayList<>();
            if (indexColObj instanceof Double) {
                indexCols.add(((Double) indexColObj).intValue() - 1);
            } else if (indexColObj instanceof List) {
                List<Double> indices = (List<Double>) indexColObj;
                for (Double d : indices) {
                    indexCols.add(d.intValue() - 1);
                }
            } else {
                throw new IllegalArgumentException("'indexCol' must be an integer or list of integers.");
            }

            this.colonnesIgnorees = new HashSet<>();
            if (config.containsKey("ignoreColumns")) {
                List<Double> rawList = (List<Double>) config.get("ignoreColumns");
                for (Double d : rawList) {
                    colonnesIgnorees.add(d.intValue() - 1);
                }
            }

            this.separator = config.containsKey("separator") ? config.get("separator").toString() : "|";
        }
    }

    public List<Integer> getIndexCols() {
        return indexCols;
    }

    public Set<Integer> getColonnesIgnorees() {
        return colonnesIgnorees;
    }

    public String getSeparator() {
        return separator;
    }
}
