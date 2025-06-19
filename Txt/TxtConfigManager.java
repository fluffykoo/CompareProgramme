package com.mmd.txt;

import com.google.gson.*;
import java.io.*;
import java.util.*;

public class TxtConfigManager {
    private List<Integer> keyIndexes;
    private List<Integer> ignoredIndexes;

    public TxtConfigManager(String configFilePath) throws IOException {
        JsonObject config = JsonParser.parseReader(new FileReader(configFilePath)).getAsJsonObject();

        keyIndexes = new ArrayList<>();
        ignoredIndexes = new ArrayList<>();

        if (config.has("primary_key_columns")) {
            for (JsonElement elem : config.getAsJsonArray("primary_key_columns")) {
                keyIndexes.add(elem.getAsInt());
            }
        }

        if (config.has("ignored_columns")) {
            for (JsonElement elem : config.getAsJsonArray("ignored_columns")) {
                ignoredIndexes.add(elem.getAsInt());
            }
        }
    }

    public List<Integer> getKeyIndexes() {
        return keyIndexes;
    }

    public Set<Integer> getIgnoredIndexes() {
        return new HashSet<>(ignoredIndexes);
    }
}
