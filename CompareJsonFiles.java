package com.mmd;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public class CompareJsonFiles {
    
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println("Usage: java CompareJsonFiles <fichier1> <fichier2> <dossier_sortie> <config>");
            return;
        }
        
        String fichierReference = args[0];
        String fichierNouveau = args[1];
        String dossierSortie = args[2];
        String fichierConfig = args[3];
        
        // Initialiser le comparateur
        JsonComparator comparateur = new JsonComparator(fichierConfig);
        
        // Effectuer la comparaison
        List<Difference> differences = comparateur.comparer(fichierReference, fichierNouveau);
        
        // Générer les rapports
        String horodatage = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        ReportGenerator generator = new ReportGenerator(dossierSortie, horodatage);
        
        generator.genererRapportTexte(differences);
        generator.genererRapportExcel(differences);
        
        // Afficher le résumé
        afficherResume(differences);
    }
    
    private static void afficherResume(List<Difference> differences) {
        long ajouts = differences.stream().filter(d -> d.getType() == TypeChangement.AJOUT).count();
        long suppressions = differences.stream().filter(d -> d.getType() == TypeChangement.SUPPRESSION).count();
        long modifications = differences.stream().filter(d -> d.getType() == TypeChangement.MODIFICATION).count();
        
        System.out.println("\n=== Résumé ===");
        System.out.println("Ajouts: " + ajouts);
        System.out.println("Suppressions: " + suppressions);
        System.out.println("Modifications: " + modifications);
    }
}
