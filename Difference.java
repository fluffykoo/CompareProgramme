package com.mmd;

public class Difference {
    private String id;
    private TypeChangement type;
    private String section;
    private String cle;
    private String valeurAncienne;
    private String valeurNouvelle;
    
    public Difference(String id, TypeChangement type, String section, 
                     String cle, String valeurAncienne, String valeurNouvelle) {
        this.id = id;
        this.type = type;
        this.section = section;
        this.cle = cle;
        this.valeurAncienne = valeurAncienne;
        this.valeurNouvelle = valeurNouvelle;
    }
    
    // Getters
    public String getId() { return id; }
    public TypeChangement getType() { return type; }
    public String getSection() { return section; }
    public String getCle() { return cle; }
    public String getValeurAncienne() { return valeurAncienne; }
    public String getValeurNouvelle() { return valeurNouvelle; }
}
