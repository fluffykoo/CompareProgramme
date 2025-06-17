package com.mmd;

import com.google.gson.*;
import java.io.*;
import java.util.*;

public class JsonComparator {
    private ConfigurationManager config;
    
    public JsonComparator(String fichierConfig) throws IOException {
        this.config = new ConfigurationManager(fichierConfig);
    }
    
    public List<Difference> comparer(String fichierReference, String fichierNouveau) throws IOException {
        // Charger les fichiers JSON
        JsonArray tableauRef = chargerJsonArray(fichierReference);
        JsonArray tableauNouv = chargerJsonArray(fichierNouveau);
        
        // Créer les index
        Map<String, JsonObject> mapRef = creerIndex(tableauRef);
        Map<String, JsonObject> mapNouv = creerIndex(tableauNouv);
        
        // Comparer et collecter les différences
        List<Difference> differences = new ArrayList<>();
        
        // Détecter les suppressions et modifications
        for (String id : mapRef.keySet()) {
            if (!mapNouv.containsKey(id)) {
                differences.add(new Difference(id, TypeChangement.SUPPRESSION, "", "", null, null));
            } else {
                comparerObjets(id, mapRef.get(id), mapNouv.get(id), differences);
            }
        }
        
        // Détecter les ajouts
        for (String id : mapNouv.keySet()) {
            if (!mapRef.containsKey(id)) {
                differences.add(new Difference(id, TypeChangement.AJOUT, "", "", null, null));
            }
        }
        
        return differences;
    }
    
    private JsonArray chargerJsonArray(String fichier) throws IOException {
        return JsonParser.parseReader(new FileReader(fichier)).getAsJsonArray();
    }
    
    private Map<String, JsonObject> creerIndex(JsonArray tableau) {
        Map<String, JsonObject> index = new LinkedHashMap<>();
        
        for (JsonElement element : tableau) {
            JsonObject objet = element.getAsJsonObject();
            String id = extraireId(objet);
            
            if (id != null) {
                index.put(id, objet);
            }
        }
        
        return index;
    }
    
    private String extraireId(JsonObject objet) {
        // Essayer avec la clé principale
        String id = extraireIdParCle(objet, config.getClePrincipale());
        
        // Fallback si nécessaire
        if (id == null && config.getCleSecondaire() != null) {
            id = extraireIdParCle(objet, config.getCleSecondaire());
        }
        
        return id;
    }
    
    private String extraireIdParCle(JsonObject objet, String cle) {
        if (!objet.has(cle)) {
            return null;
        }
        
        JsonElement element = objet.get(cle);
        
        // Cas simple : valeur primitive
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        
        // Cas objet : prendre la première valeur
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (!obj.keySet().isEmpty()) {
                String premiereCle = obj.keySet().iterator().next();
                JsonElement valeur = obj.get(premiereCle);
                if (valeur.isJsonPrimitive()) {
                    return valeur.getAsString();
                }
            }
        }
        
        return null;
    }
    
    private void comparerObjets(String id, JsonObject ref, JsonObject nouv, List<Difference> differences) {
        Set<String> toutesLesCles = new HashSet<>();
        toutesLesCles.addAll(ref.keySet());
        toutesLesCles.addAll(nouv.keySet());
        
        for (String cle : toutesLesCles) {
            JsonElement valRef = ref.get(cle);
            JsonElement valNouv = nouv.get(cle);
            
            if (valRef == null && valNouv != null) {
                differences.add(new Difference(id, TypeChangement.AJOUT, cle, "", null, valNouv.toString()));
            } else if (valRef != null && valNouv == null) {
                differences.add(new Difference(id, TypeChangement.SUPPRESSION, cle, "", valRef.toString(), null));
            } else if (valRef != null && valNouv != null && !valRef.equals(valNouv)) {
                comparerValeurs(id, cle, valRef, valNouv, differences);
            }
        }
    }
    
    private void comparerValeurs(String id, String section, JsonElement ref, JsonElement nouv, List<Difference> differences) {
        // Cas simples : valeurs primitives
        if (ref.isJsonPrimitive() && nouv.isJsonPrimitive()) {
            differences.add(new Difference(id, TypeChangement.MODIFICATION, section, 
                "valeur", ref.getAsString(), nouv.getAsString()));
            return;
        }
        
        // Cas tableaux
        if (ref.isJsonArray() && nouv.isJsonArray()) {
            comparerTableaux(id, section, ref.getAsJsonArray(), nouv.getAsJsonArray(), differences);
            return;
        }
        
        // Cas objets
        if (ref.isJsonObject() && nouv.isJsonObject()) {
            comparerObjets(id, ref.getAsJsonObject(), nouv.getAsJsonObject(), differences);
            return;
        }
        
        // Cas par défaut : types différents
        differences.add(new Difference(id, TypeChangement.MODIFICATION, section, 
            "type_change", ref.toString(), nouv.toString()));
    }
    
    private void comparerTableaux(String id, String section, JsonArray ref, JsonArray nouv, List<Difference> differences) {
        // Si pas de configuration spéciale, comparaison directe
        JsonObject subKeys = config.getSubSectionKeys();
        if (subKeys == null || !subKeys.has(section) || subKeys.get(section).getAsString().isEmpty()) {
            if (!ref.equals(nouv)) {
                differences.add(new Difference(id, TypeChangement.MODIFICATION, section, 
                    "tableau", ref.toString(), nouv.toString()));
            }
            return;
        }
        
        // Comparaison par clé spécifique
        String cleComparaison = subKeys.get(section).getAsString();
        Map<String, JsonObject> mapRef = indexerTableauParCle(ref, cleComparaison);
        Map<String, JsonObject> mapNouv = indexerTableauParCle(nouv, cleComparaison);
        
        Set<String> toutesLesCles = new HashSet<>();
        toutesLesCles.addAll(mapRef.keySet());
        toutesLesCles.addAll(mapNouv.keySet());
        
        for (String cle : toutesLesCles) {
            JsonObject objRef = mapRef.get(cle);
            JsonObject objNouv = mapNouv.get(cle);
            
            if (objRef == null) {
                differences.add(new Difference(id, TypeChangement.AJOUT, section, 
                    cleComparaison + "=" + cle, null, objNouv.toString()));
            } else if (objNouv == null) {
                differences.add(new Difference(id, TypeChangement.SUPPRESSION, section, 
                    cleComparaison + "=" + cle, objRef.toString(), null));
            } else if (!objRef.equals(objNouv)) {
                comparerObjets(id, objRef, objNouv, differences);
            }
        }
    }
    
    private Map<String, JsonObject> indexerTableauParCle(JsonArray tableau, String cle) {
        Map<String, JsonObject> index = new HashMap<>();
        
        for (JsonElement element : tableau) {
            if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                if (obj.has(cle)) {
                    index.put(obj.get(cle).getAsString(), obj);
                }
            }
        }
        
        return index;
    }
}
