package com.mmd.json;

import com.google.gson.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigurationManager {
    private String primaryKey;
    private String fallbackKey;
    private JsonObject subSectionKeys;

    public ConfigurationManager(String configFile) throws IOException {
        JsonObject config = JsonParser
                .parseReader(new FileReader(configFile))
                .getAsJsonObject();
        this.primaryKey = config.get("primary_key").getAsString();
        this.fallbackKey = config.has("fallback_key")
                ? config.get("fallback_key").getAsString()
                : null;
        this.subSectionKeys = config.getAsJsonObject("subSectionKeys");
    }

    public String getPrimaryKey() { return primaryKey; }
    public String getFallbackKey() { return fallbackKey; }
    public JsonObject getSubSectionKeys() { return subSectionKeys; }

    public List<String> getSubSectionKeys(String sectionName) {
        JsonElement keyElement = subSectionKeys.get(sectionName);

        if (keyElement == null || keyElement.isJsonNull()) {
            return new ArrayList<>();
        }

        List<String> keys = new ArrayList<>();

        if (keyElement.isJsonPrimitive()) {
            // clé simple
            keys.add(keyElement.getAsString());
        } else if (keyElement.isJsonArray()) {
            for (JsonElement elem : keyElement.getAsJsonArray()) {
                keys.add(elem.getAsString());
            }
        }

        return keys;
    }
}
