package com.mmd;

import com.google.gson.*;
import java.io.*;

public class ConfigurationManager {
    private String clePrincipale;
    private String cleSecondaire;
    private JsonObject subSectionKeys;
    
    public ConfigurationManager(String fichierConfig) throws IOException {
        chargerConfiguration(fichierConfig);
    }
    
    private void chargerConfiguration(String fichierConfig) throws IOException {
        JsonObject config = JsonParser.parseReader(new FileReader(fichierConfig)).getAsJsonObject();
        
        this.clePrincipale = config.get("primary_key").getAsString();
        this.cleSecondaire = config.has("fallback_key") ? 
            config.get("fallback_key").getAsString() : null;
        this.subSectionKeys = config.getAsJsonObject("subSectionKeys");
    }
    
    // Getters
    public String getClePrincipale() { return clePrincipale; }
    public String getCleSecondaire() { return cleSecondaire; }
    public JsonObject getSubSectionKeys() { return subSectionKeys; }
}
