package com.mmd.txt;

import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import com.google.gson.Gson;

public class TxtSimpleConfigReader {
    public static int getIndexColFromConfig(String cheminConfig) throws IOException {
        try (FileReader reader = new FileReader(cheminConfig)) {
            Gson gson = new Gson();
            Map<?, ?> config = gson.fromJson(reader, Map.class);
            if (config.containsKey("indexCol")) {
                return ((Double) config.get("indexCol")).intValue() - 1;
            } else {
                throw new IllegalArgumentException("Le champ 'indexCol' est manquant dans le fichier config.");
            }
        }
    }
}
