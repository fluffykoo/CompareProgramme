package com.mmd.txt;

import com.google.gson.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class TxtConfigManager {
    private List<Integer> keyIndexes;
    private List<Integer> ignoredIndexes;

    public TxtConfigManager(String configFile) throws IOException {
        JsonObject config = JsonParser.parseReader(new FileReader(configFile)).getAsJsonObject();

        this.keyIndexes = parseIndexArray(config.getAsJsonArray("key_indexes"));
        this.ignoredIndexes = parseIndexArray(config.getAsJsonArray("ignored_indexes"));
    }

    private List<Integer> parseIndexArray(JsonArray array) {
        List<Integer> list = new ArrayList<>();
        if (array != null) {
            for (JsonElement el : array) {
                list.add(el.getAsInt());
            }
        }
        return list;
    }

    public List<Integer> getKeyIndexes() {
        return keyIndexes;
    }

    public List<Integer> getIgnoredIndexes() {
        return ignoredIndexes;
    }
}
