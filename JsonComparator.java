package com.mmd.json;

import com.google.gson.*;
import java.io.*;
import java.util.*;

public class JsonComparator {
    private final ConfigurationManager config;

    public JsonComparator(String configFile) throws IOException {
        this.config = new ConfigurationManager(configFile);
    }

    public List<Difference> compare(String referenceFile, String newFile) throws IOException {
        JsonArray refArray = JsonParser.parseReader(new FileReader(referenceFile)).getAsJsonArray();
        JsonArray newArray = JsonParser.parseReader(new FileReader(newFile)).getAsJsonArray();

        Map<String, JsonObject> refMap = indexEntities(refArray);
        Map<String, JsonObject> newMap = indexEntities(newArray);

        List<Difference> differences = new ArrayList<>();

        // Handle deletions and modifications
        for (String entityId : refMap.keySet()) {
            if (!newMap.containsKey(entityId)) {
                differences.add(new Difference(entityId, ChangeType.DELETION, "", "", null, null));
            } else {
                compareEntities(entityId, refMap.get(entityId), newMap.get(entityId), differences);
            }
        }
        // Handle additions
        for (String entityId : newMap.keySet()) {
            if (!refMap.containsKey(entityId)) {
                differences.add(new Difference(entityId, ChangeType.ADDITION, "", "", null, null));
            }
        }
        return differences;
    }

    // Index entities by primary or fallback key
    private Map<String, JsonObject> indexEntities(JsonArray array) {
        Map<String, JsonObject> map = new LinkedHashMap<>();
        for (JsonElement elem : array) {
            JsonObject obj = elem.getAsJsonObject();
            String id = extractEntityId(obj);
            if (id != null) map.put(id, obj);
        }
        return map;
    }

    private String extractEntityId(JsonObject obj) {
        String id = extractIdByKey(obj, config.getPrimaryKey());
        if (id == null && config.getFallbackKey() != null) {
            id = extractIdByKey(obj, config.getFallbackKey());
        }
        return id;
    }

    private String extractIdByKey(JsonObject obj, String key) {
        if (!obj.has(key)) return null;
        JsonElement element = obj.get(key);
        if (element.isJsonPrimitive()) return element.getAsString();
        if (element.isJsonObject()) {
            JsonObject o = element.getAsJsonObject();
            if (!o.keySet().isEmpty()) {
                String firstKey = o.keySet().iterator().next();
                JsonElement value = o.get(firstKey);
                if (value.isJsonPrimitive()) return value.getAsString();
            }
        }
        return null;
    }

    // Main comparison logic for an entity
    private void compareEntities(String entityId, JsonObject ref, JsonObject nov, List<Difference> differences) {
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(ref.keySet());
        allKeys.addAll(nov.keySet());

        JsonObject subSectionKeys = config.getSubSectionKeys();

        for (String key : allKeys) {
            JsonElement refVal = ref.get(key);
            JsonElement novVal = nov.get(key);

            // If key is a sub-section (array of objects)
            if (subSectionKeys != null && subSectionKeys.has(key)) {
                String subKey = subSectionKeys.get(key).getAsString();
                if (refVal != null && refVal.isJsonArray() && novVal != null && novVal.isJsonArray()) {
                    compareSubSectionArrays(entityId, key, subKey, refVal.getAsJsonArray(), novVal.getAsJsonArray(), differences);
                } else if (refVal != null && refVal.isJsonArray()) {
                    // Section deleted
                    for (JsonElement elem : refVal.getAsJsonArray()) {
                        String subId = getSubId(elem.getAsJsonObject(), subKey);
                        differences.add(new Difference(entityId, ChangeType.DELETION, key, subKey + "=" + subId, elem.toString(), null));
                    }
                } else if (novVal != null && novVal.isJsonArray()) {
                    // Section added
                    for (JsonElement elem : novVal.getAsJsonArray()) {
                        String subId = getSubId(elem.getAsJsonObject(), subKey);
                        differences.add(new Difference(entityId, ChangeType.ADDITION, key, subKey + "=" + subId, null, elem.toString()));
                    }
                }
            }
            // Simple field or classic section
            else {
                if (refVal == null && novVal != null) {
                    differences.add(new Difference(entityId, ChangeType.ADDITION, key, key, null, novVal.toString()));
                } else if (refVal != null && novVal == null) {
                    differences.add(new Difference(entityId, ChangeType.DELETION, key, key, refVal.toString(), null));
                } else if (refVal != null && novVal != null && !refVal.equals(novVal)) {
                    if (refVal.isJsonPrimitive() && novVal.isJsonPrimitive()) {
                        differences.add(new Difference(entityId, ChangeType.MODIFICATION, key, key, refVal.getAsString(), novVal.getAsString()));
                    } else {
                        // Pour objets ou tableaux non gérés par subSectionKeys, on affiche tout
                        differences.add(new Difference(entityId, ChangeType.MODIFICATION, key, key, refVal.toString(), novVal.toString()));
                    }
                }
            }
        }
    }

    // Compare arrays in sub-sections using subSectionKeys
    private void compareSubSectionArrays(String entityId, String section, String subKey, JsonArray refArray, JsonArray novArray, List<Difference> differences) {
        Map<String, JsonObject> refMap = indexBySubKey(refArray, subKey);
        Map<String, JsonObject> novMap = indexBySubKey(novArray, subKey);

        Set<String> allSubIds = new HashSet<>();
        allSubIds.addAll(refMap.keySet());
        allSubIds.addAll(novMap.keySet());

        for (String subId : allSubIds) {
            JsonObject refObj = refMap.get(subId);
            JsonObject novObj = novMap.get(subId);

            if (refObj == null && novObj != null) {
                differences.add(new Difference(entityId, ChangeType.ADDITION, section, subKey + "=" + subId, null, novObj.toString()));
            } else if (refObj != null && novObj == null) {
                differences.add(new Difference(entityId, ChangeType.DELETION, section, subKey + "=" + subId, refObj.toString(), null));
            } else if (refObj != null && novObj != null) {
                // Compare fields inside the sub-object
                Set<String> allFields = new HashSet<>();
                allFields.addAll(refObj.keySet());
                allFields.addAll(novObj.keySet());
                for (String field : allFields) {
                    JsonElement refVal = refObj.get(field);
                    JsonElement novVal = novObj.get(field);
                    if (refVal == null && novVal != null) {
                        differences.add(new Difference(entityId, ChangeType.ADDITION, section, subKey + "=" + subId + ", " + field, null, novVal.toString()));
                    } else if (refVal != null && novVal == null) {
                        differences.add(new Difference(entityId, ChangeType.DELETION, section, subKey + "=" + subId + ", " + field, refVal.toString(), null));
                    } else if (refVal != null && novVal != null && !refVal.equals(novVal)) {
                        differences.add(new Difference(entityId, ChangeType.MODIFICATION, section, subKey + "=" + subId + ", " + field, refVal.toString(), novVal.toString()));
                    }
                }
            }
        }
    }

    private Map<String, JsonObject> indexBySubKey(JsonArray array, String subKey) {
        Map<String, JsonObject> map = new HashMap<>();
        for (JsonElement elem : array) {
            if (elem.isJsonObject()) {
                JsonObject obj = elem.getAsJsonObject();
                String key = getSubId(obj, subKey);
                if (key != null) map.put(key, obj);
            }
        }
        return map;
    }

    private String getSubId(JsonObject obj, String subKey) {
        if (subKey == null || subKey.isEmpty()) return obj.toString();
        if (!obj.has(subKey)) return null;
        JsonElement val = obj.get(subKey);
        return val.isJsonPrimitive() ? val.getAsString() : val.toString();
    }
}
