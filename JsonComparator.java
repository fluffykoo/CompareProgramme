package com.mmd.json;

import com.google.gson.*;
import java.io.*;
import java.util.*;

public class JsonComparator {
    private final ConfigurationManager config;

    public JsonComparator(String cfgFile) throws IOException {
        this.config = new ConfigurationManager(cfgFile);
    }

    public List<Difference> compare(String refFile, String newFile) throws IOException {
        JsonArray refArr = JsonParser.parseReader(new FileReader(refFile)).getAsJsonArray();
        JsonArray newArr = JsonParser.parseReader(new FileReader(newFile)).getAsJsonArray();
        Map<String, JsonObject> refMap = indexEntities(refArr);
        Map<String, JsonObject> newMap = indexEntities(newArr);
        List<Difference> diffs = new ArrayList<>();

        // Suppressions & modifications
        for (String id : refMap.keySet()) {
            if (!newMap.containsKey(id)) {
                diffs.add(new Difference(id, ChangeType.DELETION, "", "", null, null));
            } else {
                compareObjects(id, refMap.get(id), newMap.get(id), diffs);
            }
        }
        // Ajouts
        for (String id : newMap.keySet()) {
            if (!refMap.containsKey(id)) {
                diffs.add(new Difference(id, ChangeType.ADDITION, "", "", null, null));
            }
        }
        return diffs;
    }

    private Map<String, JsonObject> indexEntities(JsonArray arr) {
        Map<String, JsonObject> map = new LinkedHashMap<>();
        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();
            String id = extractByKey(o, config.getPrimaryKey());
            if (id == null && config.getFallbackKey() != null) {
                id = extractByKey(o, config.getFallbackKey());
            }
            if (id != null) map.put(id, o);
        }
        return map;
    }

    private String extractByKey(JsonObject o, String key) {
        if (key == null || !o.has(key)) return null;
        JsonElement el = o.get(key);
        return el.isJsonPrimitive() ? el.getAsString() : null;
    }

    private void compareObjects(String objId, JsonObject refO, JsonObject newO, List<Difference> diffs) {
        Set<String> secs = new HashSet<>(refO.keySet());
        secs.addAll(newO.keySet());
        for (String sec : secs) {
            JsonElement r = refO.get(sec), n = newO.get(sec);
            List<String> subKeys = config.getSubSectionKeyList(sec);
            if (!subKeys.isEmpty() && r != null && r.isJsonArray() && n != null && n.isJsonArray()) {
                compareSubSection(objId, sec, subKeys, r.getAsJsonArray(), n.getAsJsonArray(), diffs);
            } else {
                compareField(objId, sec, r, n, diffs);
            }
        }
    }

    private void compareField(String id, String sec, JsonElement r, JsonElement n, List<Difference> diffs) {
        if (r == null && n != null) {
            diffs.add(new Difference(id, ChangeType.ADDITION, sec, sec, null, extractSimple(n)));
        } else if (r != null && n == null) {
            diffs.add(new Difference(id, ChangeType.DELETION, sec, sec, extractSimple(r), null));
        } else if (r != null && n != null && !r.equals(n)) {
            if (r.isJsonPrimitive() && n.isJsonPrimitive()) {
                diffs.add(new Difference(id, ChangeType.MODIFICATION, sec, sec, r.getAsString(), n.getAsString()));
            } else {
                diffs.add(new Difference(id, ChangeType.MODIFICATION, sec, sec, extractSimple(r), extractSimple(n)));
            }
        }
    }

    private void compareSubSection(String id, String sec, List<String> keys,
                                   JsonArray refArr, JsonArray newArr, List<Difference> diffs) {
        Map<String, JsonObject> refMap = indexByCompositeKey(refArr, keys);
        Map<String, JsonObject> newMap = indexByCompositeKey(newArr, keys);
        Set<String> allIds = new HashSet<>(refMap.keySet());
        allIds.addAll(newMap.keySet());

        for (String subId : allIds) {
            JsonObject o1 = refMap.get(subId), o2 = newMap.get(subId);
            if (o1 == null) {
                for (String f : o2.keySet()) {
                    diffs.add(new Difference(id, ChangeType.ADDITION, sec, subId + ", " + f, null, extractSimple(o2.get(f))));
                }
            } else if (o2 == null) {
                for (String f : o1.keySet()) {
                    diffs.add(new Difference(id, ChangeType.DELETION, sec, subId + ", " + f, extractSimple(o1.get(f)), null));
                }
            } else {
                Set<String> fields = new HashSet<>(o1.keySet());
                fields.addAll(o2.keySet());
                for (String f : fields) {
                    JsonElement v1 = o1.get(f), v2 = o2.get(f);
                    if ((v1 == null && v2 != null) || (v1 != null && v2 == null) || (v1 != null && v2 != null && !v1.equals(v2))) {
                        ChangeType t = v1 == null ? ChangeType.ADDITION : v2 == null ? ChangeType.DELETION : ChangeType.MODIFICATION;
                        diffs.add(new Difference(id, t, sec, subId + ", " + f, extractSimple(v1), extractSimple(v2)));
                    }
                }
            }
        }
    }

    private Map<String, JsonObject> indexByCompositeKey(JsonArray arr, List<String> keys) {
        Map<String, JsonObject> map = new HashMap<>();
        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();
            List<String> vals = new ArrayList<>();
            boolean ok = true;
            for (String k : keys) {
                if (!o.has(k) || !o.get(k).isJsonPrimitive()) { ok = false; break; }
                vals.add(o.get(k).getAsString());
            }
            if (ok) map.put(String.join("|", vals), o);
        }
        return map;
    }

    private String extractSimple(JsonElement el) {
        return (el != null && el.isJsonPrimitive()) ? el.getAsString() : "";
    }
}
