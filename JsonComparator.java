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
        for (String objectId : refMap.keySet()) {
            if (!newMap.containsKey(objectId)) {
                differences.add(new Difference(objectId, ChangeType.DELETION, "", "", null, null));
            } else {
                compareObjects(objectId, refMap.get(objectId), newMap.get(objectId), differences);
            }
        }
        // Handle additions
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
        if (!obj.has(key)) return null;
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
        JsonObject subKeys = config.getSubSectionKeys();

        for (String section : keys) {
            JsonElement r = refObj.get(section);
            JsonElement n = newObj.get(section);

            if (subKeys != null && subKeys.has(section)
                && r != null && r.isJsonArray()
                && n != null && n.isJsonArray()) {
                compareSubSection(objectId, section,
                    subKeys.get(section).getAsString(),
                    r.getAsJsonArray(),
                    n.getAsJsonArray(), diffs);
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

    private void compareSubSection(String objectId, String section, String key, JsonArray a, JsonArray b, List<Difference> diffs) {
        Map<String, JsonObject> m1 = indexByKey(a, key);
        Map<String, JsonObject> m2 = indexByKey(b, key);
        Set<String> all = new HashSet<>(m1.keySet());
        all.addAll(m2.keySet());

        for (String subId : all) {
            JsonObject o1 = m1.get(subId);
            JsonObject o2 = m2.get(subId);
            if (o1 == null) {
                for (String f : o2.keySet()) {
                    JsonElement val = o2.get(f);
                    diffs.add(new Difference(objectId, ChangeType.ADDITION,
                        section, key + "=" + subId + ", " + f,
                        null, extractSimpleValue(val)));
                }
            } else if (o2 == null) {
                for (String f : o1.keySet()) {
                    JsonElement val = o1.get(f);
                    diffs.add(new Difference(objectId, ChangeType.DELETION,
                        section, key + "=" + subId + ", " + f,
                        extractSimpleValue(val), null));
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
                            section, key + "=" + subId + ", " + f,
                            extractSimpleValue(v1), extractSimpleValue(v2)));
                    }
                }
            }
        }
    }

    private Map<String, JsonObject> indexByKey(JsonArray arr, String key) {
        Map<String, JsonObject> map = new HashMap<>();
        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();
            if (o.has(key)) {
                map.put(o.get(key).getAsString(), o);
            }
        }
        return map;
    }

    // Utility to extract only the simple value for Excel/text
    private String extractSimpleValue(JsonElement el) {
        if (el == null) return null;
        if (el.isJsonPrimitive()) {
            return el.getAsString();
        }
        // For objects/arrays, return empty string (or a summary if you want)
        return "";
    }
}
