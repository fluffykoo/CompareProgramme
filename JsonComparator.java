package com.mmd.json;

import com.google.gson.*;
import java.io.*;
import java.util.*;

public class JsonComparator {
    private final ConfigurationManager config;

    public JsonComparator(String configFile) throws IOException {
        this.config = new ConfigurationManager(configFile);
    }

    public List<Difference> compare(String referenceFile,
                                    String newFile) throws IOException {
        JsonArray refArray = JsonParser
            .parseReader(new FileReader(referenceFile))
            .getAsJsonArray();
        JsonArray newArray = JsonParser
            .parseReader(new FileReader(newFile))
            .getAsJsonArray();

        Map<String, JsonObject> refMap = indexEntities(refArray);
        Map<String, JsonObject> newMap = indexEntities(newArray);
        List<Difference> diffs = new ArrayList<>();

        // Handle deletions and modifications
        for (String id : refMap.keySet()) {
            if (!newMap.containsKey(id)) {
                diffs.add(new Difference(id, ChangeType.DELETION,
                                         "", "", null, null));
            } else {
                compareEntities(id, refMap.get(id),
                                 newMap.get(id), diffs);
            }
        }
        // Handle additions
        for (String id : newMap.keySet()) {
            if (!refMap.containsKey(id)) {
                diffs.add(new Difference(id, ChangeType.ADDITION,
                                         "", "", null, null));
            }
        }
        return diffs;
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

    private void compareEntities(String id, JsonObject refObj,
                                 JsonObject newObj,
                                 List<Difference> diffs) {
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
                compareSubSection(id, section,
                    subKeys.get(section).getAsString(),
                    r.getAsJsonArray(),
                    n.getAsJsonArray(), diffs);
            } else {
                compareField(id, section, r, n, diffs);
            }
        }
    }

    private void compareField(String id, String section,
                              JsonElement r, JsonElement n,
                              List<Difference> diffs) {
        if (r == null && n != null) {
            diffs.add(new Difference(id, ChangeType.ADDITION,
                                     section, section, null,
                                     n.toString()));
        } else if (r != null && n == null) {
            diffs.add(new Difference(id, ChangeType.DELETION,
                                     section, section,
                                     r.toString(), null));
        } else if (r != null && n != null && !r.equals(n)) {
            if (r.isJsonPrimitive() && n.isJsonPrimitive()) {
                diffs.add(new Difference(id, ChangeType.MODIFICATION,
                                         section, section,
                                         r.getAsString(),
                                         n.getAsString()));
            } else {
                diffs.add(new Difference(id, ChangeType.MODIFICATION,
                                         section, section,
                                         r.toString(), n.toString()));
            }
        }
    }

    private void compareSubSection(String id, String section,
                                   String key, JsonArray a, JsonArray b,
                                   List<Difference> diffs) {
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
                    diffs.add(new Difference(id, ChangeType.ADDITION,
                        section, key + "=" + subId + ", " + f,
                        null, val.toString()));
                }
            } else if (o2 == null) {
                for (String f : o1.keySet()) {
                    JsonElement val = o1.get(f);
                    diffs.add(new Difference(id, ChangeType.DELETION,
                        section, key + "=" + subId + ", " + f,
                        val.toString(), null));
                }
            } else {
                for (String f : new HashSet<>(o1.keySet())
                                 {{ addAll(o2.keySet()); }}) {
                    JsonElement v1 = o1.get(f), v2 = o2.get(f);
                    if ((v1 == null && v2 != null)
                     || (v1 != null && v2 == null)
                     || (v1 != null && v2 != null && !v1.equals(v2))) {
                        String typ = (v1 == null ? ChangeType.ADDITION
                                    : v2 == null ? ChangeType.DELETION
                                                  : ChangeType.MODIFICATION)
                                      .name();
                        diffs.add(new Difference(id,
                            ChangeType.valueOf(typ),
                            section, key + "=" + subId + ", " + f,
                            v1 != null ? v1.toString() : null,
                            v2 != null ? v2.toString() : null));
                    }
                }
            }
        }
    }

    private Map<String, JsonObject> indexByKey(JsonArray arr,
                                               String key) {
        Map<String, JsonObject> map = new HashMap<>();
        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();
            if (o.has(key)) {
                map.put(o.get(key).getAsString(), o);
            }
        }
        return map;
    }
}
