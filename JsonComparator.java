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

        // Gestion des suppressions et modifications
        for (String entityId : refMap.keySet()) {
            if (!newMap.containsKey(entityId)) {
                differences.add(new Difference(entityId, ChangeType.DELETION, "", "", null, null));
            } else {
                compareEntities(entityId, refMap.get(entityId), newMap.get(entityId), differences);
            }
        }
        // Gestion des ajouts
        for (String entityId : newMap.keySet()) {
            if (!refMap.containsKey(entityId)) {
                differences.add(new Difference(entityId, ChangeType.ADDITION, "", "", null, null));
            }
        }
        return differences;
    }

    Map<String, JsonObject> indexEntities(JsonArray array) {
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

    // Logique principale de comparaison - similaire à l'original comparerSections
    private void compareEntities(String entityId, JsonObject ref, JsonObject nov, List<Difference> differences) {
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(ref.keySet());
        allKeys.addAll(nov.keySet());

        JsonObject subSectionKeys = config.getSubSectionKeys();

        for (String key : allKeys) {
            JsonElement refVal = ref.get(key);
            JsonElement novVal = nov.get(key);

            // Si la clé est une sous-section (tableau d'objets)
            if (subSectionKeys != null && subSectionKeys.has(key)) {
                String subKey = subSectionKeys.get(key).getAsString();
                if (refVal != null && refVal.isJsonArray() && novVal != null && novVal.isJsonArray()) {
                    compareJsonArraysByKey(entityId, key, subKey, refVal.getAsJsonArray(), novVal.getAsJsonArray(), differences);
                } else if (refVal != null && refVal.isJsonArray()) {
                    // Section supprimée
                    for (JsonElement elem : refVal.getAsJsonArray()) {
                        if (elem.isJsonObject()) {
                            JsonObject obj = elem.getAsJsonObject();
                            String subId = getSubId(obj, subKey);
                            if (subId != null) {
                                differences.add(new Difference(entityId, ChangeType.DELETION, key, subKey + "=" + subId,
                                        elem.toString(), null));
                            }
                        }
                    }
                } else if (novVal != null && novVal.isJsonArray()) {
                    // Section ajoutée
                    for (JsonElement elem : novVal.getAsJsonArray()) {
                        if (elem.isJsonObject()) {
                            JsonObject obj = elem.getAsJsonObject();
                            String subId = getSubId(obj, subKey);
                            if (subId != null) {
                                differences.add(new Difference(entityId, ChangeType.ADDITION, key, subKey + "=" + subId,
                                        null, elem.toString()));
                            }
                        }
                    }
                }
            }
            // Champ simple ou section classique
            else {
                if (refVal == null && novVal != null) {
                    differences.add(new Difference(entityId, ChangeType.ADDITION, key, key, null, novVal.toString()));
                } else if (refVal != null && novVal == null) {
                    differences.add(new Difference(entityId, ChangeType.DELETION, key, key, refVal.toString(), null));
                } else if (refVal != null && novVal != null && !refVal.equals(novVal)) {
                    if (refVal.isJsonPrimitive() && novVal.isJsonPrimitive()) {
                        String cleFinale = "Modified field : " + key;
                        differences.add(new Difference(entityId, ChangeType.MODIFICATION, key,
                                cleFinale, refVal.getAsString(), novVal.getAsString()));
                    } else if (refVal.isJsonObject() && novVal.isJsonObject()) {
                        // Comparaison récursive des objets imbriqués
                        compareNestedObjects(entityId, key, refVal.getAsJsonObject(), novVal.getAsJsonObject(), differences, subSectionKeys);
                    } else if (refVal.isJsonArray() && novVal.isJsonArray()) {
                        // Comparaison des tableaux sans clé spécifique
                        if (!refVal.equals(novVal)) {
                            differences.add(new Difference(entityId, ChangeType.MODIFICATION, key,
                                    "array", refVal.toString(), novVal.toString()));
                        }
                    } else {
                        // Types différents
                        differences.add(new Difference(entityId, ChangeType.MODIFICATION, key,
                                "mixed", refVal.toString(), novVal.toString()));
                    }
                }
            }
        }
    }

    // Comparaison des objets imbriqués - maintient la hiérarchie dans le chemin des clés
    private void compareNestedObjects(String entityId, String section, JsonObject ref, JsonObject nov,
                                      List<Difference> differences, JsonObject subSectionKeys) {
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(ref.keySet());
        allKeys.addAll(nov.keySet());

        for (String key : allKeys) {
            JsonElement refVal = ref.get(key);
            JsonElement novVal = nov.get(key);

            if (refVal == null && novVal != null) {
                differences.add(new Difference(entityId, ChangeType.ADDITION, section, key, null, novVal.toString()));
            } else if (refVal != null && novVal == null) {
                differences.add(new Difference(entityId, ChangeType.DELETION, section, key, refVal.toString(), null));
            } else if (refVal != null && novVal != null && !refVal.equals(novVal)) {
                // Cas des tableaux imbriqués
                if (refVal.isJsonArray() && novVal.isJsonArray()) {
                    if (subSectionKeys != null && subSectionKeys.has(key)) {
                        String subKey = subSectionKeys.get(key).getAsString();
                        compareJsonArraysByKey(entityId, key, subKey, refVal.getAsJsonArray(), novVal.getAsJsonArray(), differences);
                    } else {
                        if (!refVal.equals(novVal)) {
                            differences.add(new Difference(entityId, ChangeType.MODIFICATION, section,
                                    key, refVal.toString(), novVal.toString()));
                        }
                    }
                }
                // Cas des objets imbriqués
                else if (refVal.isJsonObject() && novVal.isJsonObject()) {
                    compareNestedObjects(entityId, section + "." + key, refVal.getAsJsonObject(),
                            novVal.getAsJsonObject(), differences, subSectionKeys);
                }
                // Cas des valeurs simples
                else {
                    differences.add(new Difference(entityId, ChangeType.MODIFICATION, section,
                            key, refVal.toString(), novVal.toString()));
                }
            }
        }
    }

    // Comparaison des tableaux par clé - similaire à l'original compareJsonArraysByKey
    private void compareJsonArraysByKey(String entityId, String section, String subKey,
                                        JsonArray refArray, JsonArray novArray, List<Difference> differences) {
        Map<String, JsonObject> mapRef = new HashMap<>();
        for (JsonElement elem : refArray) {
            if (elem.isJsonObject()) {
                JsonObject obj = elem.getAsJsonObject();
                if (obj.has(subKey)) {
                    mapRef.put(obj.get(subKey).getAsString(), obj);
                }
            }
        }

        Map<String, JsonObject> mapNouv = new HashMap<>();
        for (JsonElement elem : novArray) {
            if (elem.isJsonObject()) {
                JsonObject obj = elem.getAsJsonObject();
                if (obj.has(subKey)) {
                    mapNouv.put(obj.get(subKey).getAsString(), obj);
                }
            }
        }

        Set<String> toutesLesCles = new HashSet<>();
        toutesLesCles.addAll(mapRef.keySet());
        toutesLesCles.addAll(mapNouv.keySet());

        for (String k : toutesLesCles) {
            JsonObject objRef = mapRef.get(k);
            JsonObject objNouv = mapNouv.get(k);

            if (objRef == null) {
                differences.add(new Difference(entityId, ChangeType.ADDITION, section,
                        subKey + "=" + k, null, objNouv.toString()));
            } else if (objNouv == null) {
                differences.add(new Difference(entityId, ChangeType.DELETION, section,
                        subKey + "=" + k, objRef.toString(), null));
            } else {
                Set<String> tousLesChamps = new HashSet<>();
                tousLesChamps.addAll(objRef.keySet());
                tousLesChamps.addAll(objNouv.keySet());

                for (String champ : tousLesChamps) {
                    JsonElement val1 = objRef.get(champ);
                    JsonElement val2 = objNouv.get(champ);

                    if (val1 == null && val2 != null) {
                        differences.add(new Difference(entityId, ChangeType.ADDITION, section,
                                subKey + "=" + k + "." + champ, null, val2.toString()));
                    } else if (val1 != null && val2 == null) {
                        differences.add(new Difference(entityId, ChangeType.DELETION, section,
                                subKey + "=" + k + "." + champ, val1.toString(), null));
                    } else if (val1 != null && val2 != null && !val1.equals(val2)) {
                        if (val1.isJsonObject() && val2.isJsonObject()) {
                            // Appel pour sous-objet
                            compareNestedObjects(entityId, section, val1.getAsJsonObject(), val2.getAsJsonObject(),
                                    differences, null);
                        } else if (val1.isJsonArray() && val2.isJsonArray()) {
                            // Comparaison directe si ce sont deux tableaux
                            if (!val1.equals(val2)) {
                                String cleFinale = "Object.key : " + subKey + " = " + k + " | Modified field : " + champ;
                                differences.add(new Difference(entityId, ChangeType.MODIFICATION, section,
                                        cleFinale, val1.toString(), val2.toString()));
                            }
                        } else {
                            // Valeur simple dans un élément de tableau
                            String cleFinale = "Object.key : " + subKey + " = " + k + " | Modified field : " + champ;
                            differences.add(new Difference(entityId, ChangeType.MODIFICATION, section,
                                    cleFinale, val1.toString(), val2.toString()));
                        }
                    }
                }
            }
        }
    }

    private String getSubId(JsonObject obj, String subKey) {
        if (subKey == null || subKey.isEmpty()) return null;
        if (!obj.has(subKey)) return null;

        JsonElement val = obj.get(subKey);
        return val.isJsonPrimitive() ? val.getAsString() : val.toString();
    }
}
