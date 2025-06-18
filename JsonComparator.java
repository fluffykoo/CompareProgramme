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

        // Deletions & modifications
        for (String objectId : refMap.keySet()) {
            if (!newMap.containsKey(objectId)) {
                differences.add(new Difference(objectId, ChangeType.DELETION, "", "", null, null));
            } else {
                compareObjects(objectId, refMap.get(objectId), newMap.get(objectId), differences);
            }
        }
        // Additions
        for (String objectId : newMap.keySet()) {
            if (!refMap.containsKey(objectId)) {
                differences.add(new Difference(objectId, ChangeType.ADDITION, "", "", null, null));
            }
        }
        return differences;
    }

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
        String id = extractByKey(obj, config.getPrimaryKey());
        if (id == null && config.getFallbackKey() != null) {
            id = extractByKey(obj, config.getFallbackKey());
        }
        return id;
    }

    private String extractByKey(JsonObject obj, String key) {
        if (key == null || !obj.has(key)) return null;
        JsonElement el = obj.get(key);
        if (el.isJsonPrimitive()) return el.getAsString();
        if (el.isJsonObject()) {
            JsonObject inner = el.getAsJsonObject();
            if (!inner.keySet().isEmpty()) {
                String k = inner.keySet().iterator().next();
                JsonElement v = inner.get(k);
                if (v.isJsonPrimitive()) return v.getAsString();
            }
        }
        return null;
    }

    private void compareObjects(String objectId, JsonObject refObj, JsonObject newObj, List<Difference> diffs) {
        Set<String> keys = new HashSet<>();
        keys.addAll(refObj.keySet());
        keys.addAll(newObj.keySet());

        for (String section : keys) {
            JsonElement r = refObj.get(section);
            JsonElement n = newObj.get(section);

            List<String> subKeys = config.getSubSectionKeyList(section);
            if (!subKeys.isEmpty() && r != null && r.isJsonArray() && n != null && n.isJsonArray()) {
                compareSubSection(objectId, section, subKeys, r.getAsJsonArray(), n.getAsJsonArray(), diffs);
            } else {
                compareField(objectId, section, r, n, diffs);
            }
        }
    }

    private void compareField(String objectId, String section, JsonElement r, JsonElement n, List<Difference> diffs) {
        if (r == null && n != null) {
            diffs.add(new Difference(objectId, ChangeType.ADDITION, section, section, null, extractSimpleValue(n)));
        } else if (r != null && n == null) {
            diffs.add(new Difference(objectId, ChangeType.DELETION, section, section, extractSimpleValue(r), null));
        } else if (r != null && n != null && !r.equals(n)) {
            if (r.isJsonPrimitive() && n.isJsonPrimitive()) {
                diffs.add(new Difference(objectId, ChangeType.MODIFICATION, section, section, r.getAsString(), n.getAsString()));
            } else {
                diffs.add(new Difference(objectId, ChangeType.MODIFICATION, section, section, extractSimpleValue(r), extractSimpleValue(n)));
            }
        }
    }

    // --- Gestion des clés multiples dans les sous-sections ---
    private void compareSubSection(String objectId, String section, List<String> keys, JsonArray refArr, JsonArray newArr, List<Difference> diffs) {
        Map<String, JsonObject> refMap = indexByCompositeKey(refArr, keys);
        Map<String, JsonObject> newMap = indexByCompositeKey(newArr, keys);
        Set<String> allIds = new HashSet<>(refMap.keySet());
        allIds.addAll(newMap.keySet());

        for (String subId : allIds) {
            JsonObject o1 = refMap.get(subId);
            JsonObject o2 = newMap.get(subId);
            if (o1 == null) {
                for (String f : o2.keySet()) {
                    diffs.add(new Difference(objectId, ChangeType.ADDITION,
                        section, subId + ", " + f,
                        null, extractSimpleValue(o2.get(f))));
                }
            } else if (o2 == null) {
                for (String f : o1.keySet()) {
                    diffs.add(new Difference(objectId, ChangeType.DELETION,
                        section, subId + ", " + f,
                        extractSimpleValue(o1.get(f)), null));
                }
            } else {
                Set<String> allFields = new HashSet<>(o1.keySet());
                allFields.addAll(o2.keySet());
                for (String f : allFields) {
                    JsonElement v1 = o1.get(f), v2 = o2.get(f);
                    if ((v1 == null && v2 != null)
                     || (v1 != null && v2 == null)
                     || (v1 != null && v2 != null && !v1.equals(v2))) {
                        ChangeType type = v1 == null ? ChangeType.ADDITION
                                        : v2 == null ? ChangeType.DELETION
                                                     : ChangeType.MODIFICATION;
                        diffs.add(new Difference(objectId, type,
                            section, subId + ", " + f,
                            extractSimpleValue(v1), extractSimpleValue(v2)));
                    }
                }
            }
        }
    }

    private Map<String, JsonObject> indexByCompositeKey(JsonArray arr, List<String> keys) {
        Map<String, JsonObject> map = new HashMap<>();
        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();
            String id = computeCompositeKey(o, keys);
            if (id != null) map.put(id, o);
        }
        return map;
    }

    private String computeCompositeKey(JsonObject obj, List<String> keys) {
        List<String> values = new ArrayList<>();
        for (String k : keys) {
            if (!obj.has(k) || !obj.get(k).isJsonPrimitive()) return null;
            values.add(obj.get(k).getAsString());
        }
        return String.join("|", values);
    }

    private String extractSimpleValue(JsonElement el) {
        if (el == null) return null;
        if (el.isJsonPrimitive()) return el.getAsString();
        return "";
    }
}
