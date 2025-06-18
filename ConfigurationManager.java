package com.mmd.json;

import com.google.gson.*;
import java.io.*;
import java.util.*;

public class ConfigurationManager {
    private Map<String, List<String>> subSectionKeys;
    private String primaryKey;
    private String fallbackKey;

    public ConfigurationManager(String configFile) throws IOException {
        JsonObject cfg = JsonParser.parseReader(new FileReader(configFile)).getAsJsonObject();
        this.primaryKey = cfg.has("primary_key") ? cfg.get("primary_key").getAsString() : null;
        this.fallbackKey = cfg.has("fallback_key") ? cfg.get("fallback_key").getAsString() : null;
        this.subSectionKeys = parseKeysMap(cfg.getAsJsonObject("subSectionKeys"));
    }

    // Parse subSectionKeys: section -> list of keys
    private Map<String, List<String>> parseKeysMap(JsonObject obj) {
        Map<String, List<String>> map = new HashMap<>();
        if (obj == null) return map;
        for (String section : obj.keySet()) {
            JsonElement e = obj.get(section);
            List<String> keys = new ArrayList<>();
            if (e.isJsonArray()) {
                for (JsonElement k : e.getAsJsonArray()) keys.add(k.getAsString());
            } else if (e.isJsonPrimitive()) {
                keys.add(e.getAsString());
            }
            map.put(section, keys);
        }
        return map;
    }

    public List<String> getSubSectionKeyList(String section) {
        return subSectionKeys.getOrDefault(section, Collections.emptyList());
    }

    public String getPrimaryKey() { return primaryKey; }
    public String getFallbackKey() { return fallbackKey; }
}
