package com.mmd.txt;

import com.google.gson.*;
import java.io.*;
import java.util.*;

public class TxtConfigManager {
    private List<Integer> keyColumns;
    private List<Integer> ignoredColumns;

    public TxtConfigManager(String configFilePath) throws IOException {
        JsonObject config = JsonParser.parseReader(new FileReader(configFilePath)).getAsJsonObject();

        keyColumns = new ArrayList<>();
        ignoredColumns = new ArrayList<>();

        if (config.has("primary_key_columns")) {
            JsonArray keyArray = config.getAsJsonArray("primary_key_columns");
            for (JsonElement elem : keyArray) {
                keyColumns.add(elem.getAsInt());
            }
        }

        if (config.has("ignored_columns")) {
            JsonArray ignoredArray = config.getAsJsonArray("ignored_columns");
            for (JsonElement elem : ignoredArray) {
                ignoredColumns.add(elem.getAsInt());
            }
        }
    }

    public List<Integer> getKeyColumns() {
        return keyColumns;
    }

    public List<Integer> getIgnoredColumns() {
        return ignoredColumns;
    }
}
